/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.policy;

import static com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.loadbalancing.common.policy.LoadBalancingPolicySpec;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( LoadBalancerNumberQuotaKey.KEY )
public class LoadBalancerNumberQuotaKey extends QuotaKey {

  public static final String KEY = "elasticloadbalancing:quota-loadbalancernumber";

  @Override
  public final void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public final boolean canApply( String action ) {
    return PolicySpec.qualifiedName(
        LoadBalancingPolicySpec.VENDOR_LOADBALANCING,
        LoadBalancingPolicySpec.LOADBALANCING_CREATELOADBALANCER ).equals( action );
  }

  @Override
  public final String value( final PolicyScope scope,
                             final String id,
                             final String resource,
                             final Long quantity ) throws AuthException {
    final OwnerFullName name;
    switch ( scope ) {
      case Account:
        name = AccountFullName.getInstance( id );
        break;
      case Group:
        return NOT_SUPPORTED;
      case User:
        name = UserFullName.getInstance( id );
        break;
      default:
        throw new AuthException( "Invalid scope" );
    }
    return Long.toString(
        RestrictedTypes.quantityMetricFunction( LoadBalancerMetadata.class ).apply( name ) +
            quantity );
  }
}
