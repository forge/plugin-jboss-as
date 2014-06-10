/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.as.jboss.common.server;

import java.net.InetAddress;
import javax.security.auth.callback.CallbackHandler;

/**
 * Holds information on how to connect to the Application Server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ConnectionInfo {
    /**
     * The port number of the server to deploy to. The default is 9999.
     *
     * @return the port number to deploy to.
     */
    int getPort();

    /**
     * Creates gets the address to the host name.
     *
     * @return the address.
     */
    InetAddress getHostAddress();

    /**
     * The callback handler for authentication.
     *
     * @return the callback handler.
     */
    CallbackHandler getCallbackHandler();
}
