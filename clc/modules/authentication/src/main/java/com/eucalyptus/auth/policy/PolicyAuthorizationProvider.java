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
package com.eucalyptus.auth.policy;

import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Lists;

/**
 * Authorization provider backed by IAM policies.
 */
public class PolicyAuthorizationProvider extends AuthorizationProviderSupport {

  @Override
  public List<Authorization> lookupQuotas( final Account account,
                                           final User user,
                                           final String resourceType ) throws AuthException {
    final List<Authorization> results = Lists.newArrayList( );
    results.addAll( account.lookupAccountGlobalQuotas( resourceType ) );
    if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
      results.addAll( account.lookupAccountGlobalQuotas( PolicySpec.ALL_RESOURCE ) );
    }
    if ( !user.isAccountAdmin( ) ) {
      results.addAll( user.lookupQuotas( resourceType ) );
      if ( !PolicySpec.ALL_RESOURCE.equals( resourceType ) ) {
        results.addAll( user.lookupQuotas( PolicySpec.ALL_RESOURCE ) );
      }
    }
    return results;
  }
}
