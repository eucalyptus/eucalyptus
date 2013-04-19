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
package com.eucalyptus.autoscaling.instances;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingInstanceMetadata;
import com.eucalyptus.util.OwnerFullName;

/**
 * Immutable core view of an auto scaling instance.
 */
public class AutoScalingInstanceCoreView implements AutoScalingInstanceMetadata {

  private final AutoScalingInstance instance;

  public AutoScalingInstanceCoreView( final AutoScalingInstance instance ) {
    this.instance = instance;
  }

  public String getInstanceId() {
    return instance.getInstanceId();
  }

  @Override
  public String getDisplayName() {
    return getInstanceId();
  }

  public ConfigurationState getConfigurationState() {
    return instance.getConfigurationState();
  }

  public LifecycleState getLifecycleState() {
    return instance.getLifecycleState();
  }

  public String getOwnerAccountNumber() {
    return instance.getOwnerAccountNumber();
  }

  public String getAvailabilityZone() {
    return instance.getAvailabilityZone();
  }

  public Integer getRegistrationAttempts() {
    return instance.getRegistrationAttempts();
  }

  public HealthStatus getHealthStatus() {
    return instance.getHealthStatus();
  }

  public String getLaunchConfigurationName() {
    return instance.getLaunchConfigurationName();
  }

  @Override
  public OwnerFullName getOwner() {
    return instance.getOwner();
  }

  public long getCreationTimestamp() {
    return instance.getCreationTimestamp().getTime();
  }
}
