/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.ui;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;
import org.jboss.forge.addon.as.jboss.wf8.WildFly8Configuration;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
@FacetConstraint({ DependencyFacet.class, ResourcesFacet.class })
public abstract class JBossConfigurationWizard extends AbstractProjectCommand implements UIWizardStep
{

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private ResourceFactory resourceFactory;


   @Inject
   @WithAttributes(label = "Version")
   private UISelectOne<Coordinate> version;

   @Inject
   @WithAttributes(label = "Install directory", description = "The path for installing the application server", required = true)
   private UIInput<DirectoryResource> installDir;

   protected abstract JBossConfiguration getConfig();

   protected abstract DependencyBuilder getJBossDistribution();

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      if (version.getValue() != null)
      {
         getConfig().setDistribution(version.getValue());
      }

      if (installDir.getValue() != null)
      {
         getConfig().setInstalldir(installDir.getValue());
      }

      context.getUIContext().getAttributeMap().put(JBossConfiguration.class,getConfig());
      
      return null;
   }

   @Override
   public void validate(UIValidationContext context)
   {
   }

   @Override
   protected boolean isProjectRequired()
   {
      return false;
   }

   @Override
   protected ProjectFactory getProjectFactory()
   {
      return projectFactory;
   }

   @Override
   public boolean isEnabled(UIContext context)
   {
      return false;
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext context = builder.getUIContext();

      Project project = getSelectedProject(context);
      
      DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);
      List<Coordinate> dists = dependencyFacet.resolveAvailableVersions(getJBossDistribution());

      version.setValueChoices(dists);
      version.setItemLabelConverter(new Converter<Coordinate, String>()
      {
         @Override
         public String convert(Coordinate coordinate)
         {
            return coordinate.getVersion();
         }
      });

      String defaultVersion = getConfig().getVersion();
      for (Coordinate coordinate : dists)
      {
         if (coordinate.getVersion().equals(defaultVersion))
            version.setDefaultValue(coordinate);
      }

      String path = getConfig().getPath();
      if(path == null) { 
         installDir.setDefaultValue(project.getRootDirectory().getChildDirectory(getConfig().getDefaultPath()));
      } else {
         installDir.setDefaultValue(resourceFactory.create(DirectoryResource.class, new File(path)));
      }
      
      builder.add(version).add(this.installDir);
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      // No-op. Do nothing.
      return Results.success();
   }

}
