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
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.forge.ResultMessage.Level;
import org.jboss.as.forge.server.Server;
import org.jboss.as.forge.server.Server.State;
import org.jboss.as.forge.server.ServerBuilder;
import org.jboss.as.forge.server.ServerOperations;
import org.jboss.as.forge.server.deployment.Deployment;
import org.jboss.as.forge.server.deployment.Deployment.Type;
import org.jboss.as.forge.server.deployment.DeploymentFailedException;
import org.jboss.as.forge.server.deployment.standalone.StandaloneDeployment;
import org.jboss.as.forge.util.Messages;
import org.jboss.as.forge.util.Streams;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.shell.events.PreShutdown;
import org.jboss.forge.shell.events.ProjectChanged;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequiresFacet(PackagingFacet.class)
class AS7ServerFacet extends BaseFacet {
    static final String PROJECT_KEY = "installed";

    @Inject
    private ProjectConfiguration configuration;

    @Inject
    private CallbackHandler callbackHandler;

    private final Messages messages = Messages.INSTANCE;

    @Inject
    private ServerController serverController;

    private ServerConsoleWrapper consoleOut;

    @Override
    public boolean install() {
        configuration.setProperty(PROJECT_KEY, "true");
        return configuration.hasProperty(PROJECT_KEY);
    }

    @Override
    public boolean uninstall() {
        configuration.clearConfig();
        return true;
    }

    @Override
    public boolean isInstalled() {
        return configuration.hasProperty(PROJECT_KEY);
    }

    ProjectConfiguration getConfiguration() {
        return configuration;
    }

    public ResultMessage override(final String hostname, final int port) {
        if (hostname != null) {
            configuration.setHostname(hostname);
        }
        if (port > 0) {
            configuration.setPort(port);
        }
        ResultMessage result = ResultMessage.of(Level.SUCCESS, messages.getMessage("override.success", configuration.getHostname(), configuration
                .getPort()));
        // Check for a running server
        if (serverController.hasServer()) {
            result = ResultMessage.of(Level.SUCCESS, messages.getMessage("override.success.running.server", configuration
                    .getHostname(), configuration.getPort()));
        } else if (serverController.hasClient()) {
            // Close the connection if there is one
            serverController.closeClient();
        }
        return result;
    }

    public List<String> readConsoleOutput(final int lines) throws IOException {
        return consoleOut == null ? Collections.<String>emptyList() :
                (lines > 0 ? consoleOut.readLines(lines) : consoleOut.readAllLines());
    }

    public ResultMessage deploy(final String path, final boolean force) throws IOException, DeploymentFailedException {
        return processDeployment(path, (force ? Type.FORCE_DEPLOY : Type.DEPLOY));
    }

    public ResultMessage redeploy(final String path) throws IOException, DeploymentFailedException {
        return processDeployment(path, Type.REDEPLOY);
    }

    public ResultMessage undeploy(final String path, final boolean ignoreMissing) throws IOException, DeploymentFailedException {
        return processDeployment(path, (ignoreMissing ? Type.UNDEPLOY_IGNORE_MISSING : Type.UNDEPLOY));
    }

    public ResultMessage executeCommand(final String cmd) throws IOException {
        if (!getState().isRunningState()) {
            return ResultMessage.of(Level.ERROR, messages.getMessage("server.not.running", configuration.getHostname(), configuration
                    .getPort()));
        }
        ResultMessage result;
        try {
            final ModelControllerClient client = getClient();
            final CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
            final ModelNode op = ctx.buildRequest(cmd);
            final ModelNode outcome = client.execute(op);
            if (ServerOperations.isSuccessfulOutcome(outcome)) {
                result = ResultMessage.of(Level.SUCCESS, outcome.toString());
            } else {
                result = ResultMessage.of(Level.ERROR, ServerOperations.getFailureDescriptionAsString(outcome));
            }
        } catch (CliInitializationException e) {
            result = ResultMessage.of(Level.ERROR, messages.getMessage("cmd.context.create.failure", e.getLocalizedMessage()));
        } catch (CommandFormatException e) {
            result = ResultMessage.of(Level.ERROR, messages.getMessage("cmd.invalid", cmd, e.getLocalizedMessage()));
        }
        return result;
    }

    public ResultMessage start(final File jbossHome, final Version version, final String javaHome) throws IOException {
        ResultMessage result;
        if ((serverController.hasServer() && serverController.getServer().isRunning()) || getState().isRunningState()) {
            result = ResultMessage.of(Level.ERROR, messages.getMessage("server.already.running"));
        } else {
            // Clean-up possible old console output
            closeConsoleOutput();
            consoleOut = new ServerConsoleWrapper();
            final File targetHome = jbossHome == null ? configuration.getJbossHome() : jbossHome;
            final String jreHome = javaHome == null ? configuration.getJavaHome() : javaHome;
            final Server server = ServerBuilder.of(callbackHandler, targetHome, version.requiresLogModule())
                    .setBundlesDir(configuration.getBundlesDir())
                    .setHostAddress(InetAddress.getByName(configuration.getHostname()))
                    .setJavaHome(jreHome)
                    .setJvmArgs(configuration.getJvmArgs())
                    .setModulesDir(configuration.getModulesDir())
                    .setOutputStream(consoleOut)
                    .setPort(configuration.getPort())
                    .setServerConfig(configuration.getServerConfigFile())
                    .build();
            server.start(configuration.getStartupTimeout());
            try {
                if (server.isRunning()) {
                    result = ResultMessage.of(Level.SUCCESS, messages.getMessage("server.start.success", configuration
                            .getVersion()));
                    // Close any previously connected clients
                    serverController.closeClient();
                    serverController.setServer(server);
                } else {
                    result = ResultMessage.of(Level.ERROR, messages.getMessage("server.start.failed", configuration.getVersion()));
                }
            } catch (Exception e) {
                result = ResultMessage.of(Level.ERROR, messages.getMessage("server.start.failed.exception", configuration
                        .getVersion(), e.getLocalizedMessage()));
            }
            if (result.getLevel() == Level.ERROR) {
                closeConsoleOutput();
            }
        }
        return result;
    }

    public State getState() {
        State result = State.SHUTDOWN;
        try {
            final ModelNode response = getClient().execute(ServerOperations.READ_STATE_OP);
            if (ServerOperations.isSuccessfulOutcome(response)) {
                result = State.fromModel(ServerOperations.readResult(response));
            }
        } catch (IOException ignore) {
            result = State.UNKNOWN;
        }
        return result;
    }

    public ResultMessage shutdown() {
        ResultMessage result = ResultMessage.of(Level.SUCCESS, messages.getMessage("server.shutdown.success"));
        final Server server = serverController.getServer();
        if (server == null) {
            try {
                final ModelNode response = getClient().execute(ServerOperations.SHUTDOWN_OP);
                if (ServerOperations.isSuccessfulOutcome(response)) {
                    result = ResultMessage.of(Level.SUCCESS, ServerOperations.readResultAsString(response));
                } else {
                    result = ResultMessage.of(Level.ERROR, ServerOperations.readResultAsString(response));
                }
            } catch (IOException e) {
                result = ResultMessage.of(Level.ERROR, e.getLocalizedMessage());
            } finally {
                serverController.closeClient();
            }
        } else {
            serverController.shutdownServer();
        }
        return result;
    }

    protected void closeClient(@Observes final ProjectChanged event) {
        serverController.closeClient();
    }

    protected void shutdownServer(@Observes final PreShutdown event) {
        serverController.shutdownServer();
        serverController.closeClient();
        closeConsoleOutput();
    }

    protected boolean isValidJBossHome(final File jbossHome) {
        if (jbossHome != null && jbossHome.exists() && jbossHome.isDirectory()) {
            // Search for jboss-modules.jar
            final File[] files = jbossHome.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    return "jboss-modules.jar".equals(pathname.getName());
                }
            });
            return files != null && files.length > 0;
        }
        return false;
    }

    private ResultMessage processDeployment(final String path, final Type type) throws IOException, DeploymentFailedException {
        final PackagingFacet packagingFacet = project.getFacet(PackagingFacet.class);
        ResultMessage result;
        // Can't deploy what doesn't exist
        if (!packagingFacet.getFinalArtifact().exists())
            throw new DeploymentFailedException(messages.getMessage("deployment.not.found", path, type));
        final File content;
        if (path == null) {
            content = new File(packagingFacet.getFinalArtifact().getFullyQualifiedName());
        } else if (path.startsWith("/")) {
            content = new File(path);
        } else {
            // TODO this might not work for EAR deployments
            content = new File(packagingFacet.getFinalArtifact().getParent().getFullyQualifiedName(), path);
        }
        final ModelControllerClient client = getClient();
        try {
            final Deployment deployment = StandaloneDeployment.create(client, content, null, type);
            deployment.execute();
            result = ResultMessage.of(Level.SUCCESS, messages.getMessage("deployment.successful", type));
        } catch (DeploymentFailedException e) {
            if (e.getCause() != null) {
                result = ResultMessage.of(Level.ERROR, e.getLocalizedMessage() + ": " + e.getCause()
                        .getLocalizedMessage());
            } else {
                result = ResultMessage.of(Level.ERROR, e.getLocalizedMessage());
            }
        }
        return result;
    }

    private ModelControllerClient getClient() throws UnknownHostException {
        final ModelControllerClient client;
        if (serverController.hasClient()) {
            client = serverController.getClient();
        } else {
            client = ModelControllerClient.Factory
                    .create(configuration.getHostname(), configuration.getPort(), callbackHandler);
            serverController.setClient(client);
        }
        return client;
    }

    private void closeConsoleOutput() {
        Streams.safeFlush(consoleOut);
        Streams.safeClose(consoleOut);
        consoleOut = null;
    }
}
