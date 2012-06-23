/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.forge;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.forge.server.ClientCallbackHandler;
import org.jboss.as.forge.util.Files;
import org.jboss.as.forge.util.Streams;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationScope;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.resources.DependencyResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerConfiguration {

    private static final FileFilter FILTER = new FileFilter() {
        @Override
        public boolean accept(final File file) {
            return file.isFile() && JBOSS_MODULES_JAR.equals(file.getName());
        }
    };

    public static final String JBOSS_MODULES_JAR = "jboss-modules.jar";

    public static final String DEFAULT_HOSTNAME = "localhost";

    public static final int DEFAULT_PORT = 9999;

    @Inject
    private DependencyResolver dependencyResolver;

    @Inject
    private Versions versions;

    @Inject
    private ClientCallbackHandler callbackHandler;

    @Inject
    private Configuration configuration;

    @Inject
    private Shell shell;

    private String hostname;
    private int port;
    private File jbossHome;
    private File modulesDir;
    private File bundlesDir;
    private String[] jvmArgs;
    private String javaHome;
    private String serverConfig;
    private long startupTimeout;
    private Version version;

    public ServerConfiguration() {
        hostname = DEFAULT_HOSTNAME;
        port = DEFAULT_PORT;
        // version = versions.defaultVersion();
    }

    @PostConstruct
    protected void init() {
        if (isConfigured()) {
            final Configuration configuration = getConfiguration();
            hostname = configuration.getString(PropertyKey.HOSTNAME, DEFAULT_HOSTNAME);
            port = configuration.getInt(PropertyKey.PORT, DEFAULT_PORT);
            jbossHome = getFile(configuration, PropertyKey.JBOSS_HOME);
            javaHome = configuration.getString(PropertyKey.JAVA_HOME, SecurityActions.getEnvironmentVariable("JAVA_HOME"));
            // modulesDir = getFile();
            modulesDir = Files.createFile(jbossHome, "modules");
            // bundlesDir = getFile();
            bundlesDir = Files.createFile(jbossHome, "bundles");
            // jvmArgs =
            serverConfig = configuration.getString(PropertyKey.SERVER_CONFIG_FILE, null);
            startupTimeout = configuration.getLong(PropertyKey.SERVER_STARTUP_TIMEOUT, 60L);
            version = (configuration.containsKey(PropertyKey.JBOSS_AS_VERSION) ?
                    versions.fromString(configuration.getString(PropertyKey.JBOSS_AS_VERSION)) :
                    versions.defaultVersion());
        }
    }

    public boolean isConfigured() {
        return configuration.getScopedConfiguration(ConfigurationScope.PROJECT).containsKey(PropertyKey.CONFIGURED);
    }

    private void setConfiguration(final ConfigurationScope scope) {
        configuration.getScopedConfiguration(ConfigurationScope.PROJECT).setProperty(PropertyKey.CONFIGURED, scope.toString());
    }

    private Configuration getConfiguration() {
        if (!isConfigured()) {
            throw new IllegalStateException("No configuration found, please set the configuration target.");
        }
        final ConfigurationScope scope = ConfigurationScope.valueOf(configuration.getScopedConfiguration(ConfigurationScope.PROJECT).getString(PropertyKey.CONFIGURED));
        return configuration.getScopedConfiguration(scope);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public File getJbossHome() {
        return jbossHome;
    }

    public void setJbossHome(final File jbossHome) {
        this.jbossHome = jbossHome;
        // TODO clean this bit up
        modulesDir = Files.createFile(jbossHome, "modules");
        bundlesDir = Files.createFile(jbossHome, "bundles");
    }

    public File getModulesDir() {
        return modulesDir;
    }

    public void setModulesDir(final File modulesDir) {
        this.modulesDir = modulesDir;
    }

    public File getBundlesDir() {
        return bundlesDir;
    }

    public void setBundlesDir(final File bundlesDir) {
        this.bundlesDir = bundlesDir;
    }

    public String[] getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(final String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(final String javaHome) {
        this.javaHome = javaHome;
    }

    public String getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(final String serverConfig) {
        this.serverConfig = serverConfig;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(final long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(final Version version) {
        this.version = version;
    }

    private static File getFile(final Configuration configuration, final String key) {
        return (configuration.containsKey(key) ? new File(configuration.getString(key)) : null);
    }

    // TODO The entire prompting needs to be cleaned up

    /**
     * Configure the settings
     *
     * @return {@code true} if configured correct, otherwise {@code false}
     */
    protected boolean configure(final File targetDir) {
        setConfiguration(ConfigurationScope.PROJECT);
        final Configuration configuration = getConfiguration();

        // Prompt for Java Home
        boolean doPrompt = true;
        if (configuration.containsKey(PropertyKey.JAVA_HOME)) {
            doPrompt = shell.promptBoolean(String.format("The Java Home '%s' is already set, would you like to override it?", configuration.getString(PropertyKey.JAVA_HOME)), false);
        }
        if (doPrompt) {
            final String javaHome = shell.prompt("Enter the Java home directory or leave blank to use the JAVA_HOME environment variable:");
            if (!javaHome.isEmpty()) {
                configuration.setProperty(PropertyKey.JAVA_HOME, javaHome);
                this.javaHome = javaHome;
            }
        }

        // Prompt the user for the version
        doPrompt = true;
        if (configuration.containsKey(PropertyKey.JBOSS_AS_VERSION)) {
            doPrompt = shell.promptBoolean(String.format("A default version of %s is already set, would you like to override it?",
                    configuration.getString(PropertyKey.JBOSS_AS_VERSION)), false);
        }
        if (doPrompt) {
            version = shell.promptChoiceTyped("Choose default JBoss AS version:", versions.getVersions());
            configuration.setProperty(PropertyKey.JBOSS_AS_VERSION, version.toString());
        }

        // Prompt for a path or download
        final String result = shell.prompt("Enter path for JBoss AS or leave blank to download:");
        final File jbossHome;
        if (result.isEmpty()) {
            jbossHome = downloadAndInstall(targetDir);
        } else {
            jbossHome = new File(result);
        }

        // Should never be null, but let's be careful
        if (jbossHome == null) {
            ShellMessages.error(shell, "The JBoss Home was found to be null, something is broken.");
            return false;
        }
        configuration.setProperty(PropertyKey.JBOSS_HOME, jbossHome.getAbsolutePath());
        this.jbossHome = jbossHome;
        return true;
    }

    protected boolean isServerInstalled() {
        final File jbossHome = this.jbossHome;
        final File[] files = jbossHome.listFiles(FILTER);
        return jbossHome.exists() && files != null && files.length > 0;
    }

    protected void downloadAndInstall() {
        downloadAndInstall(jbossHome);
    }

    protected File downloadAndInstall(final File baseDir) {
        if (shell.promptBoolean(String.format("You are about to download JBoss AS %s to '%s' which could take a while. Would you like to continue?", version, baseDir))) {
            final List<DependencyResource> asArchive = dependencyResolver.resolveArtifacts(version.getDependency());
            if (asArchive.isEmpty()) {
                throw new IllegalStateException(String.format("Could not find artifact: %s", version.getDependency()));
            }
            final DependencyResource zipFile = asArchive.get(0);
            // For now just delete the target
            return extract(zipFile, baseDir.getParentFile());
        }
        return null;
    }

    /**
     * Extracts the data from the downloaded zip file.
     *
     * @param zipFile the zip file to extract
     * @param target  the directory the data should be extracted to
     */
    private File extract(final DependencyResource zipFile, final File target) {
        File result = target;
        final byte buff[] = new byte[1024];
        ZipFile file = null;
        try {
            file = new ZipFile(zipFile.getFullyQualifiedName());
            final Enumeration<? extends ZipEntry> entries = file.entries();
            boolean firstEntry = true;
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                // Create the extraction target
                final File extractTarget = new File(target, entry.getName());
                // First entry should be a the base directory
                if (firstEntry) {
                    firstEntry = false;
                    // Confirm override on the extraction target only once
                    if (extractTarget.exists()) {
                        if (shell.promptBoolean(String.format("The target (%s) already exists, would you like to replace the directory?", extractTarget), true)) {
                            Files.deleteRecursively(extractTarget);
                        } else {
                            result = null;
                            break;
                        }
                    }
                    result = extractTarget;
                }
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
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Error extracting '%s'", (file == null ? "null file" : file.getName())), e);
        } finally {
            Streams.safeClose(file);
        }
        return result;
    }
}
