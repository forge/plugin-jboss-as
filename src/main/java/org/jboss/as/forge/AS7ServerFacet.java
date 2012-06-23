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
import java.io.IOException;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.as.forge.server.Server;
import org.jboss.as.forge.server.Server.State;
import org.jboss.as.forge.server.StandaloneServer;
import org.jboss.forge.env.Configuration;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.resources.Resource;
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
    private Configuration configuration;

    @Inject
    private ServerConfiguration serverConfiguration;

    @Override
    public boolean install() {
        return configure();
    }

    @Override
    public boolean isInstalled() {
        return serverConfiguration.isConfigured();
    }

    protected boolean configure() {
        final Server server = getServer(project);
        if (server != null && server.isStarted()) {
            return false;
        }
        return serverConfiguration.configure(getProjectTarget());
    }

    public boolean isStarted() {
        final Server server = getServer(project);
        return server != null && server.isStarted();
    }

    public void executeCommand(final String cmd) throws IOException {
        final Server server = getServer(project);
        if (server == null) {
            ShellMessages.error(shell, "The server is not running.");
        } else {
            server.executeCliCommand(cmd);
        }
    }

    public void start(final ServerConfiguration serverConfiguration) throws IOException {
        // Make sure the server is installed
        if (!serverConfiguration.isServerInstalled()) {
            // If the JBoss Home is the projects target directory, just download and install
            if (serverConfiguration.getJbossHome().getParentFile().equals(getProjectTarget())) {
                serverConfiguration.downloadAndInstall();
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
        final Server server = getServer(project);
        if (server != null) {
            server.shutdown();
            ShellMessages.info(shell, String.format("JBoss AS %s has successfully shutdown.", serverConfiguration.getVersion()));
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
}
