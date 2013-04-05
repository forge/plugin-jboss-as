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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.jboss.as.forge.util.Messages;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.shell.project.ProjectScoped;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ProjectScoped
public class Versions {
    private static final Dependency BASE = DependencyBuilder.create().setGroupId("org.jboss.as").
            setArtifactId("jboss-as-dist").setPackagingType("zip");

    private static final Dependency[] VERSIONS = {
            DependencyBuilder.create(BASE).setVersion("7.0.0.Final"),
            DependencyBuilder.create(BASE).setVersion("7.0.1.Final"),
            DependencyBuilder.create(BASE).setVersion("7.0.2.Final"),
            DependencyBuilder.create(BASE).setVersion("7.1.0.Final"),
            DependencyBuilder.create(BASE).setVersion("7.1.1.Final"),
    };

    private final TreeMap<String, Version> versions;
    private final Version defaultVersion;

    private final Messages messages = Messages.INSTANCE;

    public Versions() {
        versions = new TreeMap<String, Version>();
        for (Dependency dependency : VERSIONS) {
            final String stringVersion = dependency.getVersion();
            final String archiveDir = String.format("jboss-as-%s", stringVersion);
            final Version version = Version.of(dependency, archiveDir, stringVersion.startsWith("7.0"));
            versions.put(version.toString(), version);
        }
        defaultVersion = versions.lastEntry().getValue();
    }

    public boolean isValidVersion(final String version) {
        return versions.containsKey(version);
    }

    public Version fromString(final String version) {
        if (versions.containsKey(version)) {
            return versions.get(version);
        }
        throw new IllegalArgumentException(messages.getMessage("version.invalid", version, versions.values()));
    }

    public Version defaultVersion() {
        return defaultVersion;
    }

    public List<Version> getVersions() {
        return new ArrayList<Version>(versions.values());
    }
}
