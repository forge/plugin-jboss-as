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
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;

/**
 * @author Jeremie Lagarde
 */
public abstract class AbstractASWizardImpl extends AbstractProjectCommand
{

   @Inject
   private ProjectFactory factory;

   @Inject
   protected AddonRegistry registry;

   protected abstract String getName();

   protected abstract String getDescription();

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
   }

   @Override
   public Metadata getMetadata(UIContext context)
   {
      return Metadata.from(super.getMetadata(context), getClass()).name("AS: " + getName())
               .description(getDescription()).category(Categories.create("AS"));
   }

   @Override
   public boolean isEnabled(UIContext context)
   {
      ApplicationServerProvider selectedProvider = getSelectedProvider(context);
      if (selectedProvider != null && super.isEnabled(context))
         return selectedProvider.isASInstalled(context);
      return false;
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      ApplicationServerProvider selectedProvider = getSelectedProvider(context.getUIContext());

      if (selectedProvider == null)
         return Results.fail("No application server provider found.");

      return execute(selectedProvider, context.getUIContext());
   }

   private ApplicationServerProvider getSelectedProvider(UIContext context)
   {
      ApplicationServerProvider selectedProvider = null;
      Imported<ApplicationServerProvider> providerInstances = registry.getServices(ApplicationServerProvider.class);
      Project project = getSelectedProject(context);
      if (project.hasFacet(ConfigurationFacet.class))
      {
         Configuration config = project.getFacet(ConfigurationFacet.class).getConfiguration();
         String providerName = config.getString("as.name");
         for (ApplicationServerProvider provider : providerInstances)
         {
            if (provider.getName().equals(providerName))
            {
               selectedProvider = provider;
               ((AbstractFacet) selectedProvider).setFaceted(project);
            }
         }
      }
      return selectedProvider;
   }

   protected abstract Result execute(ApplicationServerProvider provider, UIContext context) throws Exception;

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