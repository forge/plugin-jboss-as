/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.LinkedList;
import javax.inject.Inject;

import org.jboss.as.forge.util.Messages;
import org.jboss.forge.maven.MavenPluginFacet;
import org.jboss.forge.maven.plugins.MavenPlugin;
import org.jboss.forge.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * A simple facet that only adds the maven plugin to the pom.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequiresFacet({DependencyFacet.class, MavenPluginFacet.class, ResourceFacet.class})
public class AS7MavenPluginFacet extends BaseFacet {

    private final Messages messages = Messages.INSTANCE;

    @Inject
    private Shell shell;

    private final DependencyBuilder pluginDependency = DependencyBuilder.create("org.jboss.as.plugins:jboss-as-maven-plugin");

    @Override
    public boolean install() {
        final DependencyBuilder dependency = DependencyBuilder.create(pluginDependency);
        final LinkedList<Dependency> versions = new LinkedList<Dependency>(project.getFacet(DependencyFacet.class).resolveAvailableVersions(dependency));
        final MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

        // Get the version
        final Dependency selectedDependency = shell.promptChoiceTyped(messages.getMessage("prompt.maven.plugin.version"), versions, versions.getLast());

        if (!pluginFacet.hasPlugin(pluginDependency)) {
            // TODO (jrp) consider EAR or multi-module set-up
            final MavenPlugin plugin = MavenPluginBuilder.create()
                    .setDependency(DependencyBuilder.create(pluginDependency).setVersion(selectedDependency
                            .getVersion()));
            pluginFacet.addPlugin(plugin);
        }
        return project.getFacet(MavenPluginFacet.class).hasPlugin(pluginDependency);
    }

    @Override
    public boolean isInstalled() {
        return project.getFacet(MavenPluginFacet.class).hasPlugin(pluginDependency);
    }

    @Override
    public boolean uninstall() {
        project.getFacet(MavenPluginFacet.class).removePlugin(pluginDependency);
        return true;
    }
}
