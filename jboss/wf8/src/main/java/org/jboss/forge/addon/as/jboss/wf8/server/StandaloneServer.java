/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.common.server.Server;
import org.jboss.forge.addon.as.jboss.common.server.ServerInfo;
import org.jboss.forge.addon.as.jboss.common.util.Files;
import org.xnio.IoUtils;

/**
 * A standalone server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class StandaloneServer extends Server<ModelControllerClient> {

    private static final String CONFIG_PATH = "/standalone/configuration/";
    private static final String STARTING = "STARTING";
    private static final String STOPPING = "STOPPING";

    private final ServerInfo serverInfo;
    private boolean isRunning;
    private ModelControllerClient client;

    /**
     * Creates a new standalone server.
     *
     * @param serverInfo the configuration information for the server
     */
    public StandaloneServer(final ServerInfo serverInfo) {
        super(serverInfo, "JBAS015950");
        this.serverInfo = serverInfo;
        isRunning = false;
    }

    @Override
    protected void init() throws IOException {
        client = ModelControllerClient.Factory.create(serverInfo.getConnectionInfo().getHostAddress(), serverInfo.getConnectionInfo().getPort(), serverInfo.getConnectionInfo().getCallbackHandler());
    }

    @Override
    protected void stopServer() {
        try {
            if (client != null) {
                try {
                    client.execute(ServerOperations.createOperation(ServerOperations.SHUTDOWN));
                } catch (IOException e) {
                    // no-op
                } finally {
                    IoUtils.safeClose(client);
                    client = null;
                }
                try {
                    getConsole().awaitShutdown(5L);
                } catch (InterruptedException ignore) {
                    // no-op
                }
            }
        } finally {
            isRunning = false;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        if (isRunning) {
            return isRunning;
        }
        checkServerState();
        return isRunning;
    }

    @Override
    public synchronized ModelControllerClient getClient() {
        return client;
    }

    @Override
    protected List<String> createLaunchCommand() {
        final File jbossHome = serverInfo.getJbossHome();
        final String javaHome = serverInfo.getJavaHome();
        final File modulesJar = new File(Files.createPath(jbossHome.getAbsolutePath(), "jboss-modules.jar"));
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);
        String javaExec = (javaHome == null ? "java" : Files.createPath(javaHome, "bin", "java"));
        if (javaExec.contains(" ")) {
            javaExec = "\"" + javaExec + "\"";
        }

        // Create the commands
        final List<String> cmd = new ArrayList<String>();
        cmd.add(javaExec);
        if (serverInfo.getJvmArgs() != null) {
            Collections.addAll(cmd, serverInfo.getJvmArgs());
        }

        cmd.add("-Dorg.jboss.boot.log.file=" + jbossHome + "/standalone/log/server.log");
        cmd.add("-Dlogging.configuration=file:" + jbossHome + CONFIG_PATH + "logging.properties");
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(serverInfo.getModulesDir().getAbsolutePath());
        cmd.add("org.jboss.as.standalone");
        cmd.add("-Djboss.home.dir=" + jbossHome);
        if (serverInfo.getServerConfig() != null) {
            cmd.add("-server-config");
            cmd.add(serverInfo.getServerConfig());
        }
        if (serverInfo.getPropertiesFile() != null) {
            cmd.add("-P");
            cmd.add(serverInfo.getPropertiesFile());
        }
        return cmd;
    }

    @Override
    public void checkServerState() {
        if (client == null) {
            isRunning = false;
        } else {
            try {
                final ModelNode result = client.execute(ServerOperations.createReadAttributeOperation(ServerOperations.SERVER_STATE));
                isRunning = ServerOperations.isSuccessfulOutcome(result) && !STARTING.equals(ServerOperations.readResultAsString(result)) &&
                        !STOPPING.equals(ServerOperations.readResultAsString(result));
            } catch (Throwable ignore) {
                isRunning = false;
            }
        }
    }

}
