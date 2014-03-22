/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;

/**
 * AS Deploy Wizard Implementation.
 * 
 * @author Jeremie Lagarde
 */
public class ASUndeployWizardImpl extends AbstractASWizardImpl implements ASUndeployWizard
{

   @Inject
   @WithAttributes(label = "ignore-missing", defaultValue = "true")
   private UIInput<Boolean> ignoremissing;

   @Inject
   @WithAttributes(label = "path", description = "The path to the application to undeploy")
   private UIInput<FileResource> path;

   @Override
   protected String getName()
   {
      return "Undeploy";
   }

   @Override
   protected String getDescription()
   {
      return "Undeploy application to the Application Server";
   }
   
   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      super.initializeUI(builder);
      builder.add(path).add(ignoremissing);
   }

   @Override
   protected Result execute(ApplicationServerProvider provider, UIContext context) throws Exception
   {
      context.getAttributeMap().put("ignore-missing", ignoremissing.getValue());
      context.getAttributeMap().put("path", path.getValue());
      return provider.undeploy(context);
   }

}