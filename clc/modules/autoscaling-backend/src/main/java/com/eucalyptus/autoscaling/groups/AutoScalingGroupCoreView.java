/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
