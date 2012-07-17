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

/**
 * An interface to build a {@link ServerConfiguration server configuration}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ServerConfigurator {

    /**
     * Checks to see if a configuration exists.
     * <p/>
     * If not configuration was previously written {@code false} is returned. If a previous configuration is found
     * {@code true} is returned.
     *
     * @return {@code true} if a configuration was previously written, otherwise {@code false}
     */
    boolean hasConfiguration();

    /**
     * Create the server configuration.
     *
     * @return the server configuration
     */
    ServerConfiguration configure();

    /**
     * Returns the default configuration.
     * <p/>
     * This configuration contains no values set via the configurator.
     *
     * @return the default configuration
     */
    ServerConfiguration defaultConfiguration();

    /**
     * Writes the configuration file and clears any current changes to the configurator.
     * <p/>
     * Note that any properties not set will be cleared from the configuration if they previously existed.
     */
    void writeConfiguration();

    /**
     * Returns the currently set hostname or {@code null} if no hostname has been set.
     *
     * @return the hostname or {@code null}
     */
    String getHostname();

    /**
     * Sets the host name to use for the server configuration.
     *
     * @param hostname the host name of the server
     */
    void setHostname(final String hostname);

    /**
     * Returns the currently set management port or 0 if the port has not been set.
     *
     * @return the port or 0
     */
    int getPort();

    /**
     * Sets the management port to use for the server configuration.
     *
     * @param port the management port to connect to
     */
    void setPort(final int port);

    /**
     * Returns the currently set JBoss home location or {@code null} if the JBoss home has not been set.
     *
     * @return the JBoss home location or {@code null}
     */
    File getJbossHome();

    /**
     * Sets the JBoss home directory to use for the server configuration.
     * <p/>
     * This should be the root directory where JBoss Application Server is installed.
     *
     * @param jbossHome the JBoss home directory
     */
    void setJbossHome(final File jbossHome);

    /**
     * Returns the currently set directory that holds the modules or {@code null} if the modules directory has not been
     * set.
     *
     * @return the modules directory or {@code null}
     */
    File getModulesDir();

    /**
     * Sets the modules directory for the server configuration.
     *
     * @param modulesDir the modules directory
     */
    void setModulesDir(final File modulesDir);

    /**
     * Returns the currently set directory for the bundles or {@code null} if the bundles directory has not been set.
     *
     * @return the bundles directory or {@code null}
     */
    File getBundlesDir();

    /**
     * Sets the bundles directory for the server configuration.
     *
     * @param bundlesDir the bundles directory
     */
    void setBundlesDir(final File bundlesDir);

    /**
     * Returns the currently set JVM arguments or {@code null} if there are no JVM arguments set.
     *
     * @return the JVM arguments or {@code null}
     */
    String[] getJvmArgs();

    /**
     * Sets the JVM arguments used when launching the server.
     *
     * @param jvmArgs the JVM arguments to use
     */
    void setJvmArgs(final String[] jvmArgs);

    /**
     * Returns the currently set Java home or {@code null} if the Java home has not been set.
     *
     * @return the Java home or {@code null}
     */
    String getJavaHome();

    /**
     * Sets the Java home for locating the java command to launch the server.
     *
     * @param javaHome the Java home directory
     */
    void setJavaHome(final String javaHome);

    /**
     * Returns the currently set server configuration file used for launching the server or {@code null} if the
     * configuration file has not been set.
     *
     * @return the path to the configuration file or {@code null}
     */
    String getServerConfigFile();

    /**
     * Sets the path to the configuration file to use for the launching the server.
     *
     * @param serverConfigFile the path to the configuration file
     */
    void setServerConfigFile(final String serverConfigFile);

    /**
     * Returns the currently set startup timeout or 0 if the startup timeout is not set.
     *
     * @return the startup timeout or 0
     */
    long getStartupTimeout();

    /**
     * Sets the timeout to wait for the server to successfully start.
     *
     * @param startupTimeout the startup timeout
     */
    void setStartupTimeout(final long startupTimeout);

    /**
     * Returns the currently set default version of the application server to use or {@code null} if the version has
     * not been set.
     *
     * @return the server version or {@code null}
     */
    Version getVersion();

    /**
     * Sets the version of the application server to use.
     *
     * @param version the version of the application server
     */
    void setVersion(final Version version);
}
