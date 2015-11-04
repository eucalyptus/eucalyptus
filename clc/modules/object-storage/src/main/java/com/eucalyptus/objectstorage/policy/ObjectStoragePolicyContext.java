/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.objectstorage.policy;

import javax.annotation.Nullable;

/**
 *
 */
public class ObjectStoragePolicyContext {

  private final static ThreadLocal<ObjectStoragePolicyContextResource> resourceLocal = new ThreadLocal<>();

  static void clearContext( ) {
    resourceLocal.set( null );
  }

  static void setObjectStoragePolicyContextResource( @Nullable final ObjectStoragePolicyContextResource resource ) {
    resourceLocal.set( resource );
  }

  static String getVersionId( ) {
    final ObjectStoragePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVersionId( );
  }

  public interface ObjectStoragePolicyContextResource {
    @Nullable
    String getVersionId( );
  }

  public static class ObjectStoragePolicyContextResourceSupport implements ObjectStoragePolicyContextResource {
    @Nullable
    @Override
    public String getVersionId( ) {
      return null;
    }
  }
}
