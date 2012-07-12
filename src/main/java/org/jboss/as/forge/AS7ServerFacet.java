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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.forge.server.Operations;
import org.jboss.as.forge.server.Server;
import org.jboss.as.forge.server.Server.State;
import org.jboss.as.forge.server.StandaloneServer;
import org.jboss.as.forge.server.deployment.Deployment;
import org.jboss.as.forge.server.deployment.Deployment.Type;
import org.jboss.as.forge.server.deployment.DeploymentFailedException;
import org.jboss.as.forge.server.deployment.standalone.StandaloneDeployment;
import org.jboss.as.forge.util.FilePermissions;
import org.jboss.as.forge.util.Files;
import org.jboss.as.forge.util.Streams;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.resources.DependencyResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PreShutdown;
import org.jboss.forge.shell.events.ProjectChanged;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequiresFacet(PackagingFacet.class)
class AS7ServerFacet extends BaseFacet {
    @Inject
    private Shell shell;

    @Inject
    private DependencyResolver dependencyResolver;

    @Inject
    private Versions versions;

    @Inject
    private ProjectServerConfigurator serverConfigurator;

    @Override
    public boolean install() {
        return configure();
    }

    @Override
    public boolean isInstalled() {
        return serverConfigurator.hasConfiguration();
    }

    public void deploy(final String path, final ServerConfiguration serverConfiguration, final boolean force) throws IOException, DeploymentFailedException {
        processDeployment(path, serverConfiguration, (force ? Type.FORCE_DEPLOY : Type.DEPLOY));
    }

    public void redeploy(final String path, final ServerConfiguration serverConfiguration) throws IOException, DeploymentFailedException {
        processDeployment(path, serverConfiguration, Type.REDEPLOY);
    }

    public void undeploy(final String path, final ServerConfiguration serverConfiguration, final boolean ignoreMissing) throws IOException, DeploymentFailedException {
        processDeployment(path, serverConfiguration, (ignoreMissing ? Type.UNDEPLOY_IGNORE_MISSING : Type.UNDEPLOY));
    }

    protected boolean configure() {
        final Server server = getServer(project);
        if (server != null && server.isStarted()) {
            return false;
        }
        return promptConfiguration();
    }

    public boolean isStarted() {
        final Server server = getServer(project);
        return server != null && server.isStarted();
    }

    public void executeCommand(final String cmd) throws IOException {
        // TODO this should also work for remote servers not just locally started servers
        final Server server = getServer(project);
        if (server == null) {
            ShellMessages.error(shell, "The server is not running.");
        } else {
            server.executeCliCommand(cmd);
        }
    }

    public void start(final ServerConfiguration serverConfiguration) throws IOException {
        // Make sure the server is installed
        if (!serverConfiguration.getJbossHome().exists()) {
            // If the JBoss Home is the projects target directory, just download and install
            if (serverConfiguration.getJbossHome().getParentFile().equals(getProjectTarget())) {
                downloadAndInstall(serverConfiguration.getJbossHome().getParentFile(), serverConfiguration.getVersion());
            } else {
                ShellMessages.error(shell, String.format("JBoss AS %s could not be started due to an invalid or missing install at '%s'.",
                        serverConfiguration.getVersion(), serverConfiguration.getJbossHome()));
                return;
            }
        }
        Server server = getServer(project);
        if (server == null) {
            server = new StandaloneServer(shell);
            setServer(project, server); // TODO (jrp) see if there is a better way to store this
        }
        if (server.isStarted()) {
            ShellMessages.error(shell, "The server is already running.");
        } else {
            server.start(serverConfiguration);
            if (server.isStarted()) {
                ShellMessages.info(shell, String.format("JBoss AS %s has successfully started.", serverConfiguration.getVersion()));
            } else {
                ShellMessages.info(shell, String.format("JBoss AS %s has failed to start. Status: %s", serverConfiguration.getVersion(), server.getState()));
            }
        }
    }

    public void status() {
        // TODO this should also work for remote servers not just locally started servers
        final Server server = getServer(project);
        final State state;
        if (server != null) {
            state = server.getState();
        } else {
            state = State.SHUTDOWN;
        }
        ShellMessages.info(shell, String.format("Server Status: %s", state));
    }

    public void shutdown() {
        // TODO this should also work for remote servers not just locally started servers
        final Server server = getServer(project);
        if (server != null) {
            server.shutdown();
            ShellMessages.info(shell, "JBoss AS has successfully shutdown.");
        } else {
            ShellMessages.error(shell, "The server is has not been created.");
        }
    }

    protected void shutdown(@Observes final ProjectChanged event) {
        shutdown(event.getOldProject());
    }

    protected void shutdown(@Observes final PreShutdown event) {
        shutdown(project);
    }

    private void shutdown(final Project project) {
        if (project != null) {
            final Server server = getServer(project);
            if (server != null) {
                if (shell != null && server.isStarted()) {
                    ShellMessages.info(shell, "An event occurred that requires the server be shutdown.");
                }
                server.shutdown();
            }
        }
    }

    private void processDeployment(final String path, final ServerConfiguration serverConfiguration, final Type type) throws IOException, DeploymentFailedException {
        final PackagingFacet packagingFacet = project.getFacet(PackagingFacet.class);
        // Can't deploy what doesn't exist
        if (!packagingFacet.getFinalArtifact().exists())
            throw DeploymentFailedException.of("Could not deploy '%s' as it does not exist. Please build before attempting to deploy.", path);
        final File content;
        if (path == null) {
            content = new File(packagingFacet.getFinalArtifact().getFullyQualifiedName());
        } else if (path.startsWith("/")) {
            content = new File(path);
        } else {
            // TODO this might not work for EAR deployments
            content = new File(packagingFacet.getFinalArtifact().getParent().getFullyQualifiedName(), path);
        }
        final Server server = getServer(project);
        final ModelControllerClient client;
        final boolean doClose;
        // Use the servers client if it's started
        if (server != null && server.isStarted()) {
            client = server.getClient();
            doClose = false;
        } else {
            client = ModelControllerClient.Factory.create(serverConfiguration.getHostname(), serverConfiguration.getPort(), serverConfiguration.getCallbackHandler());
            doClose = true;
        }
        try {
            final Deployment deployment = StandaloneDeployment.create(client, content, null, type);
            switch (deployment.execute()) {
                case REQUIRES_RESTART: {
                    if (shell.promptBoolean(String.format("The deployment operation (%s) requires the server be restarted. Would you like to reload now?", type), false)) {
                        client.execute(Operations.createOperation(Operations.RELOAD));
                    } else {
                        shell.println(String.format("The deployment operation (%s) was successful, but the server needs to be restarted.", type));
                    }
                    break;
                }
                case SUCCESS: {
                    shell.println(String.format("The deployment operation (%s) was successful.", type));
                }
            }
        } finally {
            if (doClose) Streams.safeClose(client);
        }
    }

    private File getProjectTarget() {
        final PackagingFacet packaging = project.getFacet(PackagingFacet.class);
        return new File(packaging.getFinalArtifact().getParent().getFullyQualifiedName(), "jboss-as-dist");
    }

    private static Server getServer(final Project project) {
        return (Server) project.getAttribute("server");
    }

    private static void setServer(final Project project, final Server server) {
        project.setAttribute("server", server);
    }

    /**
     * Configure the settings
     *
     * @return {@code true} if configured correct, otherwise {@code false}
     */
    private boolean promptConfiguration() {
        final ServerConfiguration defaultConfig = serverConfigurator.defaultConfiguration();

        // Prompt for Java Home
        boolean doPrompt = true;
        if (defaultConfig.getJavaHome() != null) {
            doPrompt = shell.promptBoolean(String.format("The Java Home '%s' is already set, would you like to override it?", defaultConfig.getJavaHome()), false);
        }
        if (doPrompt) {
            final String javaHome = shell.prompt("Enter the Java home directory or leave blank to use the JAVA_HOME environment variable:");
            if (javaHome.isEmpty()) {
                serverConfigurator.setJavaHome(null);
            } else {
                serverConfigurator.setJavaHome(javaHome);
            }
        }

        // Prompt the user for the version
        doPrompt = true;
        if (defaultConfig.getVersion() != null) {
            doPrompt = shell.promptBoolean(String.format("A default version of %s is already set, would you like to override it?",
                    defaultConfig.getVersion()), false);
        }
        // TODO fix the version and download
        final Version version;
        if (doPrompt) {
            version = shell.promptChoiceTyped("Choose default JBoss AS version:", versions.getVersions());
        } else {
            version = versions.defaultVersion();
        }
        serverConfigurator.setVersion(version);

        // Prompt for a path or download
        final String result = shell.prompt("Enter path for JBoss AS or leave blank to download:");
        final File jbossHome;
        if (result.isEmpty()) {
            jbossHome = downloadAndInstall(serverConfigurator.getProjectTarget(), version);
            serverConfigurator.setJbossHome(null);
        } else {
            jbossHome = new File(result);
            serverConfigurator.setJbossHome(jbossHome);
        }

        // Should never be null, but let's be careful
        if (jbossHome == null) {
            ShellMessages.error(shell, "The JBoss Home was found to be null, something is broken.");
            return false;
        }
        serverConfigurator.writeConfiguration();
        return true;
    }

    // TODO version *may* not be necessary
    protected File downloadAndInstall(final File baseDir, final Version version) {
        if (shell.promptBoolean(String.format("You are about to download JBoss AS %s to '%s' which could take a while. Would you like to continue?", version, baseDir))) {
            final List<DependencyResource> asArchive = dependencyResolver.resolveArtifacts(version.getDependency());
            if (asArchive.isEmpty()) {
                throw new IllegalStateException(String.format("Could not find artifact: %s", version.getDependency()));
            }
            final DependencyResource zipFile = asArchive.get(0);
            return extract(zipFile, baseDir);
        }
        return null;
    }

    /**
     * Extracts the data from the downloaded zip file.
     *
     * @param zipFile the zip file to extract
     * @param target  the directory the data should be extracted to
     */
    private File extract(final DependencyResource zipFile, final File target) {
        File result = target;
        final byte buff[] = new byte[1024];
        ZipFile file = null;
        try {
            file = new ZipFile(zipFile.getFullyQualifiedName());
            final Enumeration<ZipArchiveEntry> entries = file.getEntries();
            boolean firstEntry = true;
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry entry = entries.nextElement();
                // Create the extraction target
                final File extractTarget = new File(target, entry.getName());
                // First entry should be a the base directory
                if (firstEntry) {
                    firstEntry = false;
                    // Confirm override on the extraction target only once
                    if (extractTarget.exists()) {
                        if (shell.promptBoolean(String.format("The target (%s) already exists, would you like to replace the directory?", extractTarget), true)) {
                            Files.deleteRecursively(extractTarget);
                        } else {
                            result = null;
                            break;
                        }
                    }
                    result = extractTarget;
                }
                if (entry.isDirectory()) {
                    extractTarget.mkdirs();
                } else {
                    final File parent = new File(extractTarget.getParent());
                    parent.mkdirs();
                    final BufferedInputStream in = new BufferedInputStream(file.getInputStream(entry));
                    try {
                        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(extractTarget));
                        try {
                            int read;
                            while ((read = in.read(buff)) != -1) {
                                out.write(buff, 0, read);
                            }
                        } finally {
                            Streams.safeClose(out);
                        }
                    } finally {
                        Streams.safeClose(in);
                    }
                    // Set the file permissions
                    if (entry.getUnixMode() > 0) {
                        setPermissions(extractTarget, FilePermissions.of(entry.getUnixMode()));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Error extracting '%s'", (file == null ? "null file" : file)), e);
        } finally {
            ZipFile.closeQuietly(file);
            // Streams.safeClose(file);
        }
        return result;
    }

    private static void setPermissions(final File file, final FilePermissions permissions) {
        file.setExecutable(permissions.owner().canExecute(), !(permissions.group().canExecute() && permissions.pub().canExecute()));
        file.setReadable(permissions.owner().canWrite(), !(permissions.group().canWrite() && permissions.pub().canWrite()));
        file.setWritable(permissions.owner().canWrite(), !(permissions.group().canWrite() && permissions.pub().canWrite()));
    }
}
