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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.forge.util.Messages;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class Server {

    public static enum State {
        UNKNOWN("unknown"),
        STARTING("starting"),
        RUNNING("running", true),
        RELOAD_REQUIRED("reload-required", true),
        RESTART_REQUIRED("restart-required", true),
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
        private final boolean isRunningState;

        private State(final String stringForm) {
            this(stringForm, false);
        }

        private State(final String stringForm, final boolean isRunningState) {
            this.stringForm = stringForm;
            this.isRunningState = isRunningState;
        }

        public static State fromString(final String state) {
            if (MAP.containsKey(state)) {
                return MAP.get(state);
            }
            return UNKNOWN;
        }

        public static State fromModel(final ModelNode state) {
            return fromString(state.asString());
        }

        /**
         * Indicates whether the state is a running state.
         *
         * @return {@code true} if this is a running state, otherwise {@code false}
         */
        public boolean isRunningState() {
            return isRunningState;
        }

        @Override
        public String toString() {
            return stringForm;
        }

    }

    private Process process;
    private final OutputStream out;

    protected final Messages messages = Messages.INSTANCE;

    protected Server(final OutputStream out) {
        this.out = out;
    }

    /**
     * Starts the server.
     *
     * @throws java.io.IOException the an error occurs creating the process
     */
    public synchronized final void start(final long timeout) throws IOException {
        SecurityActions.registerShutdown(this);
        final List<String> cmd = createLaunchCommand();
        final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
        startConsoleConsumer(process.getInputStream());
        long startTimeout = timeout * 1000;
        boolean serverAvailable = false;
        long sleep = 50;
        init();
        while (startTimeout > 0 && !serverAvailable) {
            serverAvailable = isRunning();
            if (!serverAvailable) {
                if (processHasDied(process))
                    break;
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    serverAvailable = false;
                    break;
                }
                startTimeout -= sleep + checkServerState();
                sleep = Math.max(sleep / 2, 100);
            }
        }
        if (!serverAvailable) {
            destroyProcess();
            throw new IllegalStateException(messages.getMessage("server.not.started", timeout));
        }
    }

    /**
     * Invokes any optional initialization that should take place after the process has been launched. Note the server
     * may not be completely started when the method is invoked.
     *
     * @throws java.io.IOException if an IO error occurs
     */
    protected abstract void init() throws IOException;

    /**
     * Stops the server before the process is destroyed. A no-op override will just destroy the process.
     */
    protected abstract void stopServer();

    /**
     * Checks the status of the server and returns {@code true} if the server is fully started.
     *
     * @return {@code true} if the server is fully started, otherwise {@code false}
     */
    public abstract boolean isRunning();

    /**
     * Returns the client that used to execute management operations on the server.
     *
     * @return the client to execute management operations
     */
    public abstract ModelControllerClient getClient();

    /**
     * Creates the command to launch the server for the process.
     *
     * @return the commands used to launch the server
     */
    protected abstract List<String> createLaunchCommand();

    /**
     * Checks whether the server is running or not. If the server is no longer running the {@link #isRunning()} should
     * return {@code false}.
     *
     * @return the approximate time (in milliseconds) the server waited for the client to connect, 0 if {@link
     *         #isRunning()} is {@code true}
     */
    protected abstract long checkServerState();

    /**
     * Stops the server.
     */
    public final synchronized void stop() {
        try {
            stopServer();
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

    private ConsoleConsumer startConsoleConsumer(final InputStream stream) {
        final ConsoleConsumer result = new ConsoleConsumer(stream);
        final Thread t = new Thread(result);
        t.setName("AS7-Console");
        t.setDaemon(true);
        t.start();
        return result;
    }

    /**
     * Runnable that consumes the output of the process.
     *
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    /**
     * Runnable that consumes the output of the process.
     *
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    class ConsoleConsumer implements Runnable {

        private final InputStream in;

        protected ConsoleConsumer(final InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {

            try {
                byte[] buf = new byte[512];
                int num;
                while ((num = in.read(buf)) != -1) {
                    if (out != null) out.write(buf, 0, num);
                }
            } catch (IOException ignore) {
            }
        }

    }
}
