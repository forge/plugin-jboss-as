/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8.server;

import java.io.IOException;
import java.util.List;

import org.jboss.forge.addon.ui.result.Result;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.common.server.Server;
import org.jboss.forge.addon.as.jboss.common.server.ServerInfo;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.ui.result.Results;
import org.xnio.IoUtils;

/**
 * A standalone server.
 * 
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Jeremie Lagarde
 */
public final class StandaloneServer extends Server<ModelControllerClient>
{

   private final ServerInfo serverInfo;
   private ModelControllerClient client;

   /**
    * Creates a new standalone server.
    * 
    * @param serverInfo the configuration information for the server
    */
   public StandaloneServer(final ServerInfo serverInfo)
   {
      super(serverInfo, "JBAS015950");
      this.serverInfo = serverInfo;
   }

   @Override
   protected void init() throws IOException
   {
      client = ModelControllerClient.Factory.create(serverInfo.getConnectionInfo().getHostAddress(), serverInfo
               .getConnectionInfo().getPort(), serverInfo.getConnectionInfo().getCallbackHandler());
   }
   
   /**
    * Creates the command to launch the server for the process.
    * 
    * @return the commands used to launch the server
    */
   protected List<String> createLaunchCommand()
   {
      List<String> cmd = super.createLaunchCommand();

      if (serverInfo.getConnectionInfo() != null && serverInfo.getConnectionInfo().getPort() != 0)
      {
         cmd.add("-Djboss.management.http.port="+serverInfo.getConnectionInfo().getPort());
      }
      return cmd;
   }

   @Override
   protected Result shutdown()
   {
      Result result = null;
      try
      {
         final ModelNode response = client.execute(ServerOperations.createOperation(ServerOperations.SHUTDOWN));
         if (ServerOperations.isSuccessfulOutcome(response))
         {
            result = Results.success(Messages.INSTANCE.getMessage("server.shutdown.success"));
         }
         else
         {
            result = Results.fail(ServerOperations.readResultAsString(response));
         }
      }
      catch (IOException e)
      {
         result = Results.fail(e.getLocalizedMessage());
      }
      finally
      {
         IoUtils.safeClose(client);
         client = null;
      }
      try
      {
         if (getConsole() != null)
            getConsole().awaitShutdown(5L);
      }
      catch (InterruptedException ignore)
      {
         // no-op
      }
      return result;
   }

   @Override
   public synchronized ModelControllerClient getClient()
   {
      return client;
   }

   @Override
   protected String getServerState() throws IOException
   {
      final ModelNode result = client.execute(ServerOperations
               .createReadAttributeOperation(ServerOperations.SERVER_STATE));
      if (ServerOperations.isSuccessfulOutcome(result))
      {
         return ServerOperations.readResultAsString(result);
      }
      else
      {
         return null;
      }
   }
}
