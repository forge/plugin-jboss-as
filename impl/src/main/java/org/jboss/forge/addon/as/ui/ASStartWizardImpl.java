/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.ui;

import org.jboss.forge.addon.as.spi.ApplicationServerProvider;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.result.Result;

/**
 * AS Setup Wizard Implementation.
 * 
 * @author Jeremie Lagarde
 */
public class ASStartWizardImpl extends AbstractASWizardImpl implements ASStartWizard
{

   @Override
   protected String getName()
   {
      return "Start";
   }

   @Override
   protected String getDescription()
   {
      return "Start the Application Server";
   }

   @Override
   protected Result execute(ApplicationServerProvider provider, UIContext context) throws Exception
   {
      return provider.start(context);
   }
}