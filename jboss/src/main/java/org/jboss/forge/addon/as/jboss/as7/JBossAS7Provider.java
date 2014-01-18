/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.jboss.as7.util.Files;
import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Provider extends AbstractFacet<Project> implements ApplicationServerProvider
{

   @Inject
   private Configuration config;

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

   @Override
   public String getName()
   {
      return "jbossas7";
   }

   @Override
   public String getDescription()
   {
      return "JBoss AS7";
   }

   @Override
   public List<Class<? extends UICommand>> getSetupFlow()
   {
      List<Class<? extends UICommand>> setupCommands = new ArrayList<Class<? extends UICommand>>();
      setupCommands.add(JBossAS7ConfigurationWizard.class);
      return setupCommands;
   }

   @Override
   public void setup(UIContext context)
   {
   }

   @Override
   public DirectoryResource install(UIContext context)
   {
      Coordinate coordinate = (Coordinate) context.getAttributeMap().get(Coordinate.class);
      DirectoryResource path = (DirectoryResource) context.getAttributeMap().get(DirectoryResource.class);

      Dependency dist = resolver.resolveArtifact(DependencyQueryBuilder.create(coordinate));

      File target = new File(path.getFullyQualifiedName());

      try
      {
         if (target.exists())
         {
            if (!Files.isEmptyDirectory(target))
            {
               Files.deleteRecursively(target);
            }
            if (!target.delete())
            {
               return null;
            }
         }
         Files.extractAppServer(dist.getArtifact().getFullyQualifiedName(), target);
         return path;
      }
      catch (IOException e)
      {
         Files.deleteRecursively(target);
      }
      finally
      {
         // wait.stop();
      }

      return null;
   }
}
