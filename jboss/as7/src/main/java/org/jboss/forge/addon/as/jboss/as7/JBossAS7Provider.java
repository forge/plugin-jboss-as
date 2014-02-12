/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.as7.server.Server;
import org.jboss.forge.addon.as.jboss.as7.server.Server.State;
import org.jboss.forge.addon.as.jboss.as7.server.ServerBuilder;
import org.jboss.forge.addon.as.jboss.as7.server.ServerOperations;
import org.jboss.forge.addon.as.jboss.as7.server.deployment.Deployment;
import org.jboss.forge.addon.as.jboss.as7.server.deployment.Deployment.Type;
import org.jboss.forge.addon.as.jboss.as7.server.deployment.DeploymentFailedException;
import org.jboss.forge.addon.as.jboss.as7.server.deployment.standalone.StandaloneDeployment;
import org.jboss.forge.addon.as.jboss.as7.ui.JBossAS7ConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.as.jboss.common.util.Streams;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.PackagingFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Provider extends JBossProvider<JBossAS7Configuration>
{
   @Inject
   private ServerController serverController;

   @Inject
   private JBossAS7Configuration configuration;

   @Inject
   private CallbackHandler callbackHandler;

   @Inject
   private ProjectFactory projectFactory;

   private ServerConsoleWrapper consoleOut;

   private final Messages messages = Messages.INSTANCE;

   @Override
   public String getName()
   {
      return "jbossas7";
   }

   @Override
   public String getDescription()
   {
      return "JBoss AS7";
   }

   @Override
   protected Class<? extends JBossConfigurationWizard> getConfigurationWizardClass()
   {
      return JBossAS7ConfigurationWizard.class;
   }

   @Override
   protected JBossAS7Configuration getConfig()
   {
      return configuration;
   }
   
   @Override
   public Result start(UIContext context)
   {
      Result result = null;
      if ((serverController.hasServer() && serverController.getServer().isRunning()) || getState().isRunningState())
      {
         result = Results.fail(messages.getMessage("server.already.running"));
      }
      else
      {
         try
         {
            // Clean-up possible old console output
            closeConsoleOutput();

            ServerConsoleWrapper consoleOut = new ServerConsoleWrapper();
            final File targetHome = new File(configuration.getPath());
            // final String jreHome = configuration.getJavaHome();
            final String version = configuration.getVersion();
            final Server server = ServerBuilder
                     .of(callbackHandler, targetHome, version.startsWith("7.0"))
                     .setBundlesDir(configuration.getBundlesDir())
                     .setHostAddress(InetAddress.getByName(configuration.getHostname()))
                     .setJavaHome(System.getenv("JAVA_HOME") /* jreHome */)
                     .setJvmArgs(configuration.getJvmArgs())
                     .setModulesDir(new File(targetHome.getAbsolutePath() + "/modules") /* configuration.getModulesDir() */)
                     .setOutputStream(consoleOut)
                     .setPort(configuration.getPort())
                     .setServerConfig(configuration.getServerConfigFile())
                     .build();
            try
            {
               server.start( configuration.getTimeout());

            }
            catch (Throwable e)
            {
               e.printStackTrace();
            }

            if (server.isRunning())
            {
               result = Results.success(messages.getMessage("server.start.success", configuration.getVersion()));

               // Close any previously connected clients
               serverController.closeClient();
               serverController.setServer(server);
            }
            else
            {
               result = Results.fail(messages.getMessage("server.start.failed", configuration.getVersion()));
            }
         }
         catch (Exception e)
         {
            result = Results.fail(messages.getMessage("server.start.failed.exception", configuration.getVersion(),
                     e.getLocalizedMessage()));
         }
         if (result instanceof Failed)
         {
            closeConsoleOutput();
         }

      }
      return result;
   }

   private ModelControllerClient getClient() throws UnknownHostException
   {
      final ModelControllerClient client;
      if (serverController.hasClient())
      {
         client = serverController.getClient();
      }
      else
      {
         client = ModelControllerClient.Factory
                  .create(configuration.getHostname(), configuration.getPort(), callbackHandler);
         serverController.setClient(client);
      }
      return client;
   }

   public State getState()
   {
      State result = State.SHUTDOWN;
      try
      {
         final ModelNode response = getClient().execute(ServerOperations.READ_STATE_OP);
         if (ServerOperations.isSuccessfulOutcome(response))
         {
            result = State.fromModel(ServerOperations.readResult(response));
         }
      }
      catch (IOException ignore)
      {
         result = State.UNKNOWN;
      }
      return result;
   }

   private void closeConsoleOutput()
   {
      Streams.safeFlush(consoleOut);
      Streams.safeClose(consoleOut);
      consoleOut = null;
   }

   @Override
   public Result shutdown(UIContext context)
   {
      Result result = Results.success(messages.getMessage("server.shutdown.success"));
      final Server server = serverController.getServer();
      if (server == null)
      {
         try
         {
            final ModelNode response = getClient().execute(ServerOperations.SHUTDOWN_OP);
            if (ServerOperations.isSuccessfulOutcome(response))
            {
               result = Results.success(ServerOperations.readResultAsString(response));
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
            serverController.closeClient();
         }
      }
      else
      {
         serverController.shutdownServer();
      }
      return result;

   }

   @Override
   public Result deploy(UIContext context) {
      return processDeployment(getSelectedProject(context), (String) context.getAttributeMap().get("path"), Type.DEPLOY);
   }

   @Override
   public Result undeploy(UIContext context) {
      return processDeployment(getSelectedProject(context), (String) context.getAttributeMap().get("path"), Type.UNDEPLOY);
   }
   
   protected Result processDeployment(Project project,String path, Type type)
   {
      final State state = getState();
      Result result;

      // The server must be running
      if (state.isRunningState())
      {

         final PackagingFacet packagingFacet = project.getFacet(PackagingFacet.class);

         // Can't deploy what doesn't exist
         if (!packagingFacet.getFinalArtifact().exists())
            throw new DeploymentFailedException(messages.getMessage("deployment.not.found", path, type));
         final File content;
         if (path == null)
         {
            content = new File(packagingFacet.getFinalArtifact().getFullyQualifiedName());
         }
         else if (path.startsWith("/"))
         {
            content = new File(path);
         }
         else
         {
            // TODO this might not work for EAR deployments
            content = new File(packagingFacet.getFinalArtifact().getParent().getFullyQualifiedName(), path);
         }
         try
         {
            final ModelControllerClient client = getClient();
            final Deployment deployment = StandaloneDeployment.create(client, content, null, type);
            deployment.execute();
            result = Results.success(messages.getMessage("deployment.successful", type));
         }
         catch (Exception e)
         {
            if (e.getCause() != null)
            {
               result = Results.fail(e.getLocalizedMessage() + ": " + e.getCause()
                        .getLocalizedMessage());
            }
            else
            {
               result = Results.fail(e.getLocalizedMessage());
            }
         }
      }
      else
      {
         result = Results.fail(messages.getMessage("server.not.running", configuration.getHostname(),
                  configuration.getPort()));

      }

      return result;

   }

   /**
    * Returns the selected project. null if no project is found
    */
   protected Project getSelectedProject(UIContext context)
   {
      Project project = null;
      UISelection<FileResource<?>> initialSelection = context.getInitialSelection();
      if (!initialSelection.isEmpty())
      {
         project = projectFactory.findProject(initialSelection.get());
      }
      return project;
   }

}
