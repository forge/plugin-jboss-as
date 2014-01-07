/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.spi;

import java.util.List;

import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.UIValidator;

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
    * Return the {@link List} of {@link UICommands} classes that begins the application server setup of this type, if any.
    */
   List<Class<? extends UICommand>> getSetupFlow();   

}
