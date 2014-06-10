/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.deployment;


/**
 * Wrapped exception for the {@link MojoFailureException}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("serial")
public class DeploymentFailureException extends RuntimeException {
   
    public DeploymentFailureException(final String message) {
        super(message);
    }

    public DeploymentFailureException(final String format, final Object... args) {
        this(String.format(format, args));
    }

    public DeploymentFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DeploymentFailureException(final Throwable cause, final String format, final Object... args) {
        this(String.format(format, args), cause);
    }
}
