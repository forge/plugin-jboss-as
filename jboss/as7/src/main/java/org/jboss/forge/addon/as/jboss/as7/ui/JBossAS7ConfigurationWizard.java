/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.as.jboss.as7.JBossAS7Configuration;
import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;

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

}