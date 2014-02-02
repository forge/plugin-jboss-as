/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8;

import org.jboss.forge.addon.as.jboss.common.JBossProvider;
import org.jboss.forge.addon.as.jboss.common.ui.JBossConfigurationWizard;
import org.jboss.forge.addon.as.jboss.wf8.ui.WildFly8ConfigurationWizard;

/**
 * The application server provider for WildFly 8
 * 
 * @author Jeremie Lagarde
 */
public class WildFly8Provider extends JBossProvider
{
   @Override
   public String getName()
   {
      return "wildfly8";
   }

   @Override
   public String getDescription()
   {
      return "WildFly 8";
   }

   @Override
   protected Class<? extends JBossConfigurationWizard> getConfigurationWizardClass()
   {
      return WildFly8ConfigurationWizard.class;
   }

}