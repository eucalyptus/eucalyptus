/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.policy;

import static com.eucalyptus.auth.euare.policy.EuarePolicyContext.EuarePolicyContextResource;
import java.util.Set;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.Sets;

/**
 *
 */
public class EuarePolicyResourceInterceptor implements RestrictedTypes.PolicyResourceInterceptor {
  private static final Set<Class<? extends RestrictedType>> accepted = Sets.newCopyOnWriteArraySet();
  private static final Set<Class<? extends RestrictedType>> rejected = Sets.newCopyOnWriteArraySet( );

  @Override
  public void onResource( final RestrictedType resource, final String action ) {
    EuarePolicyContext.clearContext( );

    if ( resource != null ) {
      if ( accepted.contains( resource.getClass( ) ) ||
          (!rejected.contains( resource.getClass( ) ) &&
              Account.class.isAssignableFrom( resource.getClass( ) ) ) ) try {
        EuarePolicyContext.setEuarePolicyContextResource(
            TypeMappers.transform( resource, EuarePolicyContextResource.class ) );
        accepted.add( resource.getClass() );
      } catch ( IllegalArgumentException e ) {
        rejected.add( resource.getClass() );
        Logs.exhaust().info(
            "Policy context not set for resource type: " + resource.getClass().getSimpleName( ) );
      }
    }
  }
}
