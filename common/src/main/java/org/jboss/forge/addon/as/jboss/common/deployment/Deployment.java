/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.deployment;


/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface Deployment {

    public enum Type {
        DEPLOY,
        FORCE_DEPLOY,
        UNDEPLOY,
        UNDEPLOY_IGNORE_MISSING,
        REDEPLOY,
    }

    public enum Status {
        SUCCESS,
        REQUIRES_RESTART
    }

    /**
     * Executes the deployment
     *
     * @return the status of the execution.
     *
     * @throws DeploymentExecutionException if the deployment fails
     * @throws DeploymentFailureException   if a failure occurs.
     */
    Status execute() throws DeploymentExecutionException, DeploymentFailureException;

    /**
     * The type of the deployment.
     *
     * @return the type of the deployment.
     */
    Type getType();
}
