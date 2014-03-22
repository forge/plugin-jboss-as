/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.ui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.furnace.services.Imported;

/**
 * AS Setup Wizard Implementation.
 * 
 * @author Jeremie Lagarde
 */
public class ASSetupWizardImpl extends AbstractASWizardImpl implements ASSetupWizard
{

   @Override
   protected String getName()
   {
      return "Setup";
   }

   @Override
   protected String getDescription()
   {
      return "Setup the Application Server";
   }
   
   @Override
   public boolean isEnabled(UIContext context)
   {
      return true;
   }
   
   @Inject
   @WithAttributes(label = "AS type", required = true)
   private UISelectOne<ApplicationServerProvider> server;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {

      Imported<ApplicationServerProvider> providerInstances = registry.getServices(ApplicationServerProvider.class);
      List<ApplicationServerProvider> providerList = new ArrayList<ApplicationServerProvider>();
      for (ApplicationServerProvider provider : providerInstances)
      {
         providerList.add(provider);
      }
      server.setDefaultValue(providerList.get(0));
      server.setValueChoices(providerList);
      server.setItemLabelConverter(new Converter<ApplicationServerProvider, String>()
      {

         @Override
         public String convert(ApplicationServerProvider source)
         {
            return source == null ? null : source.getName();
         }
      });
      builder.add(server);
   }

   @Override
   public void validate(UIValidationContext validator)
   {
      super.validate(validator);
      server.getValue().validate(validator);
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      ApplicationServerProvider selectedProvider = server.getValue();
      UIContext uiContext = context.getUIContext();
      uiContext.getAttributeMap().put(ApplicationServerProvider.class, selectedProvider);

      Project project = getSelectedProject(context);      
      uiContext.getAttributeMap().put(Project.class, project);

      Configuration config = project.getFacet(ConfigurationFacet.class).getConfiguration();
      config.setProperty("as.name", selectedProvider.getName());

      // Get the step sequence from the selected application server provider
      List<Class<? extends UICommand>> setupFlow = selectedProvider.getSetupFlow();

      // Extract the first command to obtain the next step
      Class<? extends UICommand> next = setupFlow.remove(0);
      Class<?>[] additional = setupFlow.toArray(new Class<?>[setupFlow.size()]);
      return context.navigateTo(next, (Class<? extends UICommand>[]) additional);
   }
   
   @Override
   protected Result execute(ApplicationServerProvider provider, UIContext context) throws Exception
   {
      provider.setup(context);
      provider.install(context);
      return Results.success("The applicaion server was setup successfully.");      
   }

}