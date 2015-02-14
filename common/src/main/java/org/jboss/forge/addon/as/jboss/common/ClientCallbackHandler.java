/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common;

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
