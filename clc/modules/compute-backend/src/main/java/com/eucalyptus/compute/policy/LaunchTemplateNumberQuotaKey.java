/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.policy;

import java.util.function.Supplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.policy.ComputePolicySpec;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( LaunchTemplateNumberQuotaKey.KEY )
public class LaunchTemplateNumberQuotaKey extends QuotaKey {

  public static final String KEY = "ec2:quota-launchtemplatenumber";

  @Override
  public void validateValueType( final String value ) throws JSONException {
    KeyUtils.validateIntegerValue( value, KEY );
  }

  @Override
  public boolean canApply( final String action ) {
    return PolicySpec.qualifiedName(
        ComputePolicySpec.VENDOR_EC2,
        ComputePolicySpec.EC2_CREATELAUNCHTEMPLATE ).equals( action );
  }

  @Override
  public String value( final PolicyScope scope,
                       final String id,
                       final String resource,
                       final Long quantity ) throws AuthException {
    final Supplier<Function<OwnerFullName, Long>> qmf =
        () -> RestrictedTypes.quantityMetricFunction( CloudMetadata.LaunchTemplateMetadata.class );
    switch ( scope ) {
      case Account:
        return Long.toString( qmf.get().apply( AccountFullName.getInstance( id ) ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( qmf.get().apply( UserFullName.getInstance( id ) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }
}
