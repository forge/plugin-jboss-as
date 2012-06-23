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

import org.jboss.forge.project.dependencies.Dependency;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Version implements Comparable<Version> {

    private final Dependency dependency;
    private final boolean requiresLogModule;
    private final String version;
    private final String archiveDir;

    public Version(final Dependency dependency, final String archiveDir, final boolean requiresLogModule) {
        this.dependency = dependency;
        this.requiresLogModule = requiresLogModule;
        this.version = dependency.getVersion();
        this.archiveDir = archiveDir;
    }

    public static Version of(final Dependency dependency, final String archiveDir, final boolean requiresLogModule) {
        return new Version(dependency, archiveDir, requiresLogModule);
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getArchiveDir() {
        return archiveDir;
    }

    public boolean requiresLogModule() {
        return requiresLogModule;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = 13;
        hash = prime * hash + (dependency.getVersion() == null ? 0 : dependency.getVersion().hashCode());
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;

        if (!(obj instanceof Version)) {
            return false;
        }
        final Version other = (Version) obj;
        return (dependency == null ? other.dependency == null :
                (dependency.getVersion() == null ? other.getDependency().getVersion() == null :
                        dependency.getVersion().equals(other.getDependency().getVersion())));
    }

    @Override
    public String toString() {
        return dependency.getVersion();
    }

    @Override
    public int compareTo(final Version o) {
        // This should work as it should sort by Alpha, Beta, CR then Final
        return version.compareTo(o.version);
    }

}
