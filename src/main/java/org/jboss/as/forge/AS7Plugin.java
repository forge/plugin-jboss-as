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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.as.forge.ResultMessage.Level;
import org.jboss.as.forge.server.Server.State;
import org.jboss.as.forge.util.Files;
import org.jboss.as.forge.util.Messages;
import org.jboss.as.forge.util.Streams;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DependencyResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.Wait;
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
@RequiresFacet({AS7MavenPluginFacet.class, AS7ServerFacet.class, DependencyFacet.class})
public class AS7Plugin implements Plugin {
    @Inject
    private Wait wait;

    @Inject
    private Shell shell;

    @Inject
    private Event<InstallFacets> install;

    @Inject
    private Project project;

    @Inject
    private Versions versions;

    @Inject
    private DependencyResolver dependencyResolver;

    private final Messages messages = Messages.INSTANCE;

    @SetupCommand
    @SuppressWarnings("unchecked")
    public void setup() {
        if (!project.hasFacet(AS7ServerFacet.class)) {
            // Make sure facets are installed
            install.fire(new InstallFacets(AS7ServerFacet.class));
        }
        if (promptConfiguration()) {
            // Make sure facets are installed
            install.fire(new InstallFacets(AS7MavenPluginFacet.class));
        }
    }

    @DefaultCommand
    public void checkProperties(final PipeOut out,
                                @Option(name = "version", shortName = "v", flagOnly = true) final boolean version,
                                @Option(name = "jboss-home", flagOnly = true) final boolean jbossHome,
                                @Option(name = "java-home", flagOnly = true) final boolean javaHome,
                                @Option(name = "hostname", flagOnly = true) final boolean hostname,
                                @Option(name = "port", flagOnly = true) final boolean port,
                                @Option(name = "all", shortName = "a", flagOnly = true) final boolean all) {

        final ProjectConfiguration configuration = project.getFacet(AS7ServerFacet.class).getConfiguration();
        if (version || all) out.println(messages.getMessage("version", configuration.getVersion()));
        if (jbossHome || all) out.println(messages.getMessage("home", configuration.getJbossHome()));
        if (javaHome || all) out.println(messages.getMessage("java.home", configuration.getJavaHome()));
        if (hostname || all) out.println(messages.getMessage("hostname", configuration.getHostname()));
        if (port || all) out.println(messages.getMessage("port", configuration.getPort()));
    }

    @Command(value = "install", help = "Downloads, if needed, and installs JBoss Application Server to the specified directory.")
    public void downloadAndInstall(final PipeOut out, @Option(required = true) final Resource<?> jbossHome,
                                   @Option(name = "version", completer = VersionCompleter.class, required = true) final String version) throws Exception {
        boolean ok = true;
        // Create the file
        final File jbossHomeDir = new File(jbossHome.getFullyQualifiedName());

        // Validate the directory
        if (jbossHomeDir.exists() && !jbossHomeDir.isDirectory()) {
            ok = false;
            ShellMessages.error(out, messages.getMessage("home.not.empty.directory", jbossHome.getFullyQualifiedName()));
        }

        // Validate the version
        if (!versions.isValidVersion(version)) {
            ok = false;
            ShellMessages.error(out, messages.getMessage("version.invalid", version, versions.getVersions()));
        }

        // Validation successful, proceed
        if (ok) {
            // Check the home directory
            if (jbossHomeDir.exists() && !Files.isEmptyDirectory(jbossHomeDir)) {
                if (shell.promptBoolean(messages.getMessage("home.not.empty.directory", jbossHomeDir.getAbsolutePath()), false)) {
                    // Delete the contents of the directory
                    Files.deleteRecursively(jbossHomeDir);
                } else {
                    return;
                }
            }
            // Process the download
            if (downloadAndInstall(out, jbossHomeDir, versions.fromString(version))) {
                shell.println(messages.getMessage("download.install.success", version, jbossHomeDir.getAbsolutePath()));
            } else {
                ShellMessages.error(out, messages.getMessage("download.install.failure", version, jbossHome.getFullyQualifiedName()));
            }
        }
    }

    @Command(help = "Overrides the management hostname and/or port to be used when executing commands." +
            "The scope of the override is the lifecycle of the project.",
            value = "override")
    public void overrideConfig(final PipeOut out,
                               @Option(name = "hostname", shortName = "h", help = "The host name for the management interface") final String hostname,
                               @Option(name = "port", shortName = "p", defaultValue = "0", help = "The port for the management interface") final int port,
                               @Option(name = "reset", shortName = "r", flagOnly = true, help = "Resets any overrides to the defaults, runs before any other options") final boolean reset) {
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        final ProjectConfiguration configuration = serverFacet.getConfiguration();
        if (reset) {
            configuration.resetDefaults();
        }

        int p = 0;
        if (validatePort(port)) {
            p = port;
        } else if (port != 0) {
            p = configuration.getPort();
            ShellMessages.error(out, messages.getMessage("port.invalid", p));
        }
        checkResult(out, serverFacet.override(hostname, p), false);
    }

    @Command(help = "Prints the console output, if any, from the server started via the plugin.",
            value = "print-console")
    public void printConsole(@SuppressWarnings("unused") final PipeOut out,
                             @Option(name = "lines", help = "The number of lines to print", defaultValue = "0") final int lines) throws Exception {
        // Get the facet
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        try {
            final List<String> consoleLines = serverFacet.readConsoleOutput(lines);
            if (consoleLines.isEmpty()) {
                ShellMessages.info(out, messages.getMessage("print.console.no-lines"));
            } else {
                for (String line : consoleLines) {
                    out.println(line);
                }
            }
        } catch (IOException e) {
            ShellMessages.error(out, messages.getMessage("print.console.error", e.getLocalizedMessage()));
            final StringWriter stackTrace = new StringWriter();
            final PrintWriter writer = new PrintWriter(stackTrace);
            try {
                e.printStackTrace(writer);
                shell.printlnVerbose(stackTrace.toString());
            } finally {
                Streams.safeClose(stackTrace);
                Streams.safeClose(writer);
            }
        }
    }

    @Command
    public void deploy(final PipeOut out,
                       @Option(name = "force", shortName = "f", defaultValue = "true") final boolean force,
                       @Option(name = "hostname", shortName = "h") final String hostname,
                       @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        // Get the facet
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        // Get the server status
        final State state = serverFacet.getState();
        // The server must be running
        if (state.isRunningState()) {
            final ProjectConfiguration configuration = serverFacet.getConfiguration();
            if (hostname != null) {
                configuration.setHostname(hostname);
            }
            if (validatePort(port)) {
                configuration.setPort(port);
            }
            checkResult(out, serverFacet.deploy(null, force));
        } else {
            ShellMessages.error(out, messages.getMessage("server.not.running", hostname, port));
        }
    }

    @Command
    public void redeploy(final PipeOut out,
                         @Option(name = "hostname", shortName = "h") final String hostname,
                         @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        // Get the facet
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        // Get the server status
        final State state = serverFacet.getState();
        // The server must be running
        if (state.isRunningState()) {
            final ProjectConfiguration configuration = serverFacet.getConfiguration();
            if (hostname != null) {
                configuration.setHostname(hostname);
            }
            if (validatePort(port)) {
                configuration.setPort(port);
            }
            checkResult(out, serverFacet.redeploy(null));
        } else {
            ShellMessages.error(out, messages.getMessage("server.not.running", hostname, port));
        }
    }

    @Command
    public void undeploy(final PipeOut out,
                         @Option(name = "ignore-missing", shortName = "i", defaultValue = "true") final boolean ignoreMissing,
                         @Option(name = "hostname", shortName = "h") final String hostname,
                         @Option(name = "port", shortName = "p", defaultValue = "0") final int port) throws Exception {
        // Get the facet
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        // Get the server status
        final State state = serverFacet.getState();
        // The server must be running
        if (state.isRunningState()) {
            final ProjectConfiguration configuration = serverFacet.getConfiguration();
            if (hostname != null) {
                configuration.setHostname(hostname);
            }
            if (validatePort(port)) {
                configuration.setPort(port);
            }
            checkResult(out, serverFacet.undeploy(null, ignoreMissing));
        } else {
            ShellMessages.error(out, messages.getMessage("server.not.running", hostname, port));
        }
    }

    @Command
    public void start(final PipeOut out,
                      @Option(name = "jboss-home") final Resource<?> jbossHome,
                      @Option(name = "java-home") final String javaHome,
                      @Option(name = "version", completer = VersionCompleter.class) final String version) throws Exception {

        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        final File target;
        if (jbossHome != null) {
            // Create the file
            target = new File(jbossHome.getFullyQualifiedName());
            // Validate the directory
            if (target.exists() && !target.isDirectory()) {
                ShellMessages.error(out, messages.getMessage("home.not.directory", jbossHome
                        .getFullyQualifiedName()));
                return;
            }
        } else {
            final ProjectConfiguration configuration = serverFacet.getConfiguration();
            target = configuration.getJbossHome();
            if (target == null) {
                ShellMessages.error(out, messages.getMessage("start.home.invalid"));
                return;
            }
            if (!target.isDirectory()) {
                ShellMessages.error(out, messages.getMessage("home.not.directory", target.getAbsolutePath()));
                return;
            }
        }

        // Validate the version
        final Version v;
        if (version == null) {
            v = versions.defaultVersion();
        } else if (versions.isValidVersion(version)) {
            v = versions.fromString(version);
        } else {
            ShellMessages.error(out, messages.getMessage("version.invalid", version, versions.getVersions()));
            return;
        }

        // Make sure the server is installed
        if (!serverFacet.isValidJBossHome(target)) {
            // Offer to download if the install does not exist
            if (shell.promptBoolean(messages.getMessage("download.execute", target.getAbsolutePath()), true)) {
                if (!downloadAndInstall(out, target, v)) {
                    ShellMessages.info(out, messages.getMessage("server.start.cancelled", v));
                    return;
                }
            } else {
                ShellMessages.info(out, messages.getMessage("server.start.cancelled", v));
                return;
            }
        }

        wait.start(messages.getMessage("server.starting"));
        final ResultMessage result = serverFacet.start(target, v, javaHome);
        wait.stop();
        if (result.getLevel() == Level.SUCCESS) {
            ShellMessages.success(out, messages.getMessage("server.start.success", v));
        } else {
            checkResult(out, result);
        }
    }

    @Command(help = "Checks the status of the server.")
    public void status(final PipeOut out) throws Exception {
        ShellMessages.info(out, messages.getMessage("server.status", project.getFacet(AS7ServerFacet.class)
                .getState()));
    }

    @Command
    public void shutdown(final PipeOut out) throws Exception {
        checkResult(out, project.getFacet(AS7ServerFacet.class).shutdown(), false);
    }

    @Command("execute-command")
    public void executeCliCommand(final PipeOut out,
                                  @Option(description = "The CLI command to execute.", required = true) final String cmd) throws IOException {
        checkResult(out, project.getFacet(AS7ServerFacet.class).executeCommand(cmd));

    }

    /**
     * Configure the settings
     *
     * @return {@code true} if configured correct, otherwise {@code false}
     */
    private boolean promptConfiguration() {
        final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
        final ProjectConfiguration configuration = serverFacet.getConfiguration();
        // Set the JAVA_HOME, null means use JAVA_HOME
        final String javaHome = shell.prompt(messages.getMessage("prompt.java.home"));
        if (javaHome.isEmpty()) {
            configuration.setJavaHome(null);
        } else {
            configuration.setJavaHome(javaHome);
        }

        // Set the target version
        final Version version = shell.promptChoiceTyped(messages.getMessage("prompt.version"), versions.getVersions(), versions
                .defaultVersion());
        configuration.setVersion(version);

        // Prompt for a path or download
        final String result = shell.prompt(messages.getMessage("prompt.home"));
        if (result.isEmpty()) {
            configuration.setDefaultJbossHomeJbossHome();
            configuration.clearHostname();
            configuration.clearPort();
        } else {
            final File jbossHome = new File(result);
            if (project.getFacet(AS7ServerFacet.class).isValidJBossHome(jbossHome)) {
                configuration.setJbossHome(jbossHome);

                // Check for the hostname override
                final String hostname = shell.prompt(messages.getMessage("prompt.hostname"), ProjectConfiguration.DEFAULT_HOSTNAME);
                if (hostname.isEmpty()) {
                    configuration.clearHostname();
                } else {
                    configuration.setHostname(hostname, true);
                }

                // Check for a port override
                final int port = shell.prompt(messages.getMessage("prompt.port"), Integer.class, ProjectConfiguration.DEFAULT_PORT);
                if (validatePort(port)) {
                    configuration.setPort(port, true);
                } else {
                    ShellMessages.error(shell, messages.getMessage("port.invalid", ProjectConfiguration.DEFAULT_PORT));
                    configuration.clearPort();
                }
            } else {
                // Offer to download if the install does not exist
                if (shell.promptBoolean(messages.getMessage("download.execute", jbossHome.getAbsolutePath()), true)) {
                    if (downloadAndInstall(shell, jbossHome, version)) {
                        configuration.setJbossHome(new File(jbossHome, version.getArchiveDir()));
                    } else {
                        shell.println(ShellColor.RED, messages.getMessage("download.cancelled", version));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void checkResult(final ShellPrintWriter out, final ResultMessage message) {
        checkResult(out, message, true);
    }

    private void checkResult(final ShellPrintWriter out, final ResultMessage message, final boolean checkState) {
        switch (message.getLevel()) {
            case SUCCESS:
                ShellMessages.success(out, message.getMessage());
                break;
            case INFO:
                ShellMessages.info(out, message.getMessage());
                break;
            case WARN:
                ShellMessages.warn(out, message.getMessage());
                break;
            case ERROR:
                ShellMessages.error(out, message.getMessage());
                break;
        }
        if (checkState) {
            final AS7ServerFacet serverFacet = project.getFacet(AS7ServerFacet.class);
            final State state = serverFacet.getState();
            switch (state) {
                case RELOAD_REQUIRED:
                    if (shell.promptBoolean(messages.getMessage("server.prompt.reload"), false)) {
                        try {
                            final ResultMessage m = serverFacet.executeCommand(":reload");
                            if (m.getLevel() == Level.SUCCESS) {
                                ShellMessages.success(out, messages.getMessage("server.reload.success"));
                            } else {
                                checkResult(out, m, false);
                            }
                        } catch (IOException e) {
                            ShellMessages.error(out, messages.getMessage("server.reload.error", e.getLocalizedMessage()));
                        }
                    }
                    break;
                case RESTART_REQUIRED:
                    ShellMessages.info(out, messages.getMessage("server.restart.required"));
                    break;
            }
        }
    }


    private boolean downloadAndInstall(final ShellPrintWriter out, final File target, final Version version) {
        if (shell.promptBoolean(messages.getMessage("download.prompt.continue", version, target))) {
            try {
                final List<DependencyResource> asArchive = dependencyResolver.resolveArtifacts(version.getDependency());
                if (asArchive.isEmpty()) {
                    throw new IllegalStateException(messages.getMessage("download.not.found", version.getDependency()));
                }
                final DependencyResource zipFile = asArchive.get(0);
                // Confirm override on the extraction target only once
                if (target.exists()) {
                    if (!Files.isEmptyDirectory(target)) {
                        if (shell.promptBoolean(messages.getMessage("home.not.empty.directory", target), false)) {
                            Files.deleteRecursively(target);
                        } else {
                            return false;
                        }
                    }
                    if (!target.delete()) {
                        return false;
                    }
                }
                wait.start(messages.getMessage("download.downloading", version));
                return Files.extractAppServer(zipFile.getFullyQualifiedName(), target);
            } catch (IOException e) {
                // Delete the directory
                Files.deleteRecursively(target);
                ShellMessages.error(out, messages.getMessage("download.extraction.error",
                        version.toString(), target.getAbsolutePath(), e.getLocalizedMessage()));
            } finally {
                wait.stop();
            }
        }
        return false;
    }

    private boolean validatePort(final int port) {
        return port > 0 && port <= 65535;
    }
}
