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
package org.jboss.forge.addon.as.jboss.as7;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.forge.addon.as.jboss.common.util.Messages;


/**
 * A CallbackHandler implementation to supply the username and password if required when connecting to the server - if
 * these are not available the user will be prompted to supply them.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ClientCallbackHandler implements CallbackHandler {

    private String username;
    private char[] password;

    private final Messages messages = Messages.INSTANCE;

   
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

//                shell.println(messages.getMessage("prompt.security.realm", defaultText));
            } else if (current instanceof RealmChoiceCallback) {
                throw new UnsupportedCallbackException(current, messages.getMessage("security.realm.unsupported"));
            } else if (current instanceof NameCallback) {
                NameCallback ncb = (NameCallback) current;
                // Use a secret prompt as a normal prompt blocks the input on a lock,
                // see org.jboss.forge.shell.ShellImpl#promptWithCompleter(String message, final Completer tempCompleter)
                // for more details.
//                if (username == null)
//                    username = shell.promptSecret(messages.getMessage("prompt.username"));

                ncb.setName(username);
            } else if (current instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback) current;
//                if (password == null)
//                    password = shell.promptSecret(messages.getMessage("prompt.password")).toCharArray();

                pcb.setPassword(password);
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }

}
