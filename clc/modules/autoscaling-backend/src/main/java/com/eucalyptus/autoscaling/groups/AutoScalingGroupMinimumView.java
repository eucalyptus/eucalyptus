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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.activities.ActivityCause;
import com.eucalyptus.autoscaling.activities.ScalingActivity;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 * Immutable minimum view of an auto scaling group.
 */
public class AutoScalingGroupMinimumView implements AutoScalingGroupMetadata {

  protected final AutoScalingGroup group;

  public AutoScalingGroupMinimumView( final AutoScalingGroup group ) {
    this.group = group;
  }

  public String getAutoScalingGroupName() {
    return group.getAutoScalingGroupName();
  }

  @Override
  public String getDisplayName() {
    return getAutoScalingGroupName();
  }

  @Override
  public String getArn() {
    return group.getArn();
  }

  public Integer getDefaultCooldown() {
    return group.getDefaultCooldown();
  }

  public Boolean getScalingRequired() {
    return group.getScalingRequired();
  }

  public HealthCheckType getHealthCheckType() {
    return group.getHealthCheckType();
  }

  @Override
  public OwnerFullName getOwner() {
    return group.getOwner();
  }

  public String getOwnerAccountNumber() {
    return group.getOwnerAccountNumber();
  }

  public Integer getMaxSize() {
    return group.getMaxSize();
  }

  public Integer getMinSize() {
    return group.getMinSize();
  }

  public Integer getCapacity() {
    return group.getCapacity();
  }

  public Integer getHealthCheckGracePeriod() {
    return group.getHealthCheckGracePeriod();
  }

  public Integer getDesiredCapacity() {
    return group.getDesiredCapacity();
  }

  public long getCreationTimestamp() {
    return group.getCreationTimestamp().getTime();
  }

  public long getLastUpdateTimestamp() {
    return group.getLastUpdateTimestamp().getTime();
  }

  public Integer getVersion() {
    return group.getVersion();
  }

  public AutoScalingInstance createInstance( final String instanceId,
                                             final String availabilityZone ) {
    return AutoScalingInstance.create( getOwner(), instanceId, availabilityZone, group );
  }

  public ScalingActivity createActivity( final String clientToken, final List<ActivityCause> causes ) {
    return ScalingActivity.create( group, clientToken, causes );
  }
}
