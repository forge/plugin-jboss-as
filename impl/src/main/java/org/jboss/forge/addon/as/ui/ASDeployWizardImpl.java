/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;

/**
 * AS Deploy Wizard Implementation.
 * 
 * @author Jeremie Lagarde
 */
public class ASDeployWizardImpl extends AbstractProjectCommand implements ASStartWizard
{

   @Inject
   Configuration config;
   
   @Inject
   private ProjectFactory factory;

   @Inject
   private AddonRegistry registry;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      
   }

   @Override
   public void validate(UIValidationContext validator)
   {
      super.validate(validator);
   }


   @Override
   public Metadata getMetadata(UIContext context)
   {
      return Metadata.from(super.getMetadata(context), getClass()).name("AS: Deploy")
               .description("Deploy an application in the AS")
               .category(Categories.create("AS", "Deploy"));
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Imported<ApplicationServerProvider> providerInstances = registry.getServices(ApplicationServerProvider.class);
      String providerName = config.getString("as.name");
      ApplicationServerProvider selectedProvider = null;
      for (ApplicationServerProvider provider : providerInstances)
      {
         if(provider.getName().equals(providerName))
            selectedProvider = provider;
      }
      
      if(selectedProvider == null)
         return Results.fail("No application server provider found.");
      
      return selectedProvider.deploy(context.getUIContext());      
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      return null;
   }

   @Override
   protected boolean isProjectRequired()
   {
      return true;
   }

   @Override
   protected ProjectFactory getProjectFactory()
   {
      return factory;
   }
}