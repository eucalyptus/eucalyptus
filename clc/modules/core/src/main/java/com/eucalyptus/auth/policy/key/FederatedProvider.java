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
package com.eucalyptus.auth.policy.key;


import static com.eucalyptus.auth.policy.key.Key.EvaluationConstraint.ReceivingHost;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.HasRole;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.tokens.RoleSecurityTokenAttributes;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.google.common.base.Optional;
import net.sf.json.JSONException;

/**
 *
 */
@SuppressWarnings( "Guava" )
@PolicyKey( value = Keys.AWS_FEDERATED_PROVIDER, evaluationConstraints = ReceivingHost )
public class FederatedProvider implements AwsKey {
  static final String KEY = Keys.AWS_FEDERATED_PROVIDER;

  @SuppressWarnings( "ConstantConditions" )
  @Override
  public String value( ) throws AuthException {
    try {
      final Context context = Contexts.lookup( );
      final UserPrincipal principal = context.getUser( );
      if ( principal instanceof HasRole && ((HasRole) principal).getRole( ) != null ) {
        final Role role = ( (HasRole) principal ).getRole( );
        final List<AccessKey> keys = principal.getKeys( );
        if ( keys.size( ) == 1 ) {
          final Optional<RoleSecurityTokenAttributes> attributes = RoleSecurityTokenAttributes.forKey( keys.get( 0 ) );
          if ( attributes.isPresent( ) && attributes.get( ) instanceof RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes ) {
            final RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes webIdAttributes =
                (RoleSecurityTokenAttributes.RoleWithWebIdSecurityTokenAttributes) attributes.get( );
            return new EuareResourceName(
                role.getAccountNumber( ),
                PolicySpec.IAM_RESOURCE_OPENID_CONNECT_PROVIDER,
                "/",
                webIdAttributes.getProviderUrl( ) ).toString( )
            ;
          }
        }
      }
    } catch ( final IllegalContextAccessException e ) {
      // null
    }
    return null;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }
}
