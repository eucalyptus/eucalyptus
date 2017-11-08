/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare.policy;

import java.util.List;
import java.util.function.Function;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypedKey;
import com.google.common.base.Optional;
import net.sf.json.JSONException;

/**
 *
 */
@SuppressWarnings( "Guava" )
abstract class OpenIDConnectProviderKeySupport implements Key {

  private final String provider;
  private final String name;

  OpenIDConnectProviderKeySupport( final String name, final String suffix ) {
    if ( !name.endsWith( suffix ) ) throw new IllegalArgumentException( "Invalid name: " + name );
    this.name = name;
    this.provider = name.substring( 0, name.length( ) - suffix.length( ) );
  }

  @Override
  final public String name( ) {
    return name;
  }

  final String getValue(
      final TypedKey<Pair<String,String>> key,
      final Function<RoleWithWebIdSecurityTokenAttributes,String> attributeExtractor
      ) throws AuthException {
    // first lookup explicitly set context value
    final Pair<String,String> providerValuePair = PolicyEvaluationContext.get( ).getAttribute( key );
    if ( providerValuePair != null && providerValuePair.getLeft( ).equals( provider ) ) {
      return providerValuePair.getRight( );
    }
    // second check role attributes of contextual principal
    final Optional<RoleWithWebIdSecurityTokenAttributes> attributes = getRoleAttributes( );
    if ( attributes.isPresent( ) ) {
      if ( attributes.get( ).getProviderUrl( ).equals( provider ) ) {
        return attributeExtractor.apply( attributes.get( ) );
      }
    }
    return null;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( name( ) + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public boolean canApply( final String action ) {
    return true;
  }

  static Optional<RoleWithWebIdSecurityTokenAttributes> getRoleAttributes( ) {
    return RoleSecurityTokenAttributes.fromContext( RoleWithWebIdSecurityTokenAttributes.class );
  }
}
