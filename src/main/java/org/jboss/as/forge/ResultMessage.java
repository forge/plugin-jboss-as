/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.PrintWriter;
import java.io.Writer;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ResultMessage {

    public static enum Level {
        SUCCESS,
        INFO,
        WARN,
        ERROR,

    }

    private final Level level;
    private final String message;

    public ResultMessage(final Level level, final String message) {
        this.level = level;
        this.message = message;
    }

    public static ResultMessage of(final Level level, final String message) {
        return new ResultMessage(level, message);
    }

    public static ResultMessage of(final Level level, final Object message) {
        return new ResultMessage(level, String.valueOf(message));
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    private static void write(final Writer writer) {}
}
