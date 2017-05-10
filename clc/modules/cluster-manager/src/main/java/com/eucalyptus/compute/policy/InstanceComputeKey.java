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

import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.*;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
public abstract class InstanceComputeKey implements ComputeKey {

  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( VENDOR_EC2, EC2_ASSOCIATEIAMINSTANCEPROFILE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ATTACHCLASSICLINKVPC ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ATTACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATETAGS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DETACHCLASSICLINKVPC ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DETACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DISASSOCIATEIAMINSTANCEPROFILE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_GETCONSOLESCREENSHOT ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REBOOTINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REPLACEIAMINSTANCEPROFILEASSOCIATION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_STARTINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_STOPINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_TERMINATEINSTANCES ) )
      .build( );

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

}
