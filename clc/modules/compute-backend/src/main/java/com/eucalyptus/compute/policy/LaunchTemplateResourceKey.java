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
import com.eucalyptus.auth.policy.condition.Bool;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( LaunchTemplateResourceKey.KEY_NAME )
public class LaunchTemplateResourceKey implements ComputeKey {
  static final String KEY_NAME = "ec2:islaunchtemplateresource";

  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .build( );

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

  @Override
  public String value( ) throws AuthException {
    return Allocation.current( )
        .map( Allocation::isLaunchTemplateResource )
        .map( String::valueOf )
        .getOrNull( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !Bool.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". Boolean conditions are required." );
    }
  }
}