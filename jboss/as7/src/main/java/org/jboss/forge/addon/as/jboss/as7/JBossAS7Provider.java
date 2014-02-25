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

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.forge.addon.as.jboss.as7.server.ServerOperations;
import org.jboss.forge.addon.as.jboss.as7.server.StandaloneServer;
import org.jboss.forge.addon.as.jboss.as7.server.deployment.standalone.StandaloneDeployment;
import org.jboss.forge.addon.as.jboss.as7.ui.JBossAS7ConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.deployment.Deployment;
import org.jboss.forge.addon.as.jboss.common.deployment.Deployment.Type;
import org.jboss.forge.addon.as.jboss.common.deployment.DeploymentFailureException;
import org.jboss.forge.addon.as.jboss.common.server.ConnectionInfo;
import org.jboss.forge.addon.as.jboss.common.server.SecurityActions;
import org.jboss.forge.addon.as.jboss.common.server.Server;
import org.jboss.forge.addon.as.jboss.common.server.ServerConsoleWrapper;
import org.jboss.forge.addon.as.jboss.common.server.ServerInfo;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.as.jboss.common.util.Streams;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.PackagingFacet;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import com.google.common.net.InetAddresses;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Provider extends JBossProvider<JBossAS7Configuration> implements ConnectionInfo
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
   public int getPort()
   {
      return configuration.getPort();
   }

   @Override
   public InetAddress getHostAddress()
   {
      return InetAddresses.forString("127.0.0.1"); // configuration.getHostname();
   }

   @Override
   public CallbackHandler getCallbackHandler()
   {
      return new ClientCallbackHandler();
   }

   @Override
   public Result start(UIContext context)
   {
      Result result = null;
      if ((serverController.hasServer() && serverController.getServer().isRunning()))
      {
         result = Results.fail(messages.getMessage("server.already.running"));
      }
      else
      {
         try
         {
            // Clean-up possible old console output
            closeConsoleOutput();

            // Validate the environment
            final File jbossHome = new File(configuration.getPath());
            if (!jbossHome.isDirectory())
            {
               Results.fail(String.format("JBOSS_HOME '%s' is not a valid directory.", jbossHome));
            }
            // JVM arguments should be space delimited
            final String[] jvmArgs = configuration.getJvmArgs();
            final String javaHome = System.getenv("JAVA_HOME"); // configuration.getJavahome();
            // if (this.javaHome == null) {
            // javaHome = SecurityActions.getEnvironmentVariable("JAVA_HOME");
            // } else {
            // javaHome = this.javaHome;
            // }
            final String modulesPath = configuration.getPath() + "/modules"; /* configuration.getModulesDir() */
            final String serverConfig = configuration.getServerConfigFile();
            final String propertiesFile = null; // configuration.getServerPropertiesFile()

            final ServerInfo serverInfo = ServerInfo.of(this, javaHome, jbossHome,
                     modulesPath, jvmArgs, serverConfig, propertiesFile, (long) configuration.getTimeout());
            if (!serverInfo.getModulesDir().isDirectory())
            {
               Results.fail(String.format("Modules path '%s' is not a valid directory.", modulesPath));
            }
            // Create the server
            final Server server = new StandaloneServer(serverInfo);
            // Add the shutdown hook
            SecurityActions.registerShutdown(server);
            // Start the server
            // log.info("Server is starting up.");
            server.start();
            server.checkServerState();

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

   @Override
   public Result shutdown(UIContext context)
   {
      Result result = Results.success(messages.getMessage("server.shutdown.success"));
      final Server server = serverController.getServer();
      if (server == null)
      {
         try
         {
            final ModelNode response = serverController.getClient().execute(ServerOperations
                     .createOperation(org.jboss.forge.addon.as.jboss.as7.server.ServerOperations.SHUTDOWN));
            if (org.jboss.forge.addon.as.jboss.as7.server.ServerOperations.isSuccessfulOutcome(response))
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
      final Server server = serverController.getServer();
      Result result;

      // The server must be running
      if (server.isRunning())
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
            final Deployment deployment = StandaloneDeployment.create(client, content, null,type);
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

   private void closeConsoleOutput()
   {
      Streams.safeFlush(consoleOut);
      Streams.safeClose(consoleOut);
      consoleOut = null;
   }

}