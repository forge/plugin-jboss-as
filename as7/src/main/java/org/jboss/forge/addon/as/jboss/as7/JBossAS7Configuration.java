/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;

/**
 * The JBoss AS 7 Configuration
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Configuration extends JBossConfiguration
{
   private static final String ASNAME = "as7";

   /**
    * The default version
    */
   private static final String DEFAULT_VERSION = "7.1.1.Final";

   /**
    * The default path
    */
   private static final String DEFAULT_PATH = "target/jboss-as-dist";

   /**
    * The default port
    */
   static final int DEFAULT_PORT = 9999;

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

   @Override
   protected int getDefaultPort()
   {
      return DEFAULT_PORT;
   }

}
