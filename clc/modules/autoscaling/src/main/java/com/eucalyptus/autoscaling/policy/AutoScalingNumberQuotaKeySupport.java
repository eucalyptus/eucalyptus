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
package com.eucalyptus.autoscaling.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.util.RestrictedTypes;
import net.sf.json.JSONException;

/**
 *
 */
abstract class AutoScalingNumberQuotaKeySupport<T extends AutoScalingMetadata> extends QuotaKey {

  private final String key;
  private final String action;
  private final Class<T> metadataClass;

  AutoScalingNumberQuotaKeySupport( final String key, 
                                    final String action, 
                                    final Class<T> metadataClass ) {
    this.key = key;
    this.action = action;
    this.metadataClass = metadataClass;
  }

  @Override
  public final void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, key );
  }

  @Override
  public final boolean canApply( String action, String resourceType ) {
    return PolicySpec.qualifiedName(
        PolicySpec.VENDOR_AUTOSCALING,
        this.action ).equals( action );
  }

  @Override
  public final String value( final QuotaKey.Scope scope, 
                             final String id, 
                             final String resource, 
                             final Long quantity ) throws AuthException {
    switch ( scope ) {
      case ACCOUNT:
        return Long.toString( RestrictedTypes.quantityMetricFunction( metadataClass )
            .apply( AccountFullName.getInstance( id ) ) + 1 );
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return Long.toString( RestrictedTypes.quantityMetricFunction( metadataClass )
            .apply( UserFullName.getInstance( id ) ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
}
