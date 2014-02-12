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

package org.jboss.forge.addon.as.jboss.as7.server;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.common.util.Messages;

/**
 * A helper for creating operations.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ServerOperations extends Operations {

    public static final String READ_CHILDREN_NAMES = "read-children-names";
    public static final String RELOAD = "reload";

    public static final ModelNode READ_STATE_OP = ServerOperations.createReadAttributeOperation("server-state");

    public static final ModelNode  SHUTDOWN_OP = ServerOperations.createOperation("shutdown");

    /**
     * Parses the result and returns the failure description.
     *
     * @param result the result of executing an operation
     *
     * @return the failure description if defined, otherwise a new undefined model node
     *
     * @throws IllegalArgumentException if the outcome of the operation was successful
     */
    public static String getFailureDescriptionAsString(final ModelNode result) {
        if (isSuccessfulOutcome(result)) {
            return "";
        }
        final String msg;
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            if (result.hasDefined(OP)) {
                msg = Messages.INSTANCE.getMessage("op.failure.address", result.get(OP), result.get(OP_ADDR), result
                        .get(FAILURE_DESCRIPTION));
            } else {
                msg = Messages.INSTANCE.getMessage("op.failure", result.get(FAILURE_DESCRIPTION));
            }
        } else {
            msg = Messages.INSTANCE.getMessage("op.failure.unknown.result", result);
        }
        return msg;
    }

    /**
     * Creates an operation to read the attribute represented by the {@code attributeName} parameter.
     *
     * @param attributeName the name of the parameter to read
     *
     * @return the operation
     */
    public static ModelNode createReadAttributeOperation(final String attributeName) {
        final ModelNode op = createOperation(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        return op;
    }

    /**
     * Creates an operation to list the deployments.
     *
     * @return the operation
     */
    public static ModelNode createListDeploymentsOperation() {
        final ModelNode op = createOperation(READ_CHILDREN_NAMES);
        op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
        return op;
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
        return (result.hasDefined(ClientConstants.RESULT) ? result.get(ClientConstants.RESULT).asString() : "");
    }
}
