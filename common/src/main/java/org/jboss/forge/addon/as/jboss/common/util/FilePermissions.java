/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.util;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FilePermissions
{

   private final FilePermission owner;
   private final FilePermission group;
   private final FilePermission pub;
   private final String string;

   public FilePermissions(final FilePermission owner, final FilePermission group, final FilePermission pub)
   {
      this.owner = owner;
      this.group = group;
      this.pub = pub;
      string = String.format("%s%s%s", owner, group, pub);
   }

   public static FilePermissions of(final int mode)
   {
      int i = mode;
      int charPos = 3;
      int[] parts = new int[charPos];
      do
      {
         parts[--charPos] = (i & 7);
         i >>>= 3;
         // Only need the 3 parts
      }
      while (charPos > 0 && i != 0);
      return new FilePermissions(FilePermission.fromInt(parts[0]), FilePermission.fromInt(parts[1]),
               FilePermission.fromInt(parts[2]));
   }

   public FilePermission owner()
   {
      return owner;
   }

   public FilePermission group()
   {
      return group;
   }

   public FilePermission pub()
   {
      return pub;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int hash = 17;
      hash = prime * hash + (owner == null ? 0 : owner.hashCode());
      hash = prime * hash + (group == null ? 0 : group.hashCode());
      hash = prime * hash + (pub == null ? 0 : pub.hashCode());
      return hash;
   }

   @Override
   public boolean equals(final Object obj)
   {
      if (obj == this)
      {
         return true;
      }
      if (!(obj instanceof FilePermissions))
      {
         return false;
      }
      final FilePermissions other = (FilePermissions) obj;
      return owner == other.owner && group == other.group && pub == other.pub;
   }

   @Override
   public String toString()
   {
      return string;
   }

   public static enum FilePermission
   {
      NONE(false, false, false),
      EXECUTE(false, false, true),
      WRITE(false, true, false),
      WRITE_EXECUTE(false, true, true),
      READ(true, false, false),
      READ_EXECUTE(true, false, true),
      READ_WRITE(true, true, false),
      READ_WRITE_EXECUTE(true, true, true);

      private final boolean canRead;
      private final boolean canWrite;
      private final boolean canExecute;
      private final String string;

      FilePermission(final boolean canRead, final boolean canWrite, final boolean canExecute)
      {
         this.canRead = canRead;
         this.canWrite = canWrite;
         this.canExecute = canExecute;
         string = String.format("%s%s%s", (canRead ? "r" : "-"), (canWrite ? "w" : "-"), (canExecute ? "x" : "-"));
      }

      static FilePermission fromInt(final int i)
      {
         if (i < 0 || i > 7)
         {
            throw new IllegalStateException(String.format(
                     "The permissions value must be between 0 and 7. Value '%d' is invalid.", i));
         }
         return values()[i];
      }

      public boolean canRead()
      {
         return canRead;
      }

      public boolean canWrite()
      {
         return canWrite;
      }

      public boolean canExecute()
      {
         return canExecute;
      }

      @Override
      public String toString()
      {
         return string;
      }
   }
}
