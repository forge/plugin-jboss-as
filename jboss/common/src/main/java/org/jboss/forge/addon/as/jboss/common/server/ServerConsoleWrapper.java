/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerConsoleWrapper extends OutputStream implements Closeable {

    private final File file;
    private final FileOutputStream out;

    public ServerConsoleWrapper() throws IOException {
        super();
        file = File.createTempFile("jboss-console", ".log");
        file.deleteOnExit();
        out = new FileOutputStream(file);
    }

    public List<String> readAllLines() throws IOException {
        final List<String> result = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> readLines(final int numberOfLines) throws IOException {
        final List<String> result = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        final int end = result.size();
        final int start = end - numberOfLines;
        if (start > 0) {
            return Collections.unmodifiableList(result.subList(start, result.size()));
        } else {
            return Collections.unmodifiableList(result);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
        file.delete();
    }
}
