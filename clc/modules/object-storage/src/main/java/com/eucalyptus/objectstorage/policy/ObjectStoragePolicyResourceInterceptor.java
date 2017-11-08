/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.objectstorage.policy;

import java.util.Set;
import javax.annotation.Nullable;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.policy.ObjectStoragePolicyContext.ObjectStoragePolicyContextResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Sets;

/**
 *
 */
public class ObjectStoragePolicyResourceInterceptor implements PolicyResourceContext.PolicyResourceInterceptor {
  private static final Set<Class<? extends RestrictedType>> accepted = Sets.newCopyOnWriteArraySet( );
  private static final Set<Class<? extends RestrictedType>> rejected = Sets.newCopyOnWriteArraySet( );

  @SuppressWarnings( "unchecked" )
  @Override
  public void onResource( @Nullable final PolicyResourceContext.PolicyResourceInfo resource,
                          @Nullable final String action ) {
    ObjectStoragePolicyContext.clearContext( );

    if ( resource != null &&
        resource.getResourceObject() != null &&
        S3AccessControlledEntity.class.isAssignableFrom( resource.getResourceClass( ) ) ) {
      if ( accepted.contains( resource.getResourceClass( ) ) ||
          (!rejected.contains( resource.getResourceClass( ) ) &&
              S3AccessControlledEntity.class.isAssignableFrom( resource.getResourceClass( ) ) ) ) try {
        ObjectStoragePolicyContext.setObjectStoragePolicyContextResource(
            TypeMappers.transform( resource.getResourceObject( ), ObjectStoragePolicyContextResource.class ) );
        accepted.add( (Class<? extends RestrictedType>) resource.getResourceClass( ) );
      } catch ( IllegalArgumentException e ) {
        rejected.add( (Class<? extends RestrictedType>) resource.getResourceClass( ) );
        Logs.exhaust( ).info(
            "Policy context not set for resource type: " + resource.getResourceClass().getSimpleName( ) );
      }
    }
  }
}
