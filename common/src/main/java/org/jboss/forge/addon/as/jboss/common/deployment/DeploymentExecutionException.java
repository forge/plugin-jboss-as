/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.deployment;


/**
 * Wrapped exception for {@link MojoExecutionException}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("serial")
public class DeploymentExecutionException extends RuntimeException {
    
    public DeploymentExecutionException(final String message, final Exception cause) {
        super(message, cause);
    }

    public DeploymentExecutionException(final Exception cause, final String format, final Object... args) {
        this(String.format(format, args), cause);
    }

    public DeploymentExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DeploymentExecutionException(final Throwable cause, final String format, final Object... args) {
        this(String.format(format, args), cause);
    }

    public DeploymentExecutionException(final String message) {
        super(message);
    }

    public DeploymentExecutionException(final String format, final Object... args) {
        this(String.format(format, args));
    }
}
