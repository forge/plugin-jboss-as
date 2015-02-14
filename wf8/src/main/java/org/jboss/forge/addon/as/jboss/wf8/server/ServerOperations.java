/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8.server;

import static org.jboss.as.controller.client.helpers.ClientConstants.CHILD_TYPE;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT;
import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;

import java.util.List;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A helper for creating operations.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerOperations extends Operations {

    public static final String ENABLE = "enable";
    public static final String LAUNCH_TYPE = "launch-type";
    public static final String PROFILE = "profile";
    public static final String READ_CHILDREN_NAMES = "read-children-names";
    public static final String READ_RESOURCE = "read-resource";
    public static final String RELOAD = "reload";
    public static final String SERVER_STATE = "server-state";
    public static final String SHUTDOWN = "shutdown";

    /**
     * Parses the result and returns the failure description. If the result was successful, an empty string is
     * returned.
     *
     * @param result the result of executing an operation
     *
     * @return the failure message or an empty string
     */
    public static String getFailureDescriptionAsString(final ModelNode result) {
        if (isSuccessfulOutcome(result)) {
            return "";
        }
        final String msg;
        if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            if (result.hasDefined(ClientConstants.OP)) {
                msg = String.format("Operation '%s' at address '%s' failed: %s", result.get(ClientConstants.OP), result.get(ClientConstants.OP_ADDR), result
                        .get(ClientConstants.FAILURE_DESCRIPTION));
            } else {
                msg = String.format("Operation failed: %s", result.get(ClientConstants.FAILURE_DESCRIPTION));
            }
        } else {
            msg = String.format("An unexpected response was found checking the deployment. Result: %s", result);
        }
        return msg;
    }

    /**
     * Creates an operation to list the deployments.
     *
     * @return the operation
     */
    public static ModelNode createListDeploymentsOperation() {
        final ModelNode op = createOperation(READ_CHILDREN_NAMES);
        op.get(CHILD_TYPE).set(DEPLOYMENT);
        return op;
    }

    /**
     * Creates a remove operation.
     *
     * @param address   the address for the operation
     * @param recursive {@code true} if the remove should be recursive, otherwise {@code false}
     *
     * @return the operation
     */
    public static ModelNode createRemoveOperation(final ModelNode address, final boolean recursive) {
        final ModelNode op = createRemoveOperation(address);
        op.get(RECURSIVE).set(recursive);
        return op;
    }

    /**
     * Creates an operation to read the attribute represented by the {@code attributeName} parameter.
     *
     * @param attributeName the name of the parameter to read
     *
     * @return the operation
     */
    public static ModelNode createReadAttributeOperation(final String attributeName) {
        return createReadAttributeOperation(new ModelNode().setEmptyList(), attributeName);
    }

    public static ModelNode createAddress(final String key, final String name) {
        final ModelNode address = new ModelNode().setEmptyList();
        address.add(key, name);
        return address;
    }

    /**
     * Creates an operation.
     *
     * @param operation the operation name
     * @param address   the address for the operation
     * @param recursive whether the operation is recursive or not
     *
     * @return the operation
     *
     * @throws IllegalArgumentException if the address is not of type {@link ModelType#LIST}
     */
    public static ModelNode createOperation(final String operation, final ModelNode address, final boolean recursive) {
        final ModelNode op = createOperation(operation, address);
        op.get(RECURSIVE).set(recursive);
        return op;
    }

    /**
     * Finds the last entry of the address list and returns it as a property.
     *
     * @param address the address to get the last part of
     *
     * @return the last part of the address
     *
     * @throws IllegalArgumentException if the address is not of type {@link ModelType#LIST} or is empty
     */
    public static Property getChildAddress(final ModelNode address) {
        if (address.getType() != ModelType.LIST) {
            throw new IllegalArgumentException("The address type must be a list.");
        }
        final List<Property> addressParts = address.asPropertyList();
        if (addressParts.isEmpty()) {
            throw new IllegalArgumentException("The address is empty.");
        }
        return addressParts.get(addressParts.size() - 1);
    }

    /**
     * Finds the parent address, everything before the last address part.
     *
     * @param address the address to get the parent
     *
     * @return the parent address
     *
     * @throws IllegalArgumentException if the address is not of type {@link ModelType#LIST} or is empty
     */
    public static ModelNode getParentAddress(final ModelNode address) {
        if (address.getType() != ModelType.LIST) {
            throw new IllegalArgumentException("The address type must be a list.");
        }
        final ModelNode result = new ModelNode();
        final List<Property> addressParts = address.asPropertyList();
        if (addressParts.isEmpty()) {
            throw new IllegalArgumentException("The address is empty.");
        }
        for (int i = 0; i < addressParts.size() - 1; ++i) {
            final Property property = addressParts.get(i);
            result.add(property.getName(), property.getValue());
        }
        return result;
    }

    /**
     * Reads the result of an operation and returns the result as a string. If the operation does not have a {@link
     * ClientConstants#RESULT} attribute and empty string is returned.
     *
     * @param result the result of executing an operation
     *
     * @return the result of the operation or an empty string
     */
    public static String readResultAsString(final ModelNode result) {
        return (result.hasDefined(RESULT) ? result.get(RESULT).asString() : "");
    }
}
