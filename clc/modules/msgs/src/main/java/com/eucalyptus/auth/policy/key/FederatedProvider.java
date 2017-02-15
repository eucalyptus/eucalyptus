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

  @Override
  public boolean canApply( final String action ) {
    return true;
  }
}
