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
import java.util.Arrays;
import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.forge.server.ClientCallbackHandler;
import org.jboss.as.forge.util.Files;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationScope;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequiresProject
@RequiresFacet(PackagingFacet.class)
public class ProjectServerConfigurator implements ServerConfigurator {

    @Inject
    private Project project;

    @Inject
    private ClientCallbackHandler callbackHandler;

    @Inject
    private Configuration configuration;

    @Inject
    private Versions versions;

    private String hostname;
    private int port;
    private File jbossHome;
    private File modulesDir;
    private File bundlesDir;
    private String[] jvmArgs;
    private String javaHome;
    private String serverConfigFile;
    private long startupTimeout;
    private Version version;

    private void initConfig() {
        // Currently only set on a per project basis
        final Configuration projectConfig = configuration.getScopedConfiguration(ConfigurationScope.PROJECT);
        if (!projectConfig.containsKey(PropertyKey.CONFIGURED))
            projectConfig.setProperty(PropertyKey.CONFIGURED, ConfigurationScope.PROJECT.toString());
    }

    @Override
    public boolean hasConfiguration() {
        return configuration.getScopedConfiguration(ConfigurationScope.PROJECT).containsKey(PropertyKey.CONFIGURED);
    }

    @Override
    public ServerConfiguration configure() {
        final PropertyServerConfiguration serverConfiguration = createDefaultConfiguration();
        // Check the host name
        if (hostname != null) serverConfiguration.hostname = hostname;

        // Check the port
        if (port > 0) serverConfiguration.port = port;

        // Check the version
        if (version != null) serverConfiguration.version = version;

        // JBoss home will be overridden if the version is overridden
        final File jbossHome;
        if (this.jbossHome == null && version != null) {
            jbossHome = formatDefaultJbossHome(version);
        } else {
            jbossHome = (this.jbossHome == null ? serverConfiguration.jbossHome : this.jbossHome);
        }

        // Check the JBoss Home
        serverConfiguration.jbossHome = jbossHome;

        // Check the modules dir
        serverConfiguration.modulesDir = (this.modulesDir == null ? Files.createFile(jbossHome, "modules") : this.modulesDir);

        // Check the bundles dir
        serverConfiguration.bundlesDir = (this.bundlesDir == null ? Files.createFile(jbossHome, "bundles") : this.bundlesDir);

        // Check the JVM arguments
        if (jvmArgs != null) serverConfiguration.jvmArgs = Arrays.copyOf(jvmArgs, jvmArgs.length);

        // Check the Java Home
        if (javaHome != null) serverConfiguration.javaHome = javaHome;

        // Check the server configuration file
        if (serverConfigFile != null) serverConfiguration.serverConfigFile = serverConfigFile;

        // Check the timeout
        if (startupTimeout > 0) serverConfiguration.startupTimeout = startupTimeout;

        return serverConfiguration;
    }

    @Override
    public ServerConfiguration defaultConfiguration() {
        return createDefaultConfiguration();
    }

    @Override
    public void writeConfiguration() {
        final Configuration configuration = getConfiguration();
        if (hostname == null) {
            configuration.clearProperty(PropertyKey.HOSTNAME);
        } else {
            configuration.setProperty(PropertyKey.HOSTNAME, hostname);
        }
        if (port > 0) {
            configuration.setProperty(PropertyKey.PORT, port);
        } else {
            configuration.clearProperty(PropertyKey.PORT);
        }
        // TODO  allow for properties
        if (jbossHome == null) {
            configuration.clearProperty(PropertyKey.JBOSS_HOME);
        } else {
            configuration.setProperty(PropertyKey.JBOSS_HOME, jbossHome.getAbsolutePath());
        }
        // TODO  allow for properties
        if (modulesDir == null) {
            configuration.clearProperty(PropertyKey.MODULES_DIR);
        } else {
            configuration.setProperty(PropertyKey.MODULES_DIR, modulesDir.getAbsolutePath());
        }
        // TODO  allow for properties
        if (bundlesDir == null) {
            configuration.clearProperty(PropertyKey.BUNDLES_DIR);
        } else {
            configuration.setProperty(PropertyKey.BUNDLES_DIR, bundlesDir.getAbsolutePath());
        }
        if (jvmArgs != null && jvmArgs.length > 0) {
            configuration.setProperty(PropertyKey.JVM_ARGS, Arrays.asList(jvmArgs));
        } else {
            configuration.clearProperty(PropertyKey.JVM_ARGS);
        }
        // TODO  allow for properties
        if (javaHome == null) {
            configuration.clearProperty(PropertyKey.JAVA_HOME);
        } else {
            configuration.setProperty(PropertyKey.JAVA_HOME, javaHome);
        }
        // TODO  allow for properties
        if (serverConfigFile == null) {
            configuration.clearProperty(PropertyKey.SERVER_CONFIG_FILE);
        } else {
            configuration.setProperty(PropertyKey.SERVER_CONFIG_FILE, serverConfigFile);
        }
        if (startupTimeout > 0) {
            configuration.setProperty(PropertyKey.SERVER_STARTUP_TIMEOUT, startupTimeout);
        } else {
            configuration.clearProperty(PropertyKey.SERVER_STARTUP_TIMEOUT);
        }

        reset();
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public File getJbossHome() {
        return jbossHome;
    }

    @Override
    public void setJbossHome(final File jbossHome) {
        this.jbossHome = jbossHome;
    }

    @Override
    public File getModulesDir() {
        return modulesDir;
    }

    @Override
    public void setModulesDir(final File modulesDir) {
        this.modulesDir = modulesDir;
    }

    @Override
    public File getBundlesDir() {
        return bundlesDir;
    }

    @Override
    public void setBundlesDir(final File bundlesDir) {
        this.bundlesDir = bundlesDir;
    }

    @Override
    public String[] getJvmArgs() {
        return jvmArgs;
    }

    @Override
    public void setJvmArgs(final String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    @Override
    public String getJavaHome() {
        return javaHome;
    }

    @Override
    public void setJavaHome(final String javaHome) {
        this.javaHome = javaHome;
    }

    @Override
    public String getServerConfigFile() {
        return serverConfigFile;
    }

    @Override
    public void setServerConfigFile(final String serverConfigFile) {
        this.serverConfigFile = serverConfigFile;
    }

    @Override
    public long getStartupTimeout() {
        return startupTimeout;
    }

    @Override
    public void setStartupTimeout(final long startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public void setVersion(final Version version) {
        this.version = version;
    }

    private String getProperty(final String key, final String defaultValue) {
        return getConfiguration().getString(key, defaultValue);
    }

    private File getProperty(final String key, final File defaultValue) {
        final Configuration configuration = getConfiguration();
        if (configuration.containsKey(key)) {
            return new File(configuration.getString(key));
        }
        return defaultValue;
    }

    private int getProperty(final String key, final int defaultValue) {
        return getConfiguration().getInt(key, defaultValue);
    }

    private long getProperty(final String key, final long defaultValue) {
        return getConfiguration().getLong(key, defaultValue);
    }

    private Configuration getConfiguration() {
        if (!hasConfiguration()) {
            throw new IllegalStateException("No configuration found, please set the configuration target.");
        }
        final ConfigurationScope scope = ConfigurationScope.valueOf(configuration.getScopedConfiguration(ConfigurationScope.PROJECT).getString(PropertyKey.CONFIGURED));
        return configuration.getScopedConfiguration(scope);
    }

    protected File formatDefaultJbossHome(final Version version) {
        return Files.createFile(getProjectTarget(), version.getArchiveDir());
    }

    protected File getProjectTarget() {
        final PackagingFacet packaging = project.getFacet(PackagingFacet.class);
        return new File(packaging.getFinalArtifact().getParent().getFullyQualifiedName(), "jboss-as-dist");
    }

    private PropertyServerConfiguration createDefaultConfiguration() {
        initConfig();
        final Configuration configuration = getConfiguration();
        final PropertyServerConfiguration serverConfiguration = new PropertyServerConfiguration(callbackHandler);
        // Check the host name
        serverConfiguration.hostname = getProperty(PropertyKey.HOSTNAME, ServerConfiguration.DEFAULT_HOSTNAME);
        // Check the port
        serverConfiguration.port = getProperty(PropertyKey.PORT, ServerConfiguration.DEFAULT_PORT);
        // Check the version
        final Version version = configuration.containsKey(PropertyKey.JBOSS_AS_VERSION) ?
                versions.fromString(configuration.getString(PropertyKey.JBOSS_AS_VERSION)) :
                versions.defaultVersion();
        serverConfiguration.version = version;
        // JBoss home will be overridden if the version is overridden
        final File jbossHome = getProperty(PropertyKey.JBOSS_HOME, formatDefaultJbossHome(version));
        // Check the JBoss Home
        serverConfiguration.jbossHome = jbossHome;
        // Check the modules dir
        serverConfiguration.modulesDir = Files.createFile(jbossHome, "modules");
        // Check the bundles dir
        serverConfiguration.bundlesDir = Files.createFile(jbossHome, "bundles");
        // Check the Java Home
        serverConfiguration.javaHome = getProperty(PropertyKey.JAVA_HOME, SecurityActions.getEnvironmentVariable("JAVA_HOME"));
        // Check the server configuration file
        serverConfiguration.serverConfigFile = getProperty(PropertyKey.SERVER_CONFIG_FILE, (String) null);
        // Check the timeout
        serverConfiguration.startupTimeout = getProperty(PropertyKey.SERVER_STARTUP_TIMEOUT, 60L);

        return serverConfiguration;
    }

    private void reset() {
        hostname = null;
        port = 0;
        jbossHome = null;
        modulesDir = null;
        bundlesDir = null;
        jvmArgs = null;
        javaHome = null;
        serverConfigFile = null;
        startupTimeout = 0L;
        version = null;
    }

    static class PropertyServerConfiguration implements ServerConfiguration {
        private final ClientCallbackHandler callbackHandler;
        private String hostname;
        private int port;
        private File jbossHome;
        private File modulesDir;
        private File bundlesDir;
        private String[] jvmArgs;
        private String javaHome;
        private String serverConfigFile;
        private long startupTimeout;
        private Version version;

        PropertyServerConfiguration(final ClientCallbackHandler callbackHandler) {
            this.callbackHandler = callbackHandler;
        }

        @Override
        public String getHostname() {
            return hostname;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public CallbackHandler getCallbackHandler() {
            return callbackHandler;
        }

        @Override
        public File getJbossHome() {
            return jbossHome;
        }

        @Override
        public File getModulesDir() {
            return modulesDir;
        }

        @Override
        public File getBundlesDir() {
            return bundlesDir;
        }

        @Override
        public String[] getJvmArgs() {
            return jvmArgs;
        }

        @Override
        public String getJavaHome() {
            return javaHome;
        }

        @Override
        public String getServerConfigFile() {
            return serverConfigFile;
        }

        @Override
        public long getStartupTimeout() {
            return startupTimeout;
        }

        @Override
        public Version getVersion() {
            return version;
        }
    }
}
