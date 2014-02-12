/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8;

import java.io.File;
import java.net.InetAddress;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.as.jboss.wf8.server.ConnectionInfo;
import org.jboss.forge.addon.as.jboss.wf8.server.SecurityActions;
import org.jboss.forge.addon.as.jboss.wf8.server.Server;
import org.jboss.forge.addon.as.jboss.wf8.server.ServerInfo;
import org.jboss.forge.addon.as.jboss.wf8.server.StandaloneServer;
import org.jboss.forge.addon.as.jboss.wf8.ui.WildFly8ConfigurationWizard;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import com.google.common.net.InetAddresses;

/**
 * The application server provider for WildFly 8
 * 
 * @author Jeremie Lagarde
 */
public class WildFly8Provider extends JBossProvider<WildFly8Configuration> implements ConnectionInfo
{

   @Inject
   private ServerController serverController;

   @Inject
   private WildFly8Configuration configuration;

   @Inject
   private CallbackHandler callbackHandler;

   @Inject
   private ProjectFactory projectFactory;

   private ServerConsoleWrapper consoleOut;

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
      if ((serverController.hasServer() && serverController.getServer().isRunning()) || false)//getState().isRunningState())
      {
         result = Results.fail(messages.getMessage("server.already.running"));
      }
      else
      {
         try
         {

            // Validate the environment
            final File jbossHome = new File(configuration.getPath());
            if (!jbossHome.isDirectory()) {
               Results.fail(String.format("JBOSS_HOME '%s' is not a valid directory.", jbossHome));
            }
            // JVM arguments should be space delimited
            final String[] jvmArgs = null; // (this.jvmArgs == null ? null : this.jvmArgs.split("\\s+"));
            final String javaHome;
//            if (this.javaHome == null) {
//                javaHome = SecurityActions.getEnvironmentVariable("JAVA_HOME");
//            } else {
//                javaHome = this.javaHome;
//            }
            final String modulesPath = null;
            final String serverConfig = null;
            final String propertiesFile = null;
            
            final ServerInfo serverInfo = ServerInfo.of(this, System.getenv("JAVA_HOME") /* jreHome */, jbossHome, modulesPath, jvmArgs, serverConfig, propertiesFile, (long)configuration.getTimeout());
            if (!serverInfo.getModulesDir().isDirectory()) {
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
           // closeConsoleOutput();
         }

      }
      return result;
   }

   @Override
   public int getPort()
   {
      return configuration.getPort();
   }

   @Override
   public InetAddress getHostAddress()
   {  return InetAddresses.forString("127.0.0.1"); //configuration.getHostname();
   }

   @Override
   public CallbackHandler getCallbackHandler()
   {
      return new ClientCallbackHandler();
   }

}