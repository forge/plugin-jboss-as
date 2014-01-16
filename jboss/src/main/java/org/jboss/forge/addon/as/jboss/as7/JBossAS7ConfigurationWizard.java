/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.FileResource;
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

   /**
    * The default host name
    */
   static final String DEFAULT_HOSTNAME = "localhost";

   /**
    * The default management port
    */
   static final int DEFAULT_PORT = 9999;

   @Inject
   private ProjectFactory factory;

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

   private Configuration configuration;

   private DependencyBuilder jbossAS7Dist = DependencyBuilder.create()
            .setGroupId("org.jboss.as")
            .setArtifactId("jboss-as-dist")
            .setVersion("[7.0.0,8.0.0[")
            .setPackaging("zip");

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      if (version.getValue() != null) {
         configuration.setProperty(CONFIG_VERSION_KEY, version.getValue().getVersion());
         context.getUIContext().setAttribute(Coordinate.class, version.getValue());
      }

      if (hostname.getValue() != null)
         configuration.setProperty(CONFIG_HOSTNAME_KEY, hostname.getValue());

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
            return coordinate.getVersion() + "-";
         }
      });

      hostname.setValue(configuration.getString(CONFIG_HOSTNAME_KEY, DEFAULT_HOSTNAME));
      port.setValue(configuration.getString(CONFIG_PORT_KEY, "" + DEFAULT_PORT));
      builder.add(version).add(hostname).add(port);
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      // No-op. Do nothing.
      return Results.success();
   }

}
