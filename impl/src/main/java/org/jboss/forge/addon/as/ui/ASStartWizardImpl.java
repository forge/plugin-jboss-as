/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.ui;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Imported;

/**
 * AS Setup Wizard Implementation.
 * 
 * @author Jeremie Lagarde
 */
public class ASStartWizardImpl extends AbstractProjectCommand implements ASStartWizard
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
      return Metadata.from(super.getMetadata(context), getClass()).name("AS: Start")
               .description("Start the AS")
               .category(Categories.create("AS", "Start"));
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Imported<ApplicationServerProvider> providerInstances = registry.getServices(ApplicationServerProvider.class);
      String providerName = config.getString("as.name");
      ApplicationServerProvider selectedProvider = providerInstances.get();
      selectedProvider.start(context.getUIContext());
      return Results.success("The application server was started successfully.");
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
//      ApplicationServerProvider selectedProvider = .getValue();
//      UIContext uiContext = context.getUIContext();
//      uiContext.getAttributeMap().put(ApplicationServerProvider.class, selectedProvider);
//
//      Project project = getSelectedProject(context);
//      uiContext.getAttributeMap().put(Project.class, project);
//
//      // Get the step sequence from the selected application server provider
//      List<Class<? extends UICommand>> setupFlow = selectedProvider.getSetupFlow();
//
//      // Extract the first command to obtain the next step
//      Class<? extends UICommand> next = setupFlow.remove(0);
//      Class<?>[] additional = setupFlow.toArray(new Class<?>[setupFlow.size()]);
//      return context.navigateTo(next, (Class<? extends UICommand>[]) additional);
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