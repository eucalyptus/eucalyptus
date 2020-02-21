/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.route53.common.Route53Metadata;
import com.eucalyptus.route53.common.policy.Route53PolicySpec;
import com.eucalyptus.util.RestrictedTypes;
import net.sf.json.JSONException;

/**
 *
 */
abstract class Route53NumberQuotaKeySupport<T extends Route53Metadata> extends QuotaKey {

  private final String key;
  private final String action;
  private final Class<T> metadataClass;

  Route53NumberQuotaKeySupport( final String key,
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
  public final boolean canApply( String action ) {
    return PolicySpec.qualifiedName(
        Route53PolicySpec.VENDOR_ROUTE53,
        this.action ).equals( action );
  }

  @Override
  public final String value( final PolicyScope scope,
                             final String id,
                             final String resource,
                             final Long quantity ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( RestrictedTypes.quantityMetricFunction( metadataClass )
            .apply( AccountFullName.getInstance( id ) ) + 1 );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( RestrictedTypes.quantityMetricFunction( metadataClass )
            .apply( UserFullName.getInstance( id ) ) + 1 );
    }
    throw new AuthException( "Invalid scope" );
  }
}
