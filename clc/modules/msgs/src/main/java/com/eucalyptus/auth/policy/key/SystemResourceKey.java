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
package com.eucalyptus.auth.policy.key;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.policy.condition.Bool;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( SystemResourceKey.KEY_NAME )
public class SystemResourceKey implements Key {
  static final String KEY_NAME = "aws:systemresource";

  private static final LoadingCache<String,Boolean> systemAccountCache = CacheBuilder.newBuilder( )
      .expireAfterAccess( 15, TimeUnit.MINUTES )
      .maximumSize( 1000 )
      .build( CacheLoader.from( SystemAccountLookup.INSTANCE ) );

  @Override
  public String value( ) throws AuthException {
    final Optional<String> accountNumberOptional =
        PolicyResourceContext.AccountNumberPolicyResourceInterceptor.getCurrentResourceAccountNumber( );
    if ( accountNumberOptional.isPresent( ) ) try {
      return String.valueOf( systemAccountCache.getUnchecked( accountNumberOptional.get( ) ) );
    } catch ( final RuntimeException e ) {
      throw Exceptions.rethrow( e, AuthException.class );
    }
    throw new AuthException( "Resource account not found" );
  }

  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !Bool.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". Boolean conditions are required." );
    }
  }

  @Override
  public boolean canApply( String action ) {
    return true;
  }

  private enum SystemAccountLookup implements Function<String,Boolean> {
    INSTANCE;

    @Nullable
    @Override
    public Boolean apply( @Nullable final String accountNumber ) {
      try {
        final String alias = Accounts.lookupAccountAliasById( accountNumber );
        return
            Accounts.isSystemAccount( alias ) &&
            !AccountIdentifiers.SYSTEM_ACCOUNT.equals( alias );
      } catch ( AuthException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
