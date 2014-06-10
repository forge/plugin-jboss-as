/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;
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
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
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

   @Inject
   @WithAttributes(label = "Port")
   private UIInput<Integer> port;

   @Inject
   @WithAttributes(label = "Stratup Timeout")
   private UIInput<Integer> timeout;

   @Inject
   @WithAttributes(label = "Java home")
   private UIInput<DirectoryResource> javaHome;

   @Inject
   @WithAttributes(label = "jvmargs")
   private UIInputMany<String> jvmargs;

   @Inject
   @WithAttributes(label = "Server config file")
   private UIInput<FileResource<?>> configFile;

   @Inject
   @WithAttributes(label = "Server properties file")
   private UIInput<FileResource<?>> propertiesFile;

   protected abstract JBossConfiguration getConfig();

   protected abstract DependencyBuilder getJBossDistribution();

   @Override
   protected boolean isProjectRequired()
   {
      return true;
   }

   @Override
   protected ProjectFactory getProjectFactory()
   {
      return projectFactory;
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      JBossConfiguration config = getConfig();

      if (version.getValue() != null)
      {
         config.setDistribution(version.getValue());
      }

      if (installDir.getValue() != null)
      {
         config.setPath(installDir.getValue().getFullyQualifiedName());
      }

      config.setPort(port.getValue());

      config.setTimeout(timeout.getValue());

      if (javaHome.getValue() != null)
      {
         config.setJavaHome(javaHome.getValue().getFullyQualifiedName());
      }

      if (jvmargs.getValue() != null && jvmargs.getValue().iterator().hasNext())
      {
         List<String> args = new ArrayList<String>();
         for (String arg : jvmargs.getValue())
         {
            args.add(arg);
         }
         config.setJvmArgs(args.toArray(new String[args.size()]));
      }
      else
      {
         config.setJvmArgs(null);
      }

      if (configFile.getValue() != null)
      {
         config.setServerConfigFile(configFile.getValue().getFullyQualifiedName());
      }

      if (propertiesFile.getValue() != null)
      {
         config.setServerPropertiesFile(propertiesFile.getValue().getFullyQualifiedName());
      }

      return null;
   }

   @Override
   public void validate(UIValidationContext context)
   {
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext context = builder.getUIContext();
      JBossConfiguration config = getConfig();
      Project project = getSelectedProject(context);
      config.setFaceted(project);

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

      String defaultVersion = config.getVersion();
      for (Coordinate coordinate : dists)
      {
         if (coordinate.getVersion().equals(defaultVersion))
            version.setDefaultValue(coordinate);
      }

      String path = config.getPath();
      if (path == null)
      {
         installDir.setDefaultValue(project.getRootDirectory().getChildDirectory(config.getDefaultPath()));
      }
      else
      {
         installDir.setDefaultValue(resourceFactory.create(DirectoryResource.class, new File(path)));
      }

      port.setDefaultValue(config.getPort());

      timeout.setDefaultValue(config.getTimeout());

      if (config.getJavaHome() != null)
      {
         javaHome.setValue(resourceFactory.create(DirectoryResource.class, new File(config.getJavaHome())));
      }

      String[] args = config.getJvmArgs();
      if (args != null && args.length > 0)
         jvmargs.setDefaultValue(Arrays.asList(args));

      if (config.getServerConfigFile() != null)
      {
         configFile.setValue(resourceFactory.create(FileResource.class, new File(config.getServerConfigFile())));
      }

      if (config.getServerPropertiesFile() != null)
      {
         propertiesFile
                  .setValue(resourceFactory.create(FileResource.class, new File(config.getServerPropertiesFile())));
      }

      builder.add(version).add(this.installDir).add(port).add(timeout).add(javaHome).add(jvmargs).add(configFile)
               .add(propertiesFile);
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      // No-op. Do nothing.
      return Results.success();
   }

}
