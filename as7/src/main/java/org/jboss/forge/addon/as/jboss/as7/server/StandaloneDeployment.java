/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7.server;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult;
import org.jboss.forge.addon.as.jboss.common.deployment.Deployment;
import org.jboss.forge.addon.as.jboss.common.deployment.DeploymentExecutionException;
import org.jboss.forge.addon.as.jboss.common.deployment.DeploymentFailureException;
import org.jboss.forge.addon.as.jboss.common.deployment.MatchPatternStrategy;

/**
 * A deployment for standalone servers.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StandaloneDeployment implements Deployment {

    private final File content;
    private final ModelControllerClient client;
    private final String name;
    private final Type type;
    private final String matchPattern;
    private final MatchPatternStrategy matchPatternStrategy;

    /**
     * Creates a new deployment.
     *
     * @param client               the client that is connected.
     * @param content              the content for the deployment.
     * @param name                 the name of the deployment, if {@code null} the name of the content file is used.
     * @param type                 the deployment type.
     * @param matchPattern         the pattern for matching multiple artifacts, if {@code null} the name is used.
     * @param matchPatternStrategy the strategy for handling multiple artifacts.
     */
    public StandaloneDeployment(final ModelControllerClient client, final File content, final String name, final Type type,
                                final String matchPattern, final MatchPatternStrategy matchPatternStrategy) {
        this.content = content;
        this.client = client;
        this.name = (name == null ? content.getName() : name);
        this.type = type;
        this.matchPattern = matchPattern;
        this.matchPatternStrategy = matchPatternStrategy;
    }

    /**
     * Creates a new deployment.
     *
     * @param client               the client that is connected.
     * @param content              the content for the deployment.
     * @param name                 the name of the deployment, if {@code null} the name of the content file is used.
     * @param type                 the deployment type.
     * @param matchPattern         the pattern for matching multiple artifacts, if {@code null} the name is used.
     * @param matchPatternStrategy the strategy for handling multiple artifacts.
     *
     * @return the new deployment
     */
    public static StandaloneDeployment create(final ModelControllerClient client, final File content, final String name, final Type type,
                                              final String matchPattern, final MatchPatternStrategy matchPatternStrategy) {
        return new StandaloneDeployment(client, content, name, type, matchPattern, matchPatternStrategy);
    }

    private DeploymentPlan createPlan(final DeploymentPlanBuilder builder) throws IOException, DeploymentFailureException {
        DeploymentPlanBuilder planBuilder = builder;

        List<String> existingDeployments = DeploymentInspector.getDeployments(client, name, matchPattern);

        switch (type) {
            case DEPLOY: {
                planBuilder = builder.add(name, content).andDeploy();
                break;
            }
            case REDEPLOY: {
                planBuilder = builder.replace(name, content).redeploy(name);
                break;
            }
            case UNDEPLOY: {
                validateExistingDeployments(existingDeployments);
                planBuilder = undeployAndRemove(builder, existingDeployments);
                break;
            }
            case FORCE_DEPLOY: {
                if (existingDeployments.contains(name)) {
                    planBuilder = builder.replace(name, content).redeploy(name);
                } else {
                    planBuilder = builder.add(name, content).andDeploy();
                }
                break;
            }
            case UNDEPLOY_IGNORE_MISSING: {
                validateExistingDeployments(existingDeployments);
                if (!existingDeployments.isEmpty()) {
                    planBuilder = undeployAndRemove(builder, existingDeployments);
                } else {
                    return null;
                }
                break;
            }
        }
        return planBuilder.build();
    }

    private DeploymentPlanBuilder undeployAndRemove(final DeploymentPlanBuilder builder, final List<String> deploymentNames) {

        DeploymentPlanBuilder planBuilder = builder;

        for (String deploymentName : deploymentNames) {
            planBuilder = planBuilder.undeploy(deploymentName).andRemoveUndeployed();

            if (matchPatternStrategy == MatchPatternStrategy.FIRST) {
                break;
            }
        }

        return planBuilder;
    }

    private void validateExistingDeployments(List<String> existingDeployments) throws DeploymentFailureException {
        if (matchPattern == null) {
            return;
        }

        if (matchPatternStrategy == MatchPatternStrategy.FAIL && existingDeployments.size() > 1) {
            throw new DeploymentFailureException(String.format("Deployment failed, found %d deployed artifacts for pattern '%s' (%s)",
                    existingDeployments.size(), matchPattern, existingDeployments));
        }
    }

    @Override
    public Status execute() throws DeploymentExecutionException, DeploymentFailureException {
        Status resultStatus = Status.SUCCESS;
        try {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(client);
            final DeploymentPlanBuilder builder = manager.newDeploymentPlan();
            final DeploymentPlan plan = createPlan(builder);
            if (plan != null) {
                if (plan.getDeploymentActions().size() > 0) {
                    final ServerDeploymentPlanResult planResult = manager.execute(plan).get();
                    // Check the results
                    for (DeploymentAction action : plan.getDeploymentActions()) {
                        final ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(action.getId());
                        final ServerUpdateActionResult.Result result = actionResult.getResult();
                        switch (result) {
                            case FAILED:
                                throw new DeploymentExecutionException("Deployment failed.", actionResult.getDeploymentException());
                            case NOT_EXECUTED:
                                throw new DeploymentExecutionException("Deployment not executed.", actionResult.getDeploymentException());
                            case ROLLED_BACK:
                                throw new DeploymentExecutionException("Deployment failed and was rolled back.", actionResult.getDeploymentException());
                            case CONFIGURATION_MODIFIED_REQUIRES_RESTART:
                                resultStatus = Status.REQUIRES_RESTART;
                                break;
                        }
                    }
                }
            }
        } catch (DeploymentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentExecutionException(e, "Error executing %s", type);
        }
        return resultStatus;
    }

    @Override
    public Type getType() {
        return type;
    }

}
