/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Files;
import org.jboss.forge.addon.as.jboss.common.util.Messages;
import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;

/**
 * The application server provider
 * 
 * @author Jeremie Lagarde
 */
@FacetConstraint({ ProjectFacet.class, ConfigurationFacet.class, ResourcesFacet.class })
public abstract class JBossProvider<CONFIG extends JBossConfiguration> extends AbstractFacet<Project> implements
         ApplicationServerProvider
{
   private final static Messages messages = Messages.INSTANCE;

   @Inject
   private DependencyResolver resolver;

   @Inject
   private Converter<File, DirectoryResource> directoryResourceConverter;

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

   protected abstract Class<? extends JBossConfigurationWizard> getConfigurationWizardClass();

   protected abstract CONFIG getConfig();

   @Override
   public void setFaceted(Project project)
   {
      super.setFaceted(project);
      if (project.hasFacet(ConfigurationFacet.class))
         getConfig().setProject(project);
   }

   @Override
   public List<Class<? extends UICommand>> getSetupFlow()
   {
      List<Class<? extends UICommand>> setupCommands = new ArrayList<Class<? extends UICommand>>();
      setupCommands.add(getConfigurationWizardClass());
      return setupCommands;
   }

   @Override
   public void setup(UIContext context)
   {
   }

   @Override
   public boolean isASInstalled(UIContext context)
   {
      return getConfig().getPath() != null && (new File(getConfig().getPath())).exists();
   }

   @Override
   public DirectoryResource install(UIContext context)
   {
      JBossConfiguration config = getConfig();
      String path = config.getPath();

      Dependency dist = resolver.resolveArtifact(DependencyQueryBuilder.create(config.getDistibution()));

      File target = new File(path);

      try
      {
         if (target.exists())
         {
            if (!Files.isEmptyDirectory(target))
            {
               Files.deleteRecursively(target);
            }
         }
         Files.extractAppServer(dist.getArtifact().getFullyQualifiedName(), target);
         return directoryResourceConverter.convert(target);
      }
      catch (IOException e)
      {
         Files.deleteRecursively(target);
      }

      return null;
   }

   public CallbackHandler getCallbackHandler()
   {
      return new ClientCallbackHandler();
   }

}