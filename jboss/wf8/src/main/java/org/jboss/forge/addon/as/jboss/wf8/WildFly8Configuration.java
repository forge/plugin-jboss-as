/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.wf8;

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;

/**
 * The WildFly 8 Configuration
 * 
 * @author Jeremie Lagarde
 */
public class WildFly8Configuration extends JBossConfiguration
{
   private static final String ASNAME = "wf8";

   /**
    * The default version
    */
   private static final String DEFAULT_VERSION = "8.0.0.Final";

   /**
    * The default path
    */
   private static final String DEFAULT_PATH = "target/wildfly-dist";

   @Override
   protected String getASName()
   {
      return ASNAME;
   }

   @Override
   protected String getDefaultVersion()
   {
      return DEFAULT_VERSION;
   }
   
   @Override
   public String getDefaultPath()
   {
      return DEFAULT_PATH;
   }

}
