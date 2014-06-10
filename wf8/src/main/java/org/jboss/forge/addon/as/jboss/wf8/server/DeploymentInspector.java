/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * Utility to lookup up Deployments.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DeploymentInspector {

    /**
     * Utility Constructor.
     */
    private DeploymentInspector() {

    }

    /**
     * Lookup an existing Deployment using a static name or a pattern. At least exactComparisonName or deploymentNamePattern
     * must be set.
     *
     * @param client
     * @param exactComparisonName Name for exact matching.
     * @param matchPattern Regex-Pattern for deployment matching.
     * @return the name of the deployment or null.
     */
    public static List<String> getDeployments(ModelControllerClient client, String exactComparisonName, String matchPattern) {

        if (exactComparisonName == null && matchPattern == null) {
            throw new IllegalArgumentException("exactComparisonName and matchPattern are null. One of them must "
                    + "be set in order to find an existing deployment.");
        }

        // CLI :read-children-names(child-type=deployment)
        final ModelNode op = ServerOperations.createListDeploymentsOperation();
        final ModelNode listDeploymentsResult;
        final List<String> result = new ArrayList<String>();
        try {
            listDeploymentsResult = client.execute(op);
            // Check to make sure there is an outcome
            if (Operations.isSuccessfulOutcome(listDeploymentsResult)) {
                if (ServerOperations.isSuccessfulOutcome(listDeploymentsResult)) {
                    final List<ModelNode> deployments = ServerOperations.readResult(listDeploymentsResult).asList();
                    for (ModelNode n : deployments) {

                        if (matches(n.asString(), exactComparisonName, matchPattern)) {
                            result.add(n.asString());
                        }
                    }
                }
            } else {
                throw new IllegalStateException(ServerOperations.getFailureDescriptionAsString(listDeploymentsResult));
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Could not execute operation '%s'", op), e);
        }

        Collections.sort(result);
        return result;

    }

    private static boolean matches(String deploymentName, String exactComparisonName, String matchPattern) {

        if (matchPattern != null) {
            return deploymentName.matches(matchPattern);
        }

        return exactComparisonName.equals(deploymentName);
    }
}
