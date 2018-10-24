/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
