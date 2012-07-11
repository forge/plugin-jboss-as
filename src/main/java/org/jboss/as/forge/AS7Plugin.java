/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import java.io.IOException;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Alias("as7")
@RequiresProject
@RequiresFacet(AS7ServerFacet.class)
public class AS7Plugin implements Plugin {
    @Inject
    private Shell shell;

    @Inject
    private Event<InstallFacets> install;

    @Inject
    private Project project;

    @Inject
    private Versions versions;

    @Inject
    private ServerConfigurator serverConfigurator;


    @SetupCommand
    public void setup(final PipeOut out) {
        // Don't run the configuration if the server is running
        if (project.hasFacet(AS7ServerFacet.class)) {
            final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
            if (serverFacet.isStarted()) {
                ShellMessages.error(shell, "Cannot run configuration while server is running.");
            } else {
                project.getFacet(AS7ServerFacet.class).configure();
            }
        }
        // Make sure facets are installed
        if (!project.hasFacet(AS7ServerFacet.class)) {
            install.fire(new InstallFacets(AS7ServerFacet.class));
        }
    }

    @DefaultCommand
    public void checkProperties(final PipeOut out,
                                @Option(name = "version", shortName = "v", flagOnly = true) final boolean version,
                                @Option(name = "jboss-home", flagOnly = true) final boolean jbossHome,
                                @Option(name = "java-home", flagOnly = true) final boolean javaHome) {
        final ServerConfiguration defaultConfig = serverConfigurator.defaultConfiguration();
        if (version) shell.println(String.format("Version: %s", defaultConfig.getVersion()));
        if (jbossHome) shell.println(String.format("JBoss Home: %s", defaultConfig.getJbossHome()));
        if (javaHome) shell.println(String.format("Java Home: %s", defaultConfig.getJavaHome()));
    }

    @Command(value = "install", help = "Downloads, if needed, and installs JBoss Application Server to the specified directory.")
    public void downloadAndInstall(final PipeOut out, @Option(required = true) final Resource<?> jbossHome,
                                   @Option(name = "version", completer = VersionCompleter.class, required = true) final String version) throws Exception {
        boolean ok = true;
        if (!(jbossHome instanceof FileResource && ((FileResource<?>) jbossHome).isDirectory())) {
            ok = false;
            ShellMessages.error(shell, String.format("JBoss Home '%s' is not a directory.", jbossHome.getFullyQualifiedName()));
        }
        // Valid the version
        if (!versions.isValidVersion(version)) {
            ok = false;
            ShellMessages.error(shell, String.format("Version '%s' is invalid. Must be one of: %s", version, versions.getVersions()));
        }
        if (ok) {
            shell.println(String.format("JBoss AS %s downloaded and installed at: %s", version,
                    project.getFacet(AS7ServerFacet.class).downloadAndInstall(new File(jbossHome.getFullyQualifiedName()), versions.fromString(version))));
        }
    }

    @Command
    public void deploy(final PipeOut out,
                       @Option(name = "force", shortName = "f", defaultValue = "true") final boolean force,
                       @Option(name = "hostname", shortName = "h") final String hostname,
                       @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        if (hostname != null) serverConfigurator.setHostname(hostname);
        if (port > 0) serverConfigurator.setPort(port);
        project.getFacet(AS7ServerFacet.class).deploy(null, serverConfigurator.configure(), force);
    }

    @Command
    public void redeploy(final PipeOut out,
                         @Option(name = "hostname", shortName = "h") final String hostname,
                         @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        if (hostname != null) serverConfigurator.setHostname(hostname);
        if (port > 0) serverConfigurator.setPort(port);
        project.getFacet(AS7ServerFacet.class).redeploy(null, serverConfigurator.configure());
    }

    @Command
    public void undeploy(final PipeOut out,
                         @Option(name = "ignore-missing", shortName = "i", defaultValue = "true") final boolean ignoreMissing,
                         @Option(name = "hostname", shortName = "h") final String hostname,
                         @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        if (hostname != null) serverConfigurator.setHostname(hostname);
        if (port > 0) serverConfigurator.setPort(port);
        project.getFacet(AS7ServerFacet.class).undeploy(null, serverConfigurator.configure(), ignoreMissing);
    }

    @Command
    public void start(final PipeOut out, @Option(name = "jboss-home") final Resource<?> jbossHome,
                      @Option(name = "java-home") final String javaHome,
                      @Option(name = "version", completer = VersionCompleter.class) final String version) throws Exception {
        if (needsSetUp()) return;
        boolean ok = true;

        // Version always needs to be set first
        if (version != null) {
            if (versions.isValidVersion(version)) {
                serverConfigurator.setVersion(versions.fromString(version));
            } else {
                ok = false;
                ShellMessages.error(shell, String.format("Invalid version '%s'. Valid versions: %s", version, versions.getAllVersions()));
            }
        }

        // Set-up the startup configuration
        if (javaHome != null) {
            serverConfigurator.setJavaHome(javaHome);
        }
        if (jbossHome != null) {
            if (jbossHome instanceof FileResource && ((FileResource<?>) jbossHome).isDirectory()) {
                serverConfigurator.setJbossHome(new File(jbossHome.getFullyQualifiedName()));
            } else {
                ok = false;
                ShellMessages.error(shell, String.format("JBoss Home '%s' is not a directory.", jbossHome.getFullyQualifiedName()));
            }
        }
        // Get the server facet
        if (ok) project.getFacet(AS7ServerFacet.class).start(serverConfigurator.configure());
    }

    @Command(help = "Checks the status of the server.")
    public void status(final PipeOut out) throws Exception {
        if (needsSetUp()) return;
        project.getFacet(AS7ServerFacet.class).status();
    }

    @Command
    public void shutdown(final PipeOut out) throws Exception {
        if (needsSetUp()) return;
        project.getFacet(AS7ServerFacet.class).shutdown();
    }

    @Command("execute-command")
    public void executeCliCommand(final PipeOut out,
                                  @Option(description = "The CLI command to execute.", required = true) final String cmd) throws IOException {
        if (needsSetUp()) return;
        project.getFacet(AS7ServerFacet.class).executeCommand(cmd);

    }

    private boolean needsSetUp() {
        if (serverConfigurator.hasConfiguration()) {
            return false;
        }
        ShellMessages.error(shell, "The server is not configured. Please run the setup command.");
        return true;
    }
}
