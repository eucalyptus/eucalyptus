/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_RUNINSTANCES;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.VENDOR_EC2;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ArnConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( LaunchTemplateKey.KEY_NAME )
public class LaunchTemplateKey  implements ComputeKey {
  static final String KEY_NAME = "ec2:launchtemplate";
  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    return Allocation.current( ).map( Allocation::getLaunchTemplateArn ).getOrNull( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !ArnConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". ARN conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertArnValue( value );
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }
}

