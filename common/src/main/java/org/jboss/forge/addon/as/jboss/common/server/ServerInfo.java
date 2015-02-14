/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.server;

import java.io.File;
import java.io.OutputStream;

import org.jboss.forge.addon.as.jboss.common.util.Files;

/**
 * Server configuration information.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerInfo {
    private final ConnectionInfo connectionInfo;
    private final File jbossHome;
    private final File modulesDir;
    private final String[] jvmArgs;
    private final String javaHome;
    private final String serverConfig;
    private final String propertiesFile;
    private final long startupTimeout;
    private final OutputStream out;
    
    private ServerInfo(final ConnectionInfo connectionInfo, final String javaHome, final File jbossHome, final String modulesDir, final String[] jvmArgs, final String serverConfig, final String propertiesFile, final long startupTimeout, final OutputStream out) {
        this.connectionInfo = connectionInfo;
        this.javaHome = javaHome;
        this.jbossHome = jbossHome;
        this.modulesDir = (modulesDir == null ? Files.createFile(jbossHome, "modules") : new File(modulesDir));
        this.jvmArgs = jvmArgs;
        this.serverConfig = serverConfig;
        this.propertiesFile = propertiesFile;
        this.startupTimeout = startupTimeout;
        this.out = out!=null?out:System.out;
    }

    /**
     * Creates the server information.
     *
     * @param connectionInfo the connection information for the management client
     * @param javaHome       the Java home directory
     * @param jbossHome      the home directory for the JBoss Application Server
     * @param modulesDir     the directory for the modules to use
     * @param jvmArgs        the JVM arguments
     * @param serverConfig   the path to the servers configuration file
     * @param startupTimeout the startup timeout
     *
     * @return the server configuration information
     */
    public static ServerInfo of(final ConnectionInfo connectionInfo, final String javaHome, final File jbossHome, final String modulesDir, final String[] jvmArgs, final String serverConfig, final String propertiesFile, final long startupTimeout, final OutputStream out) {
        return new ServerInfo(connectionInfo, javaHome, jbossHome, modulesDir, jvmArgs, serverConfig, propertiesFile, startupTimeout, out);
    }

    /**
     * The connection information for the management operations.
     *
     * @return the connection information
     */
    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    /**
     * The JBoss Application Server home directory.
     *
     * @return the home directory
     */
    public File getJbossHome() {
        return jbossHome;
    }

    /**
     * The directory for all the modules.
     *
     * @return the modules directory
     */
    public File getModulesDir() {
        return modulesDir;
    }

    /**
     * The optional JVM arguments.
     *
     * @return the JVM arguments or {@code null} if there are none
     */
    public String[] getJvmArgs() {
        return jvmArgs;
    }

    /**
     * The Java home directory.
     *
     * @return the Java home directory
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * The path to the server configuration file to use.
     *
     * @return the path to the configuration file or {@code null} if the default configuration file is being used
     */
    public String getServerConfig() {
        return serverConfig;
    }

    /**
     * The path to the system properties file to load.
     *
     * @return the path to the properties file or {@code null} if no properties should be loaded.
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * The timeout to use for the server startup.
     *
     * @return the server startup timeout
     */
    public long getStartupTimeout() {
        return startupTimeout;
    }

   public OutputStream getOut()
   {
      return out;
   }
}
