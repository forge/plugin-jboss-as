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

import java.io.File;
import javax.security.auth.callback.CallbackHandler;

/**
 * The configuration used to launch JBoss Application Server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ServerConfiguration {

    /**
     * The default host name
     */
    final String DEFAULT_HOSTNAME = "localhost";

    /**
     * The default management port
     */
    final int DEFAULT_PORT = 9999;

    /**
     * Returns the hostname of the management console to attach to.
     * <p/>
     * By default {@link #DEFAULT_HOSTNAME} is returned.
     *
     * @return the hostname
     */
    String getHostname();

    /**
     * Returns the management port to attach to.
     * <p/>
     * By default {@link #DEFAULT_PORT} is returned.
     *
     * @return the management port
     */
    int getPort();

    /**
     * Returns the security callback handler used if a username and password are required when attaching to the
     * management console.
     *
     * @return the callback handler
     */
    CallbackHandler getCallbackHandler();

    /**
     * Returns the JBoss home directory.
     * <p/>
     * This is the directory where JBoss application server is installed and cannot be {@code null}.
     *
     * @return the JBoss home directory.
     */
    File getJbossHome();

    /**
     * Returns the directory to the modules for JBoss Application Server.
     * <p/>
     * By default {@link #getJbossHome() JBOSS_HOME/modules} is returned,
     *
     * @return the modules directory
     */
    File getModulesDir();

    /**
     * Returns the directory to the bundles for JBoss Application Server.
     * <p/>
     * By default {@link #getJbossHome() JBOSS_HOME/bundles} is returned,
     *
     * @return the bundles directory
     */
    File getBundlesDir();

    /**
     * Returns an array of the JVM arguments to pass to the Java command when launching the server.
     *
     * @return the JVM arguments or {@code null} if there are none set
     */
    String[] getJvmArgs();

    /**
     * Returns the Java home directory.
     * <p/>
     * By default the {@literal JAVA_HOME} environment variable is used. If {@code null} is returned the server may not
     * properly work.
     *
     * @return the Java home directory
     */
    String getJavaHome();

    /**
     * Returns the server configuration file or {@code null} if using the default configuration file.
     *
     * @return the server configuration file or {@code null}
     */
    String getServerConfigFile();

    /**
     * Returns the timeout to wait for the server to successfully start.
     * <p/>
     * A number greater than 0 should be returned. Any default value greater than 0 is acceptable.
     *
     * @return the timeout
     */
    long getStartupTimeout();

    /**
     * The version of the JBoss Application Server to use.
     * <p/>
     * By default the {@link org.jboss.as.forge.Versions#defaultVersion()} will be returned.
     *
     * @return the version of the JBoss Application Server to use
     */
    Version getVersion();
}
