/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */package org.jboss.forge.addon.as.jboss.common.util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Utility for {@link java.io.File files}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Files {
    static final String TMP_DIR_PROPERTY = "java.io.tmpdir";

    /**
     * Creates a file from the base with each path element.
     *
     * @param base  the parent directory
     * @param paths the paths to create
     *
     * @return a new file
     */
    public static File createFile(final File base, final String... paths) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String path : paths) {
            sb.append(path);
            if (!path.endsWith(File.separator) && (++count < paths.length)) {
                sb.append(File.separator);
            }
        }
        return new File(base, sb.toString());
    }

    /**
     * Creates a new file based on the path elements.
     *
     * @param paths the path elements
     *
     * @return a new file
     */
    public static String createPath(final String... paths) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String path : paths) {
            sb.append(path);
            if (!path.endsWith(File.separator) && (++count < paths.length)) {
                sb.append(File.separator);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the temporary directory.
     *
     * @return the temporary directory
     */
    public static File getTempDirectory() {
        return new File(getProperty(TMP_DIR_PROPERTY));
    }

    public static boolean extractAppServer(final String zipPath, final File target) throws IOException {
        return extractAppServer(zipPath, target, false);
    }

    public static boolean extractAppServer(final String zipPath, final File target, final boolean overwrite) throws IOException {
        if (target.exists() && !overwrite) {
            throw new IllegalStateException(Messages.INSTANCE.getMessage("files.not.empty.directory"));
        }
        // Create a temporary directory
        final File tmpDir = new File(getTempDirectory(), "jboss-as-" + zipPath.hashCode());
        if (tmpDir.exists()) {
            deleteRecursively(tmpDir);
        }
        try {
            final byte buff[] = new byte[1024];
            ZipFile file = null;
            try {
                file = new ZipFile(zipPath);
                final Enumeration<ZipArchiveEntry> entries = file.getEntries();
                while (entries.hasMoreElements()) {
                    final ZipArchiveEntry entry = entries.nextElement();
                    // Create the extraction target
                    final File extractTarget = new File(tmpDir, entry.getName());
                    if (entry.isDirectory()) {
                        extractTarget.mkdirs();
                    } else {
                        final File parent = new File(extractTarget.getParent());
                        parent.mkdirs();
                        final BufferedInputStream in = new BufferedInputStream(file.getInputStream(entry));
                        try {
                            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(extractTarget));
                            try {
                                int read;
                                while ((read = in.read(buff)) != -1) {
                                    out.write(buff, 0, read);
                                }
                            } finally {
                                Streams.safeClose(out);
                            }
                        } finally {
                            Streams.safeClose(in);
                        }
                        // Set the file permissions
                        if (entry.getUnixMode() > 0) {
                            setPermissions(extractTarget, FilePermissions.of(entry.getUnixMode()));
                        }
                    }
                }
            } catch (IOException e) {
                throw new IOException(Messages.INSTANCE.getMessage("files.extraction.error", file), e);
            } finally {
                ZipFile.closeQuietly(file);
                // Streams.safeClose(file);
            }
            // If the target exists, remove then rename
            if (target.exists()) {
                deleteRecursively(target);
            }
            // First child should be a directory and there should only be one child
            final File[] children = tmpDir.listFiles();
            if (children != null && children.length == 1) {
                return moveDirectory(children[0], target);
            }
            return moveDirectory(tmpDir, target);

        } finally {
            deleteRecursively(tmpDir);
        }
    }

    /**
     * Checks to see if a directory is empty.
     *
     * @param dir the directory to check
     *
     * @return {@code true} if the directory is empty, otherwise {@code false}
     *
     * @throws IllegalArgumentException if the parameter in is not a directory
     */
    public static boolean isEmptyDirectory(final File dir) {
        if (!dir.exists()) return true;
        if (dir.isDirectory()) {
            final String[] files = dir.list();
            return files == null || files.length == 0;
        }
        throw new IllegalArgumentException(Messages.INSTANCE.getMessage("files.not.directory", dir));
    }

    /**
     * Recursively deletes all files and directories within the directory as well as the directory itself.
     * <p/>
     * On a failure this does not preserve files that have been deleted.
     *
     * @param dir the directory to delete
     *
     * @return {@code true} if the directory and all it's contents were deleted, otherwise {@code false}
     */
    public static boolean deleteRecursively(final File dir) {
        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();
            if (files != null) {
                for (final File f : files) {
                    if (f.isDirectory()) {
                        if (!deleteRecursively(f)) {
                            return false;
                        }
                    } else if (!f.delete()) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * Recursively copies a directories contents to the target directory then deletes the source directory.
     * <p/>
     * If a non-fatal failure occurs the {@code srcDir} might be partially deleted if the failure occurred during the
     * delete, otherwise it should be left as is. The {@code targetDir} may or may not have been created and may or may
     * not contain some or all of the contents from the {@code srcDir}.
     *
     * @param srcDir    the source directory
     * @param targetDir the target directory
     *
     * @return {@code true} if the contents were successfully copied, {@code false} if the contents were not copied or
     *         partially copied but a failure occurred
     *
     * @throws IOException if an IO error occurs copying a file or creating directories
     */
    public static boolean moveDirectory(final File srcDir, final File targetDir) throws IOException {
        boolean result = srcDir.renameTo(targetDir);
        // First try rename
        if (!result) {
            result = copyDirectory(srcDir, targetDir) && deleteRecursively(srcDir);
        }
        return result;
    }

    /**
     * Recursively copies a directories contents to the target directory.
     *
     * @param srcDir    the source directory
     * @param targetDir the target directory
     *
     * @return {@code true} if the contents were successfully copied, {@code false} if the contents were not copied or
     *         partially copied but a failure occurred
     *
     * @throws IOException if an IO error occurs copying a file or creating directories
     */
    public static boolean copyDirectory(final File srcDir, final File targetDir) throws IOException {
        final File[] contents = srcDir.listFiles();
        for (File file : contents != null ? contents : new File[0]) {
            final File target = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                target.mkdirs();
                if (!copyDirectory(file, target)) {
                    return false;
                }
            } else {
                target.getParentFile().mkdirs();
                if (!copyFile(file, target)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Copies the source file to the destination file.
     *
     * @param srcFile    the file to copy
     * @param targetFile the target file
     *
     * @return {@code true} if the file was successfully copied, {@code false} if the copy failed or was incomplete
     *
     * @throws IOException if an IO error occurs copying the file
     */
    public static boolean copyFile(final File srcFile, final File targetFile) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            in = new FileInputStream(srcFile);
            inChannel = in.getChannel();
            out = new FileOutputStream(targetFile);
            outChannel = out.getChannel();
            long bytesTransferred = 0;
            while (bytesTransferred < inChannel.size()) {
                bytesTransferred += inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        } finally {
            Streams.safeClose(outChannel);
            Streams.safeClose(out);
            Streams.safeClose(inChannel);
            Streams.safeClose(in);
        }
        return srcFile.length() == targetFile.length();
    }

    /**
     * Sets the permissions on the file.
     *
     * @param file        the file
     * @param permissions the permissions to set
     */
    public static void setPermissions(final File file, final FilePermissions permissions) {
        file.setExecutable(permissions.owner().canExecute(), !(permissions.group().canExecute() && permissions.pub()
                .canExecute()));
        file.setReadable(permissions.owner().canWrite(), !(permissions.group().canWrite() && permissions.pub()
                .canWrite()));
        file.setWritable(permissions.owner().canWrite(), !(permissions.group().canWrite() && permissions.pub()
                .canWrite()));
    }

    static String getProperty(final String key) {
        if (System.getSecurityManager() == null) {
            return System.getProperty(key);
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }
}
