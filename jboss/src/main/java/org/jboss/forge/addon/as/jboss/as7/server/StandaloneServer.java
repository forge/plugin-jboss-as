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

package org.jboss.forge.addon.as.jboss.as7.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.as7.util.Streams;
import org.jboss.forge.addon.as.jboss.common.util.Files;

/**
 * A standalone server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class StandaloneServer extends Server {

    private static final String CONFIG_PATH = "/standalone/configuration/";
    private static final String STARTING = "STARTING";
    private static final String STOPPING = "STOPPING";

    private final File jbossHome;
    private final CallbackHandler callbackHandler;
    private final File modulesDir;
    private final File bundlesDir;
    private final InetAddress hostAddress;
    private final String[] jvmArgs;
    private final String javaHome;
    private final int port;
    private final String serverConfig;
    private final boolean requiresLogModule;
    private boolean isRunning;
    private ModelControllerClient client;

    StandaloneServer(final OutputStream out, final File jbossHome, final CallbackHandler callbackHandler, final File modulesDir,
                     final File bundlesDir, final InetAddress hostAddress, final String[] jvmArgs,
                     final String javaHome, final int port, final String serverConfig, final boolean requiresLogModule) {
        super(out);
        this.jbossHome = jbossHome;
        this.callbackHandler = callbackHandler;
        this.modulesDir = modulesDir;
        this.bundlesDir = bundlesDir;
        this.hostAddress = hostAddress;
        this.jvmArgs = jvmArgs;
        this.javaHome = javaHome;
        this.port = port;
        this.serverConfig = serverConfig;
        this.requiresLogModule = requiresLogModule;
        isRunning = false;
    }

    @Override
    protected void init() throws IOException {
        client = ModelControllerClient.Factory.create(hostAddress, port, callbackHandler);
    }

    @Override
    protected void stopServer() {
        try {
            if (client != null) {
                try {
                    client.execute(ServerOperations.SHUTDOWN_OP);
                } catch (IOException e) {
                    // no-op
                } finally {
                    Streams.safeClose(client);
                    client = null;
                }
            }
        } finally {
            isRunning = false;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    @Override
    public synchronized ModelControllerClient getClient() {
        return client;
    }

    @Override
    protected List<String> createLaunchCommand() {
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
        if (jvmArgs != null) {
            Collections.addAll(cmd, jvmArgs);
        }

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-Dorg.jboss.boot.log.file=" + jbossHome + "/standalone/log/boot.log");
        cmd.add("-Dlogging.configuration=file:" + jbossHome + CONFIG_PATH + "logging.properties");
        cmd.add("-Djboss.modules.dir=" + modulesDir.getAbsolutePath());
        cmd.add("-Djboss.bundles.dir=" + bundlesDir.getAbsolutePath());
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(modulesDir.getAbsolutePath());
        if (requiresLogModule) {
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
        }
        cmd.add("-jaxpmodule");
        cmd.add("javax.xml.jaxp-provider");
        cmd.add("org.jboss.as.standalone");
        if (serverConfig != null) {
            cmd.add("-server-config");
            cmd.add(serverConfig);
        }
        return cmd;
    }

    @Override
    protected long checkServerState() {
        if (client == null) {
            isRunning = false;
            return 0;
        } else {
            final long start = System.currentTimeMillis();
            try {
                final ModelNode result = client.execute(ServerOperations.READ_STATE_OP);
                isRunning = ServerOperations.isSuccessfulOutcome(result) && !STARTING.equals(ServerOperations.readResultAsString(result)) &&
                        !STOPPING.equals(ServerOperations.readResultAsString(result));
            } catch (Throwable ignore) {
                isRunning = false;
            }
            final long end = System.currentTimeMillis();
            return (end - start);
        }
    }

}
