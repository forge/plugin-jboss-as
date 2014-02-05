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

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.util.Files;
import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * The application server provider
 * 
 * @author Jeremie Lagarde
 */
public abstract class JBossProvider extends AbstractFacet<Project> implements ApplicationServerProvider
{

   @Inject
   private DependencyResolver resolver;

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
   public DirectoryResource install(UIContext context)
   {
      JBossConfiguration config = (JBossConfiguration) context.getAttributeMap().get(JBossConfiguration.class);
      DirectoryResource path = config.getInstallDir();

      Dependency dist = resolver.resolveArtifact(DependencyQueryBuilder.create(config.getDistibution()));

      File target = new File(path.getFullyQualifiedName());

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
         return path;
      }
      catch (IOException e)
      {
         Files.deleteRecursively(target);
      }

      return null;
   }

   @Override
   public Result start(UIContext context)
   {
      return Results.fail("Not implemented yet");
   }
}