/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import org.jboss.forge.addon.as.jboss.as7.ui.JBossAS7ConfigurationWizard;
import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;

/**
 * The application server provider for JBoss AS7
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Provider extends JBossProvider
{

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
   protected Class<? extends JBossConfigurationWizard> getConfigurationWizardClass()
   {
      return JBossAS7ConfigurationWizard.class;
   }

}
