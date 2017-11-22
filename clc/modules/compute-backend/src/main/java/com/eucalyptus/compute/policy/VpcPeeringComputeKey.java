/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
