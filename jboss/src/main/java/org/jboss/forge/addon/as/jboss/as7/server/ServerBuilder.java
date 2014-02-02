/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.io.OutputStream;
import java.net.InetAddress;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.forge.addon.as.jboss.common.util.Files;


/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerBuilder {

    private final File jbossHome;
    private final CallbackHandler callbackHandler;
    private final boolean requiresLogModule;
    private OutputStream out;
    private File modulesDir;
    private File bundlesDir;
    private InetAddress hostAddress;
    private String[] jvmArgs;
    private String javaHome;
    private int port;
    private String serverConfig;

    private ServerBuilder(final CallbackHandler callbackHandler, final File jbossHome, final boolean requiresLogModule) {
        this.callbackHandler = callbackHandler;
        this.jbossHome = jbossHome;
        this.requiresLogModule = requiresLogModule;
    }

    public static ServerBuilder of(final CallbackHandler callbackHandler, final File jbossHome, final boolean requiresLogModule) {
        return new ServerBuilder(callbackHandler, jbossHome, requiresLogModule);
    }

    public Server build() {
        final File modulesDir = (this.modulesDir == null ? Files.createFile(jbossHome, "modules") : new File(this.modulesDir
                .getAbsolutePath()));
        final File bundlesDir = (this.bundlesDir == null ? Files.createFile(jbossHome, "bundles") : new File(this.modulesDir
                .getAbsolutePath()));
        final String[] jvmArgs;
        if (this.jvmArgs != null) {
            jvmArgs = new String[this.jvmArgs.length];
            System.arraycopy(this.jvmArgs, 0, jvmArgs, 0, jvmArgs.length);
        } else {
            jvmArgs = null;
        }
        return new StandaloneServer(out, jbossHome, callbackHandler, modulesDir, bundlesDir, hostAddress, jvmArgs, javaHome, port, serverConfig, requiresLogModule);
    }

    public ServerBuilder setModulesDir(final File modulesDir) {
        this.modulesDir = modulesDir;
        return this;
    }

    public ServerBuilder setBundlesDir(final File bundlesDir) {
        this.bundlesDir = bundlesDir;
        return this;
    }

    public ServerBuilder setHostAddress(final InetAddress hostAddress) {
        this.hostAddress = hostAddress;
        return this;
    }

    public ServerBuilder setJvmArgs(final String[] jvmArgs) {
        this.jvmArgs = jvmArgs;
        return this;
    }

    public ServerBuilder setJavaHome(final String javaHome) {
        this.javaHome = javaHome;
        return this;
    }

    public ServerBuilder setOutputStream(final OutputStream out) {
        this.out = out;
        return this;
    }

    public ServerBuilder setPort(final int port) {
        this.port = port;
        return this;
    }

    public ServerBuilder setServerConfig(final String serverConfig) {
        this.serverConfig = serverConfig;
        return this;
    }
}
