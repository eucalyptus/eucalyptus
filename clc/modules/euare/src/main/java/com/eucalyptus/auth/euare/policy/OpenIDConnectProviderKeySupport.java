/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
  public void validateValueType( final String value ) throws JSONException {
  }

  @Override
  public boolean canApply( final String action ) {
    return true;
  }

  static Optional<RoleWithWebIdSecurityTokenAttributes> getRoleAttributes( ) {
    return RoleSecurityTokenAttributes.fromContext( RoleWithWebIdSecurityTokenAttributes.class );
  }
}
