/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.compute.policy.ComputePolicyContext.ComputePolicyContextResource;
import static com.eucalyptus.util.RestrictedTypes.PolicyResourceInterceptor;
import java.util.Set;
import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Sets;

/**
 *
 */
public class ComputePolicyResourceInterceptor implements PolicyResourceInterceptor {
  private static final Set<Class<? extends RestrictedType>> accepted = Sets.newCopyOnWriteArraySet( );
  private static final Set<Class<? extends RestrictedType>> rejected = Sets.newCopyOnWriteArraySet( );

  @Override
  public void onResource( final RestrictedType resource, final String action ) {
    ComputePolicyContext.clearContext( );

    if ( accepted.contains( resource.getClass() ) ||
        (!rejected.contains( resource.getClass() ) &&
          CloudMetadata.class.isAssignableFrom( resource.getClass( ) ) ) ) try {
      ComputePolicyContext.setComputePolicyContextResource(
          TypeMappers.transform( resource, ComputePolicyContextResource.class ) );
      accepted.add( resource.getClass() );
    } catch ( IllegalArgumentException e ) {
      rejected.add( resource.getClass() );
      Logs.exhaust( ).info(
          "Policy context not set for resource type: " + resource.getClass().getSimpleName( ) );
    }
  }
}
