/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.util;

import java.io.Closeable;
import java.io.Flushable;
import java.util.zip.ZipFile;

/**
 * Utilities for handling streams.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Streams {


    public static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception e) {
            // no-op
        }
    }

    public static void safeClose(final ZipFile file) {
        if (file != null) try {
            file.close();
        } catch (Exception e) {
            // no-op
        }
    }


    public static void safeFlush(final Flushable flushable) {
        if (flushable != null) try {
            flushable.flush();
        } catch (Exception e) {
            // no-op
        }
    }
}
