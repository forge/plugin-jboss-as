/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.converter.DirectoryResourceConverter;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
@FacetConstraint({ DependencyFacet.class, ResourcesFacet.class })
public class JBossAS7ConfigurationWizard extends AbstractProjectCommand implements UIWizardStep
{
   private static final String CONFIG_VERSION_KEY = "as.jboss.version";

   private static final String CONFIG_PORT_KEY = "as.jboss.port";

   private static final String CONFIG_HOSTNAME_KEY = "as.jboss.hostname";

   private static final String CONFIG_PATH_KEY = "as.jboss.path";

   /**
    * The default host name
    */
   static final String DEFAULT_HOSTNAME = "localhost";

   /**
    * The default management port
    */
   static final int DEFAULT_PORT = 9999;

   /**
    * The default version
    */
   static final String DEFAULT_VERSION = "7.1.1.Final";

   /**
    * The default path
    */
   static final String DEFAULT_PATH = "target/jboss-as-dist";
   
   @Inject
   private ProjectFactory factory;

   @Inject
   private DependencyResolver resolver;

   @Inject
   @WithAttributes(label = "Java Home")
   private UIInput<FileResource<?>> javaHome;

   @Inject
   @WithAttributes(label = "Version")
   private UISelectOne<Coordinate> version;

   @Inject
   @WithAttributes(label = "Home")
   private UIInput<String> home;

   @Inject
   @WithAttributes(label = "Hostname", defaultValue = DEFAULT_HOSTNAME)
   private UIInput<String> hostname;

   @Inject
   @WithAttributes(label = "Port", defaultValue = "" + DEFAULT_PORT)
   private UIInput<String> port;

   @Inject
   @WithAttributes(label = "Install directory", description = "The path for installing the JBoss AS", required = true)
   private UIInput<DirectoryResource> installDir;

   private Configuration configuration;

   private DependencyBuilder jbossAS7Dist = DependencyBuilder.create()
            .setGroupId("org.jboss.as")
            .setArtifactId("jboss-as-dist")
            .setVersion("[7.0.0,8.0.0[")
            .setPackaging("zip");

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      if (version.getValue() != null)
      {
         configuration.setProperty(CONFIG_VERSION_KEY, version.getValue().getVersion());
         context.getUIContext().getAttributeMap().put(Coordinate.class, version.getValue());
      }

      if (hostname.getValue() != null)
         configuration.setProperty(CONFIG_HOSTNAME_KEY, hostname.getValue());

      if (installDir.getValue() != null)
      {
         String path = installDir.getValue().getFullyQualifiedName();
         String root = getSelectedProject(context).getProjectRoot().getFullyQualifiedName();
         if(path.startsWith(root))
            configuration.setProperty(CONFIG_PATH_KEY, path.substring(root.length()+1));
         context.getUIContext().getAttributeMap().put(DirectoryResource.class, installDir.getValue());
      }

      configuration.setProperty(CONFIG_PORT_KEY, port.getValue());

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
      return factory;
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Enter JBoss AS7 configuration")
               .description("Define and configure the JBoss AS7 server.");

   }

   @Override
   public boolean isEnabled(UIContext context)
   {
      return true;
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext context = builder.getUIContext();

      Project project = getSelectedProject(context);
      ConfigurationFacet configurationFacet = project.getFacet(ConfigurationFacet.class);
      configuration = configurationFacet.getConfiguration();

      DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);
      List<Coordinate> dists = dependencyFacet.resolveAvailableVersions(jbossAS7Dist);

      version.setValueChoices(dists);
      version.setItemLabelConverter(new Converter<Coordinate, String>()
      {
         @Override
         public String convert(Coordinate coordinate)
         {
            return coordinate.getVersion();
         }
      });
      
      String defaultVersion = configuration.getString(CONFIG_VERSION_KEY, DEFAULT_VERSION);
      for (Coordinate coordinate : dists)
      {
         if(coordinate.getVersion().equals(defaultVersion))
            version.setDefaultValue(coordinate);
      }
      
      hostname.setValue(configuration.getString(CONFIG_HOSTNAME_KEY, DEFAULT_HOSTNAME));
      port.setValue(configuration.getString(CONFIG_PORT_KEY, "" + DEFAULT_PORT));
      
      String path = configuration.getString(CONFIG_PATH_KEY,DEFAULT_PATH);
      installDir.setDefaultValue(project.getProjectRoot().getChildDirectory(path));   
      
      builder.add(version).add(hostname).add(port).add(installDir);
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      // No-op. Do nothing.
      return Results.success();
   }

}
