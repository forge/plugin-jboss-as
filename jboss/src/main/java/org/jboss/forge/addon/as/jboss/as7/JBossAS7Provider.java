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
 */
public class JBossAS7Provider extends AbstractFacet<Project> implements ApplicationServerProvider
{

   @Override
   public boolean install()
   {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      // TODO Auto-generated method stub
      return true;
   }

   @Override
   public void validate(UIValidationContext context)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public String getName()
   {
      // TODO Auto-generated method stub
      return "jbossas7";
   }

   @Override
   public String getDescription()
   {
      // TODO Auto-generated method stub
      return "JBossAS7";
   }

   @Override
   public List<Class<? extends UICommand>> getSetupFlow()
   {
      // TODO Auto-generated method stub
      return null;
   }

}
