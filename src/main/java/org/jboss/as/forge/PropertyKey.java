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

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class PropertyKey {

    public static final String AS7 = "as7";

    public static final String CONFIGURED = generateKey(AS7, "configured");

    public static final String HOSTNAME = generateKey(AS7, "hostname");

    public static final String JAVA_HOME = generateKey("java-home");

    public static final String JBOSS_HOME = generateKey(AS7, "jboss-home");

    public static final String JBOSS_AS_VERSION = generateKey(AS7, "version");

    public static final String PORT = generateKey(AS7, "port");

    public static final String SERVER_CONFIG_FILE = generateKey(AS7, "server-config");

    public static final String SERVER_STARTUP_TIMEOUT = generateKey(AS7, "timeout");

    private static final String BASE = "jboss-as";

    private static String generateKey(final String... args) {
        StringBuilder result = new StringBuilder(BASE);
        for (String arg : args)
            result = result.append(".").append(arg);
        return result.toString();
    }
}
