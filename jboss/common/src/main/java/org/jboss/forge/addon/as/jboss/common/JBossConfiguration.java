/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common;

import org.apache.commons.lang.StringUtils;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.configuration.facets.ConfigurationFacet;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;

/**
 * The Common JBoss Configuration
 * 
 * @author Jeremie Lagarde
 */
public abstract class JBossConfiguration extends AbstractFacet<Project>
{
   private static final String CONFIG_PREFIX = "as.jboss";
   private static final String CONFIG_VERSION_KEY = "version";
   private static final String CONFIG_PORT_KEY = "port";
   private static final String CONFIG_TIMEOUT_KEY = "timeout";
   private static final String CONFIG_HOSTNAME_KEY = "hostname";
   private static final String CONFIG_PATH_KEY = "path";
   private static final String CONFIG_DISTRIBUTION_KEY = "dist";
   private static final String CONFIG_JAVAHOME_KEY = "javahome";
   private static final String CONFIG_JVMARGS_KEY = "jvmargs";
   private static final String CONFIG_CONFIGFILE_KEY = "serverconfigfile";
   private static final String CONFIG_PROPERTIESFILE_KEY = "propertiesfile";
   
   protected String index = CONFIG_PREFIX + "." + getASName() + ".";

   /**
    * The default host name
    */
   static final String DEFAULT_HOSTNAME = "localhost";

   /**
    * The default timeout
    */
   static final int DEFAULT_TIMEOUT = 90;

   protected Configuration config;

   protected abstract String getASName();

   protected abstract String getDefaultVersion();

   @Override
   public boolean install()
   {
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return true;
   }

   void setProject(Project project)
   {
      if (project.hasFacet(ConfigurationFacet.class))
         config = project.getFacet(ConfigurationFacet.class).getConfiguration();
   }

   public void setFaceted(Project project)
   {
      super.setFaceted(project);
      setProject(project);
   }

   public String getVersion()
   {
      return config.getString(index + CONFIG_VERSION_KEY, getDefaultVersion());
   }

   public void setVersion(String version)
   {
      if (StringUtils.isNotBlank(version))
         config.setProperty(index + CONFIG_VERSION_KEY, version);
      else
         config.clearProperty(index + CONFIG_VERSION_KEY);
   }

   public abstract String getDefaultPath();

   public String getPath()
   {
      if (config == null)
         return null;
      return config.getString(index + CONFIG_PATH_KEY);
   }

   public void setPath(String path)
   {
      if (StringUtils.isNotBlank(path))
         config.setProperty(index + CONFIG_PATH_KEY, path);
      else
         config.clearProperty(index + CONFIG_PATH_KEY);
   }

   public String getHostname()
   {
      return config.getString(index + CONFIG_HOSTNAME_KEY, DEFAULT_HOSTNAME);
   }

   protected abstract int getDefaultPort();

   public int getPort()
   {
      return config.getInt(index + CONFIG_PORT_KEY, getDefaultPort());
   }

   public void setPort(int port)
   {
      if (port > 0)
         config.setProperty(index + CONFIG_PORT_KEY, port);
      else
         config.clearProperty(index + CONFIG_PORT_KEY);
   }

   public int getTimeout()
   {
      return config.getInt(index + CONFIG_TIMEOUT_KEY, DEFAULT_TIMEOUT);
   }

   public void setTimeout(int timeout)
   {
      if (timeout > 0)
         config.setProperty(index + CONFIG_TIMEOUT_KEY, timeout);
      else
         config.clearProperty(index + CONFIG_TIMEOUT_KEY);
   }

   public Coordinate getDistibution()
   {
      return CoordinateBuilder.create(config.getString(index + CONFIG_DISTRIBUTION_KEY));
   }

   public void setDistribution(Coordinate dist)
   {
      setVersion(dist.getVersion());
      config.setProperty(index + CONFIG_DISTRIBUTION_KEY, dist.toString());
   }

   public String getJavaHome()
   {
      return config.getString(index + CONFIG_JAVAHOME_KEY);
   }

   public void setJavaHome(String javaHome)
   {
      config.addProperty(index + CONFIG_JAVAHOME_KEY, javaHome);
   }

   public String[] getJvmArgs()
   {
      return config.getStringArray(index + CONFIG_JVMARGS_KEY);
   }

   public void setJvmArgs(String[] args)
   {
      config.addProperty(index + CONFIG_JVMARGS_KEY, args);
   }

   public String getServerConfigFile()
   {
      return config.getString(index + CONFIG_CONFIGFILE_KEY);
   }

   public void setServerConfigFile(String serverConfigFile)
   {
      config.addProperty(index + CONFIG_CONFIGFILE_KEY, serverConfigFile);
   }

   public String getServerPropertiesFile()
   {
      return config.getString(index + CONFIG_PROPERTIESFILE_KEY);
   }

   public void setServerPropertiesFile(String serverPropertiesFile)
   {
      config.addProperty(index + CONFIG_PROPERTIESFILE_KEY, serverPropertiesFile);
   }

}
