/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.EC2_RUNINSTANCES;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_EC2;
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ArnConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( SubnetKey.KEY_NAME )
public class SubnetKey implements ComputeKey {
  static final String KEY_NAME = "ec2:subnet";
  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    return ComputePolicyContext.getSubnetArn( );
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
  public boolean canApply( final String action,
                           final String resourceType ) {
    return actions.contains( action );
  }
}
