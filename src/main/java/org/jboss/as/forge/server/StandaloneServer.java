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

package org.jboss.as.forge.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.forge.ServerConfiguration;
import org.jboss.as.forge.server.deployment.Deployment;
import org.jboss.as.forge.server.deployment.standalone.StandaloneDeployment;
import org.jboss.as.forge.util.Files;
import org.jboss.as.forge.util.Streams;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.shell.ShellPrintWriter;

/**
 * A standalone server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class StandaloneServer extends Server {

    private static final String CONFIG_PATH = "/standalone/configuration/";
    private static final ModelNode READ_STATE_OP = Operations.createReadAttributeOperation(Operations.SERVER_STATE);

    private boolean isStarted;
    private ModelControllerClient client;

    /**
     * Creates a new standalone server.
     *
     * @param out the shell to write console output to
     */
    public StandaloneServer(final ShellPrintWriter out) {
        super(out, "JBAS015950");
        isStarted = false;
    }

    @Override
    protected void init(final ServerConfiguration serverConfiguration) throws IOException {
        client = ModelControllerClient.Factory.create(serverConfiguration.getHostname(), serverConfiguration.getPort(), serverConfiguration.getCallbackHandler());
    }

    @Override
    protected void shutdownServer() {
        try {
            if (client != null) {
                try {
                    client.execute(Operations.createOperation(Operations.SHUTDOWN));
                } catch (IOException e) {
                    // no-op
                } finally {
                    Streams.safeClose(client);
                    client = null;
                }
                try {
                    getConsole().awaitShutdown(5L);
                } catch (InterruptedException ignore) {
                    // no-op
                }
            }
        } finally {
            isStarted = false;
        }
    }

    @Override
    public State getState() {
        State result = State.UNKNOWN;
        if (client == null) {
            result = State.SHUTDOWN;
        } else {
            try {
                final ModelNode response = client.execute(READ_STATE_OP);
                if (Operations.successful(response)) {
                    result = State.fromModel(Operations.getResult(response));
                }
            } catch (IOException ignore) {
                result = State.UNKNOWN;
            }
        }
        return result;
    }

    @Override
    public synchronized boolean isStarted() {
        if (isStarted) {
            return isStarted;
        }
        if (client == null) {
            isStarted = false;
        } else {
            try {
                ModelNode rsp = client.execute(READ_STATE_OP);
                if (Operations.successful(rsp)) {
                    final State currentState = State.fromModel(Operations.getResult(rsp));
                    isStarted = currentState == State.RUNNING;
                }
            } catch (Throwable ignore) {
                isStarted = false;
            }
        }
        return isStarted;
    }

    @Override
    public synchronized ModelControllerClient getClient() {
        return client;
    }

    @Override
    protected List<String> createLaunchCommand(final ServerConfiguration serverConfiguration) {
        final File jbossHome = serverConfiguration.getJbossHome();
        final String javaHome = serverConfiguration.getJavaHome();
        final File modulesJar = new File(Files.createPath(jbossHome.getAbsolutePath(), "jboss-modules.jar"));
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);
        String javaExec = Files.createPath(javaHome, "bin", "java");
        if (javaHome.contains(" ")) {
            javaExec = "\"" + javaExec + "\"";
        }

        // Create the commands
        final List<String> cmd = new ArrayList<String>();
        cmd.add(javaExec);
        if (serverConfiguration.getJvmArgs() != null) {
            Collections.addAll(cmd, serverConfiguration.getJvmArgs());
        }

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-Dorg.jboss.boot.log.file=" + jbossHome + "/standalone/log/boot.log");
        cmd.add("-Dlogging.configuration=file:" + jbossHome + CONFIG_PATH + "logging.properties");
        cmd.add("-Djboss.modules.dir=" + serverConfiguration.getModulesDir().getAbsolutePath());
        cmd.add("-Djboss.bundles.dir=" + serverConfiguration.getBundlesDir().getAbsolutePath());
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(serverConfiguration.getModulesDir().getAbsolutePath());
        if (serverConfiguration.getVersion().requiresLogModule()) {
            cmd.add("-logmodule");
            cmd.add("org.jboss.logmanager");
        }
        cmd.add("-jaxpmodule");
        cmd.add("javax.xml.jaxp-provider");
        cmd.add("org.jboss.as.standalone");
        if (serverConfiguration.getServerConfigFile() != null) {
            cmd.add("-server-config");
            cmd.add(serverConfiguration.getServerConfigFile());
        }
        return cmd;
    }

}
