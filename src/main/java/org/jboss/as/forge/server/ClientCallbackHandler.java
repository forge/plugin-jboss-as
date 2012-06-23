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
package org.jboss.as.forge.server;

import java.io.IOException;
import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.project.ProjectScoped;

/**
 * A CallbackHandler implementation to supply the username and password if required when connecting to the server - if
 * these are not available the user will be prompted to supply them.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ProjectScoped
public class ClientCallbackHandler implements CallbackHandler {

    @Inject
    private Shell shell;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        // Special case for anonymous authentication to avoid prompting user for their name.
        if (callbacks.length == 1 && callbacks[0] instanceof NameCallback) {
            ((NameCallback) callbacks[0]).setName("anonymous demo user");
            return;
        }

        for (Callback current : callbacks) {
            if (current instanceof RealmCallback) {
                RealmCallback rcb = (RealmCallback) current;
                String defaultText = rcb.getDefaultText();
                rcb.setText(defaultText); // For now just use the realm suggested.

                shell.println(String.format("Authenticating against security realm: %s", defaultText));
            } else if (current instanceof RealmChoiceCallback) {
                throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
            } else if (current instanceof NameCallback) {
                NameCallback ncb = (NameCallback) current;
                String userName = shell.prompt("Username:");

                ncb.setName(userName);
            } else if (current instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback) current;
                char[] password = shell.promptSecret("Password:").toCharArray();

                pcb.setPassword(password);
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }

}
