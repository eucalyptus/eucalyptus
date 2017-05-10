/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_ACCEPTVPCPEERINGCONNECTION;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_CREATEVPCPEERINGCONNECTION;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_DELETEVPCPEERINGCONNECTION;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_REJECTVPCPEERINGCONNECTION;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.VENDOR_EC2;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
public abstract class VpcPeeringComputeKey implements ComputeKey {

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_EC2, EC2_ACCEPTVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATEVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REJECTVPCPEERINGCONNECTION ) )
      .build( );

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertArnValue( value );
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }
}
