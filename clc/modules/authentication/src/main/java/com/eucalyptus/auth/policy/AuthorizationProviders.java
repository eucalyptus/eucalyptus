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
import java.util.concurrent.CopyOnWriteArrayList;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Lists;

/**
 *
 */
class AuthorizationProviders {

  private static CopyOnWriteArrayList<AuthorizationProvider> providers = new CopyOnWriteArrayList<>( );

  static List<Authorization> lookupQuotas(
      final Account account,
      final User user,
      final String resourceType
  ) throws AuthException {
    final List<Authorization> authorizations = Lists.newArrayList( );
    for ( final AuthorizationProvider provider : providers ) {
      authorizations.addAll( provider.lookupQuotas( account, user, resourceType ) );
    }
    return authorizations;
  }

  static void register( final AuthorizationProvider provider ) {
    providers.add( provider );
  }

}
