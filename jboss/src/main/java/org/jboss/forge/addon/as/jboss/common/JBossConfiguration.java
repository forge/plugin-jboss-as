package org.jboss.forge.addon.as.jboss.common;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.resource.DirectoryResource;

public abstract class JBossConfiguration
{
   private static final String CONFIG_PREFIX = "as.jboss";

   private static final String CONFIG_VERSION_KEY = "version";

   private static final String CONFIG_PORT_KEY = "port";

   private static final String CONFIG_HOSTNAME_KEY = "hostname";

   private static final String CONFIG_PATH_KEY = "path";

   protected String index = CONFIG_PREFIX + "." + getASName() + ".";

   /**
    * The default host name
    */
   static final String DEFAULT_HOSTNAME = "localhost";

   /**
    * The default port
    */
   static final int DEFAULT_PORT = 9999;

   
   @Inject
   protected Configuration config;

   private Coordinate distribution;

   private DirectoryResource installDir;

   protected abstract String getASName();

   protected abstract String getDefaultVersion();

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
      return config.getString(CONFIG_HOSTNAME_KEY, DEFAULT_HOSTNAME);
   }

   public int getPort()
   {
      return config.getInt(CONFIG_PORT_KEY, DEFAULT_PORT);
   }

   public Coordinate getDistibution()
   {
      return distribution;
   }

   public void setDistribution(Coordinate dist)
   {
      setVersion(dist.getVersion());
      this.distribution = dist;
   }

   public DirectoryResource getInstallDir()
   {
      return installDir;
   }

   public void setInstalldir(DirectoryResource dir)
   {
      setPath(dir.getFullyQualifiedName());
      this.installDir = dir;
   }
}
