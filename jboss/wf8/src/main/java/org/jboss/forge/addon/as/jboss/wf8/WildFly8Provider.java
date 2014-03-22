/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.deployment.Deployment;
import org.jboss.forge.addon.as.jboss.common.deployment.Deployment.Type;
import org.jboss.forge.addon.as.jboss.common.deployment.DeploymentFailureException;
import org.jboss.forge.addon.as.jboss.common.server.ConnectionInfo;
import org.jboss.forge.addon.as.jboss.common.server.SecurityActions;
import org.jboss.forge.addon.as.jboss.common.server.Server;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.as.jboss.wf8.server.StandaloneDeployment;
import org.jboss.forge.addon.as.jboss.wf8.ui.WildFly8ConfigurationWizard;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.PackagingFacet;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * The application server provider for WildFly 8
 * 
 * @author Jeremie Lagarde
 */
public class WildFly8Provider extends JBossProvider<WildFly8Configuration> implements ConnectionInfo
{

   @Inject
   private WildFly8ServerController serverController;

   @Inject
   private WildFly8Configuration configuration;

   private final Messages messages = Messages.INSTANCE;

   @Override
   public String getName()
   {
      return "wildfly8";
   }

   @Override
   public String getDescription()
   {
      return "WildFly 8";
   }

   @Override
   protected Class<? extends JBossConfigurationWizard> getConfigurationWizardClass()
   {
      return WildFly8ConfigurationWizard.class;
   }

   @Override
   protected WildFly8Configuration getConfig()
   {
      return configuration;
   }

   @Override
   public Result start(UIContext context)
   {
      Result result = null;
      if ((serverController.hasServer() && serverController.getServer().isRunning()) || false)// getState().isRunningState())
      {
         result = Results.fail(messages.getMessage("server.already.running"));
      }
      else
      {
         try
         {

            if (!serverController.hasServer())
               createServer(context);

            Server<ModelControllerClient> server = serverController.getServer();

            // Start the server
            server.start();
            server.checkServerState();

            if (server.isRunning())
            {
               result = Results.success(messages.getMessage("server.start.success", configuration.getVersion()));
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
            // closeConsoleOutput();
         }

      }
      return result;
   }

   private Server<ModelControllerClient> createServer(UIContext context)
   {
      final File jbossHome = new File(configuration.getPath());

      final String modulesPath = null;
      final Server<ModelControllerClient> server = serverController.createServer(this, jbossHome,
               configuration.getJavaHome(), configuration.getJvmArgs(),
               modulesPath, configuration.getServerConfigFile(), configuration.getServerPropertiesFile(),
               (long) configuration.getTimeout(), context
                        .getProvider().getOutput().out());

      // Close any previously connected clients
      serverController.closeClient();
      serverController.setServer(server);

      // Add the shutdown hook
      SecurityActions.registerShutdown(server);

      return server;
   }

   @Override
   public Result shutdown(UIContext context)
   {
      try
      {
         if (!serverController.hasServer())
         {
            createServer(context);
         }
         return serverController.shutdownServer();

      }
      catch (Exception e)
      {
         return Results.fail(e.getLocalizedMessage());
      }
      finally
      {
         serverController.closeClient();
      }
   }

   @Override
   public int getPort()
   {
      return configuration.getPort();
   }

   @Override
   public InetAddress getHostAddress()
   {
      try
      {
         return InetAddress.getByName(configuration.getHostname());
      }
      catch (UnknownHostException e)
      {
         throw new IllegalArgumentException(String.format("Host name '%s' is invalid.", configuration.getHostname()), e);
      }
   }

   @Override
   public Result deploy(UIContext context)
   {
      return processDeployment(getFaceted(), (String) context.getAttributeMap().get("path"), Type.DEPLOY);
   }

   @Override
   public Result undeploy(UIContext context)
   {
      return processDeployment(getFaceted(), (String) context.getAttributeMap().get("path"),
               Type.UNDEPLOY);
   }

   protected Result processDeployment(Project project, String path, Type type)
   {
      Result result;

      // The server must be running
      if (serverController.hasServer() && serverController.getServer().isRunning())
      {
         final PackagingFacet packagingFacet = project.getFacet(PackagingFacet.class);

         // Can't deploy what doesn't exist
         if (!packagingFacet.getFinalArtifact().exists())
            throw new DeploymentFailureException(messages.getMessage("deployment.not.found", path, type));
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
            final ModelControllerClient client = serverController.getClient();
            final Deployment deployment = StandaloneDeployment.create(client, content, null, type, null, null);
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
}