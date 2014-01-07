/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.util.List;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.UICommand;
import org.jboss.forge.addon.ui.context.UIValidationContext;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Provider extends AbstractFacet<Project> implements ApplicationServerProvider
{

   @Override
   public boolean install()
   {
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return true;
   }

   @Override
   public void validate(UIValidationContext context)
   {
   }

   @Override
   public String getName()
   {
      return "JBossAS7";
   }

   @Override
   public String getDescription()
   {
      return "JBoss AS7";
   }

   @Override
   public List<Class<? extends UICommand>> getSetupFlow()
   {
      return null;
   }

}
