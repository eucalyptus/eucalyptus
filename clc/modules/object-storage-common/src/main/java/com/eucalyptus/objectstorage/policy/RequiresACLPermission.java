/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;

/**
 * Declares the set of ACL permissions required to execute the request. Setting ownerOnly declares the operation to be only executable by the resource
 * owner account. ownerOnly is typically used for S3 operations that don't have a corresponding ACL operation. IAM evaluation for users within the
 * owning account must still be performed
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresACLPermission {
  /**
   * The set of ACL permissions, from {@link ObjectStorageProperties.Permission} that
   */
  ObjectStorageProperties.Permission[] bucket();

  ObjectStorageProperties.Permission[] object();

  ObjectStorageProperties.Resource[] ownerOf() default {};

}
