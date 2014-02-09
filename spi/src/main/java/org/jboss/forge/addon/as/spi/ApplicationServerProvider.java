/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.spi;

import java.util.List;

import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.validate.UIValidator;

/**
 * Provides an implementation of Application Server connector to provide management operations.
 * 
 * @author Jeremie Lagarde
 */
public interface ApplicationServerProvider extends ProjectFacet, UIValidator
{
   /**
    * Return the name for this {@link ApplicationServerProvider}
    * 
    * Ex: JBossAS7
    */
   String getName();

   /**
    * Return the description for this {@link ApplicationServerProvider}
    * 
    * Ex: JBoss AS7
    */
   String getDescription();

   /**
    * Return the {@link List} of {@link UICommands} classes that begins the application server setup of this type, if
    * any.
    */
   List<Class<? extends UICommand>> getSetupFlow();

   /**
    * Set up this application server provider.
    */
   void setup(UIContext context);

   /**
    * Download and Install the application server.
    */
   DirectoryResource install(UIContext context);
   
   /**
    * Check if the application server is installed.
    */
   boolean isASInstalled(UIContext context);
   
   /**
    * Start the application server.
    */
   Result start(UIContext context);

   /**
    * Shutdown the application server.
    */
   Result shutdown(UIContext context);

   /**
    * Deploy an application in the application server.
    */
   Result deploy(UIContext uiContext);

   /**
    * Undeploy an application in the application server.
    */
   Result undeploy(UIContext uiContext);

}
