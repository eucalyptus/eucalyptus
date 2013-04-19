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
package com.eucalyptus.autoscaling.groups;

import java.util.List;
import com.google.common.collect.ImmutableList;

/**
 * Immutable core view of an auto scaling group.
 */
public class AutoScalingGroupCoreView extends AutoScalingGroupMinimumView {

  private final ImmutableList<String> availabilityZones;
  private final ImmutableList<String> loadBalancerNames;
  private final ImmutableList<GroupScalingCause> scalingCauses;
  private final ImmutableList<SuspendedProcess> suspendedProcesses;

  public AutoScalingGroupCoreView( final AutoScalingGroup group ) {
    super( group );
    this.availabilityZones = ImmutableList.copyOf( group.getAvailabilityZones() );
    this.loadBalancerNames = ImmutableList.copyOf( group.getLoadBalancerNames() );
    this.scalingCauses = ImmutableList.copyOf( group.getScalingCauses() );
    this.suspendedProcesses = ImmutableList.copyOf( group.getSuspendedProcesses() );
  }

  public List<String> getAvailabilityZones() {
    return availabilityZones;
  }

  public List<GroupScalingCause> getScalingCauses() {
    return scalingCauses;
  }

  public List<SuspendedProcess> getSuspendedProcesses() {
    return suspendedProcesses;
  }

  public List<String> getLoadBalancerNames() {
    return loadBalancerNames;
  }
}
