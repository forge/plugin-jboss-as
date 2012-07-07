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

import jline.Completor;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.shell.PromptType;
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
@RequiresFacet({AS7Facet.class, AS7ServerFacet.class})
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
        if (!project.hasAllFacets(AS7Facet.class, AS7ServerFacet.class)) {
            install.fire(new InstallFacets(AS7Facet.class, AS7ServerFacet.class));
        } else if (!project.hasFacet(AS7Facet.class)) {
            install.fire(new InstallFacets(AS7Facet.class));
        } else if (!project.hasFacet(AS7ServerFacet.class)) {
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
    public void downloadAndInstall(final PipeOut out, @Option(name = "directory", shortName = "dir", required = true) final File jbossHome,
                                   @Option(name = "version", completer = VersionCompleter.class, required = true) final String version) throws Exception {
        // Valid the version
        if (versions.isValidVersion(version)) {
            shell.println(String.format("JBoss AS %s downloaded and installed at: %s", version,
                    project.getFacet(AS7ServerFacet.class).downloadAndInstall(jbossHome, versions.fromString(version))));
        } else {
            ShellMessages.error(shell, String.format("Version '%s' is invalid. Must be one of: %s", version, versions.getVersions()));
        }
    }

    @Command
    public void deploy(final PipeOut out) throws Exception {
        shell.execute("mvn jboss-as:deploy");
    }

    @Command
    public void redeploy(final PipeOut out) throws Exception {
        shell.execute("mvn jboss-as:redeploy");
    }

    @Command
    public void undeploy(final PipeOut out) throws Exception {
        shell.execute("mvn jboss-as:undeploy");
    }

    @Command
    public void start(final PipeOut out, @Option(name = "jboss-home", type = PromptType.FILE_PATH) final String jbossHome,
                      @Option(name = "java-home") final String javaHome,
                      @Option(name = "version", completer = VersionCompleter.class) final String version) throws Exception {
        if (needsSetUp()) return;

        // Version always needs to be set first
        if (version != null) {
            if (versions.isValidVersion(version)) {
                serverConfigurator.setVersion(versions.fromString(version));
            } else {
                ShellMessages.error(shell, String.format("Invalid version '%s'. Valid versions: %s", version, versions.getAllVersions()));
            }
        }

        // Set-up the startup configuration
        if (javaHome != null) {
            serverConfigurator.setJavaHome(javaHome);
        }
        if (jbossHome != null) {
            serverConfigurator.setJbossHome(new File(jbossHome));
        }
        // Get the server facet
        project.getFacet(AS7ServerFacet.class).start(serverConfigurator.configure());
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
