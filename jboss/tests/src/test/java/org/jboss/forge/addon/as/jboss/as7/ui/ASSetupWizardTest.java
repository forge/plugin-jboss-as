/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.as7.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.as.ui.ASSetupWizard;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.command.AbstractCommandExecutionListener;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing AS Setup Wizard for JBoss AS7.
 * 
 * @author Jeremie Lagarde
 */
@RunWith(Arquillian.class)
public class ASSetupWizardTest
{
   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.addon:ui"),
            @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
            @AddonDependency(name = "org.jboss.forge.addon:as"),
            @AddonDependency(name = "org.jboss.forge.addon:as-jboss-as7"),
            @AddonDependency(name = "org.jboss.forge.addon:as-jboss-wf8"),
            @AddonDependency(name = "org.jboss.forge.addon:maven")
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:configuration"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:as"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:as-jboss-as7"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:as-jboss-wf8"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness")
               );
   }

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private UITestHarness testHarness;

   @Test
   public void testSetupDefault() throws Exception
   {
      final Project project = projectFactory.createTempProject();
      try (WizardCommandController tester = testHarness.createWizardController(ASSetupWizard.class,
               project.getRootDirectory()))
      {
         // Launch
         tester.initialize();

         Assert.assertTrue(tester.isValid());
         tester.setValueFor("server", "jbossas7");

         Assert.assertTrue(tester.canMoveToNextStep());
         tester.next().initialize();
         Assert.assertFalse(tester.canMoveToNextStep());

         final AtomicBoolean flag = new AtomicBoolean();
         tester.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener()
         {
            @Override
            public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result)
            {
               if (result.getMessage() != null
                        && result.getMessage().equals("The applicaion server was setup successfully."))
               {
                  flag.set(true);
               }
            }
         });
         tester.execute();
         // Ensure that the application server is installed
         Assert.assertEquals(true, flag.get());
      }
   }
}