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

package org.jboss.as.forge;

import java.util.Queue;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.handlers.ArchiveHandler;
import org.jboss.as.cli.handlers.CommandCommandHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.DeploymentInfoHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.ReadAttributeHandler;
import org.jboss.as.cli.handlers.ReadOperationHandler;
import org.jboss.as.cli.handlers.UndeployHandler;
import org.jboss.as.cli.handlers.batch.BatchClearHandler;
import org.jboss.as.cli.handlers.batch.BatchDiscardHandler;
import org.jboss.as.cli.handlers.batch.BatchEditLineHandler;
import org.jboss.as.cli.handlers.batch.BatchHandler;
import org.jboss.as.cli.handlers.batch.BatchHoldbackHandler;
import org.jboss.as.cli.handlers.batch.BatchListHandler;
import org.jboss.as.cli.handlers.batch.BatchMoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRemoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRunHandler;
import org.jboss.as.forge.server.Server;
import org.jboss.forge.shell.completer.CommandCompleter;
import org.jboss.forge.shell.completer.CommandCompleterState;

/**
 * This is just a base CLI command completer that tries to wrap the JBoss AS CLI Command Completer. So far it doesn't
 * work at all since it also needs a terminal. This may work by executing :read-operations on the current directory,
 * which could be difficult to determine. Further investigation is required.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class CliCommandCompleter implements CommandCompleter {
    private final org.jboss.as.cli.CommandCompleter completer;
    private final CommandContext context = Server.createCommandContext();

    public CliCommandCompleter() {
        final CommandRegistry cmdRegistry = new CommandRegistry();
        cmdRegistry.registerHandler(new CommandCommandHandler(cmdRegistry), "command");
        cmdRegistry.registerHandler(new LsHandler(), "ls");
        cmdRegistry.registerHandler(new ReadAttributeHandler(context), "read-attribute");
        cmdRegistry.registerHandler(new ReadOperationHandler(context), "read-operation");

        // deployment
        cmdRegistry.registerHandler(new DeployHandler(context), "deploy");
        cmdRegistry.registerHandler(new UndeployHandler(context), "undeploy");
        cmdRegistry.registerHandler(new DeploymentInfoHandler(context), "deployment-info");

        // batch commands
        cmdRegistry.registerHandler(new BatchHandler(context), "batch");
        cmdRegistry.registerHandler(new BatchDiscardHandler(), "discard-batch");
        cmdRegistry.registerHandler(new BatchListHandler(), "list-batch");
        cmdRegistry.registerHandler(new BatchHoldbackHandler(), "holdback-batch");
        cmdRegistry.registerHandler(new BatchRunHandler(context), "run-batch");
        cmdRegistry.registerHandler(new BatchClearHandler(), "clear-batch");
        cmdRegistry.registerHandler(new BatchRemoveLineHandler(), "remove-batch-line");
        cmdRegistry.registerHandler(new BatchMoveLineHandler(), "move-batch-line");
        cmdRegistry.registerHandler(new BatchEditLineHandler(), "edit-batch-line");
        completer = new org.jboss.as.cli.CommandCompleter(cmdRegistry);
        SecurityActions.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                context.terminateSession();
            }
        }));
    }

    @Override
    public void complete(final CommandCompleterState state) {
        StringBuilder buffer = new StringBuilder();
        int count = 0;
        for (String token : state.getOriginalTokens()) {
            if (++count > 2)
                buffer = buffer.append(token);
        }
        completer.complete(context, buffer.toString(), state.getIndex(), state.getCandidates());
    }
}
