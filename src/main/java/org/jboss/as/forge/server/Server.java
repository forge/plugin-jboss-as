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
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.forge.ServerConfiguration;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.shell.ShellPrintWriter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class Server {

    public static enum State {
        UNKNOWN("unknown"),
        STARTING("starting"),
        RUNNING("running"),
        RELOAD_REQUIRED("reload-required"),
        RESTART_REQUIRED("restart-required"),
        STOPPING("stopping"),
        SHUTDOWN("shutdown");

        private static final Map<String, State> MAP;

        static {
            MAP = new HashMap<String, State>();
            for (State state : values()) {
                MAP.put(state.stringForm, state);
            }
        }

        private final String stringForm;

        private State(final String stringForm) {
            this.stringForm = stringForm;
        }

        public static State fromString(final String state) {
            if (MAP.containsKey(state)) {
                return MAP.get(state);
            }
            return UNKNOWN;
        }

        static State fromModel(final ModelNode state) {
            return fromString(state.asString());
        }

        @Override
        public String toString() {
            return stringForm;
        }

    }


    private Process process;
    private ConsoleConsumer console;
    private CommandContext commandContext;
    private final ShellPrintWriter out;
    private final String shutdownId;

    protected Server(final ShellPrintWriter out) {
        this.out = out;
        shutdownId = null;
    }

    protected Server(final ShellPrintWriter out, final String shutdownId) {
        this.out = out;
        this.shutdownId = shutdownId;
    }

    /**
     * The console that is associated with the server.
     *
     * @return the console
     */
    protected final ConsoleConsumer getConsole() {
        return console;
    }

    /**
     * Starts the server.
     *
     * @throws java.io.IOException the an error occurs creating the process
     */
    public synchronized final void start(final ServerConfiguration serverConfiguration) throws IOException {
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }));
        final List<String> cmd = createLaunchCommand(serverConfiguration);
        final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
        console = createConsole(process.getInputStream());
        long timeout = serverConfiguration.getStartupTimeout() * 1000;
        boolean serverAvailable = false;
        long sleep = 50;
        init(serverConfiguration);
        while (timeout > 0 && !serverAvailable) {
            serverAvailable = isStarted();
            if (!serverAvailable) {
                if (processHasDied(process))
                    break;
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    serverAvailable = false;
                    break;
                }
                timeout -= sleep;
                sleep = Math.max(sleep / 2, 100);
            }
        }
        if (!serverAvailable) {
            destroyProcess();
            throw new IllegalStateException(String.format("Managed server was not started within [%d] s", serverConfiguration.getStartupTimeout()));
        }
    }

    /**
     * Stops the server.
     */
    public synchronized final void shutdown() {
        try {
            shutdownServer();
            try {
                if (commandContext != null) {
                    commandContext.terminateSession();
                }
            } catch (Exception ignore) {
                // no-op
            }
        } finally {
            if (process != null) {
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException ignore) {
                    // no-op
                }
            }
        }
    }

    /**
     * Invokes any optional initialization that should take place after the process has been launched. Note the server
     * may not be completely started when the method is invoked.
     *
     * @param serverConfiguration the server configuration
     *
     * @throws java.io.IOException if an IO error occurs
     */
    protected abstract void init(final ServerConfiguration serverConfiguration) throws IOException;

    /**
     * Stops the server before the process is destroyed. A no-op override will just destroy the process.
     */
    protected abstract void shutdownServer();

    /**
     * Checks the state of the server.
     *
     * @return the state of the server
     */
    public abstract State getState();

    /**
     * Checks the status of the server and returns {@code true} if the server is fully started.
     *
     * @return {@code true} if the server is fully started, otherwise {@code false}
     */
    public abstract boolean isStarted();

    /**
     * Returns the client that used to execute management operations on the server.
     *
     * @return the client to execute management operations
     */
    public abstract ModelControllerClient getClient();

    /**
     * Creates the command to launch the server for the process.
     *
     * @param serverConfiguration the server configuration
     *
     * @return the commands used to launch the server
     */
    protected abstract List<String> createLaunchCommand(final ServerConfiguration serverConfiguration);

    /**
     * Execute a CLI command.
     *
     * @param cmd the command to execute
     *
     * @throws java.io.IOException      if an error occurs on the connected client
     * @throws IllegalArgumentException if the command is invalid or was unsuccessful
     */
    public synchronized void executeCliCommand(final String cmd) throws IOException {
        if (!isStarted()) {
            throw new IllegalStateException("Cannot execute commands on a server that is not running.");
        }
        CommandContext ctx = commandContext;
        if (ctx == null) {
            commandContext = ctx = createCommandContext();
        }
        final ModelControllerClient client = getClient();
        final ModelNode op;
        final ModelNode result;
        try {
            op = ctx.buildRequest(cmd);
            result = client.execute(op);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException(String.format("Command '%s' is invalid", cmd), e);
        }
        if (!Operations.successful(result)) {
            throw new IllegalArgumentException(String.format("Command '%s' was unsuccessful. Reason: %s", cmd, Operations.getFailureDescription(result)));
        }
        out.println(result.toString());
    }


    private int destroyProcess() {
        if (process == null)
            return 0;
        process.destroy();
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean processHasDied(final Process process) {
        try {
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // good
            return false;
        }
    }

    /**
     * Creates the command context and binds the client to the context.
     * <p/>
     * If the client is {@code null}, no client is bound to the context.
     *
     * @return the command line context
     *
     * @throws IllegalStateException if the context fails to initialize
     */
    // TODO clean this up, add shutdown hook and don't keep creating a new context
    public static CommandContext createCommandContext() {
        final CommandContext commandContext;
        try {
            commandContext = CommandContextFactory.getInstance().newCommandContext();
        } catch (CliInitializationException e) {
            throw new IllegalStateException("Failed to initialize CLI context", e);
        }
        return commandContext;
    }

    /**
     * Creates the console.
     *
     * @param stream the stream to consume
     *
     * @return the console
     */
    private ConsoleConsumer createConsole(final InputStream stream) {
        final ConsoleConsumer result = new ConsoleConsumer(stream);
        final Thread t = new Thread(result);
        t.setName("AS7-Console");
        t.start();
        return result;
    }

    /**
     * Runnable that consumes the output of the process.
     *
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    final class ConsoleConsumer implements Runnable {

        private final InputStream in;
        private final CountDownLatch latch;

        private ConsoleConsumer(final InputStream in) {
            this.in = in;
            latch = new CountDownLatch(1);
        }

        @Override
        public void run() {

            try {
                byte[] buf = new byte[512];
                int num;
                while ((num = in.read(buf)) != -1) {
                    // Swallow console output
                    out.write(buf, 0, num);
                    if (shutdownId != null && new String(buf).contains(shutdownId))
                        latch.countDown();
                }
            } catch (IOException ignore) {
            }
        }

        void awaitShutdown(final long seconds) throws InterruptedException {
            if (shutdownId == null) latch.countDown();
            latch.await(seconds, TimeUnit.SECONDS);
        }

    }
}
