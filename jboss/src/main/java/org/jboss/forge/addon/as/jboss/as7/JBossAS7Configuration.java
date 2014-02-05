/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7;

import java.io.File;

import org.jboss.forge.addon.as.jboss.common.JBossConfiguration;

/**
 * The JBOss AS7 Configuration
 * 
 * @author Jeremie Lagarde
 */
public class JBossAS7Configuration extends JBossConfiguration
{
   private static final String ASNAME = "as7";

   private static final String CONFIG_BUNDLESDIR_KEY = "bundlesdir";
   private static final String CONFIG_JVMARGS_KEY = "jvmargs";
   private static final String CONFIG_CONFIGFILE_KEY = "serverconfigfile";

   /**
    * The default versionO
    */
   private static final String DEFAULT_VERSION = "7.1.1.Final";

   /**
    * The default path
    */
   private static final String DEFAULT_PATH = "target/jboss-as-dist";

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

   public File getBundlesDir()
   {
      String file = config.getString(index + CONFIG_BUNDLESDIR_KEY);
      if (file != null)
         return new File(file);
      return null;
   }

   public String[] getJvmArgs()
   {
      return config.getStringArray(index + CONFIG_JVMARGS_KEY);
   }

   public void  setJvmArgs(String[] args)
   {
      config.addProperty(index + CONFIG_JVMARGS_KEY, args);
   }
   
   public String getServerConfigFile()
   {
      return config.getString(index + CONFIG_CONFIGFILE_KEY);
   }
}
