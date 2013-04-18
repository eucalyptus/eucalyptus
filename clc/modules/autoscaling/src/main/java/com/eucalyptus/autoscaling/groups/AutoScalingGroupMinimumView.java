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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import java.util.List;
import com.eucalyptus.autoscaling.activities.ActivityCause;
import com.eucalyptus.autoscaling.activities.ScalingActivity;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.util.OwnerFullName;

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
