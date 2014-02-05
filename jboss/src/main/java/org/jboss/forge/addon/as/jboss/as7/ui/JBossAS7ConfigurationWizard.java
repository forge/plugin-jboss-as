/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.as.jboss.as7.JBossAS7Configuration;
import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.ManyValued;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;

/**
 * The JBoss AS7 Configuration Wisard
 * 
 * @author Jeremie Lagarde
 */
@FacetConstraint({ DependencyFacet.class, ResourcesFacet.class })
public class JBossAS7ConfigurationWizard extends JBossConfigurationWizard
{

   @Inject
   private JBossAS7Configuration config;

   @Inject
   @WithAttributes(label = "jvmargs")
   private UIInputMany<String> jvmargs;
   
   private DependencyBuilder jbossAS7Dist = DependencyBuilder.create()
            .setGroupId("org.jboss.as")
            .setArtifactId("jboss-as-dist")
            .setVersion("[7.0.0,8.0.0[")
            .setPackaging("zip");

   @Override
   protected JBossConfiguration getConfig()
   {
      return config;
   }

   @Override
   protected DependencyBuilder getJBossDistribution()
   {
      return jbossAS7Dist;
   }


   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      super.initializeUI(builder);
            
      String[] args = config.getJvmArgs();
      if(args!=null && args.length>0)
         jvmargs.setDefaultValue(Arrays.asList(args));
      
      builder.add(jvmargs);
   }
   
   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      NavigationResult result = super.next(context);

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
      
      return result;
   }
}
