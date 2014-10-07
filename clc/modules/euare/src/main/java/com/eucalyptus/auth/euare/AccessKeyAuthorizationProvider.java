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
package com.eucalyptus.auth.euare;

import java.util.List;
import java.util.Objects;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationProperties;
import com.eucalyptus.auth.policy.AuthorizationProviderSupport;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Lists;

/**
 * Authorization provider for IAM access key limit via cloud property.
 */
public class AccessKeyAuthorizationProvider extends AuthorizationProviderSupport {

  @Override
  public List<Authorization> lookupQuotas(
      final Account account,
      final User user,
      final String resourceType
  ) throws AuthException {
    final List<Authorization> authorizations = Lists.newArrayList();
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_IAM, PolicySpec.IAM_RESOURCE_ACCESS_KEY ).equals( resourceType ) ) {
      authorizations.add( new QuotaAuthorization(
          account.getAccountNumber( ),
          user.getUserId( ),
          PolicySpec.IAM_RESOURCE_ACCESS_KEY,
          new QuotaCondition(
              Keys.IAM_QUOTA_ACCESS_KEY_NUMBER_PER_USER,
              Objects.toString( AuthenticationProperties.ACCESS_KEYS_LIMIT, "2" )
          )
      ) );
    }
    return authorizations;
  }



}
