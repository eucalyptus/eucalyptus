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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.*;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_ATTACHVOLUME;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_CREATEVOLUME;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_DELETEVOLUME;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_DETACHVOLUME;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.EC2_RUNINSTANCES;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.VENDOR_EC2;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
public abstract class VolumeComputeKey implements ComputeKey {
  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_EC2, EC2_ATTACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATETAGS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATEVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DETACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .build( );

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }
}
