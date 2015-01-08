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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.auth.principal.User;

/**
 *
 */
public class AuthorizationProviderSupport implements AuthorizationProvider {

  @Override
  public List<Authorization> lookupQuotas( final Account account,
                                           final User user,
                                           final String resourceType ) throws AuthException {
    return Collections.emptyList( );
  }

  public static final class QuotaCondition implements Condition {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final String value;

    public QuotaCondition( final String key,
                           final String value ) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String getType( ) {
      return Conditions.NUMERICLESSTHANEQUALS;
    }

    @Override
    public String getKey( ) {
      return key;
    }

    @Override
    public Set<String> getValues( ) throws AuthException {
      return Collections.singleton( value );
    }
  }

  public static final class QuotaAuthorization implements Authorization {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String userId;
    private final String resourceType;
    private final Condition condition;

    public QuotaAuthorization( final String accountNumber,
                               final String userId,
                               final String resourceType,
                               final Condition condition ) {
      this.accountNumber = accountNumber;
      this.userId = userId;
      this.resourceType = resourceType;
      this.condition = condition;
    }

    @Override
    public EffectType getEffect( ) {
      return EffectType.Limit;
    }

    @Override
    public String getAccount( ) {
      return accountNumber;
    }

    @Override
    public String getType( ) {
      return resourceType;
    }

    @Override
    public Boolean isNotAction( ) {
      return Boolean.FALSE;
    }

    @Override
    public Set<String> getActions( ) throws AuthException {
      return Collections.singleton( "*" );
    }

    @Override
    public Boolean isNotResource( ) {
      return Boolean.FALSE;
    }

    @Override
    public Set<String> getResources( ) throws AuthException {
      return Collections.singleton( "*" );
    }

    @Override
    public List<Condition> getConditions( ) throws AuthException {
      return Collections.singletonList( condition );
    }

    @Override
    public Scope getScope( ) throws AuthException {
      return Scope.USER;
    }

    @Override
    public String getScopeId( ) throws AuthException {
      return userId;
    }

    @Override
    public Principal getPrincipal( ) {
      return null;
    }
  }
}
