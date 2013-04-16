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
package com.eucalyptus.autoscaling.configurations;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.LaunchConfigurationMetadata;
import com.eucalyptus.util.OwnerFullName;

/**
 * Immutable minimum view of a launch configuration.
 */
public class LaunchConfigurationMinimumView implements LaunchConfigurationMetadata {

  private final LaunchConfiguration launchConfiguration;

  public LaunchConfigurationMinimumView( final LaunchConfiguration launchConfiguration ) {
    this.launchConfiguration = launchConfiguration;
  }

  public String getLaunchConfigurationName() {
    return launchConfiguration.getLaunchConfigurationName();
  }

  @Override
  public String getDisplayName() {
    return getLaunchConfigurationName();
  }

  @Override
  public String getArn() {
    return launchConfiguration.getArn();
  }

  public String getIamInstanceProfile() {
    return launchConfiguration.getIamInstanceProfile();
  }

  public String getImageId() {
    return launchConfiguration.getImageId();
  }

  public Boolean getInstanceMonitoring() {
    return launchConfiguration.getInstanceMonitoring();
  }

  public String getInstanceType() {
    return launchConfiguration.getInstanceType();
  }

  public String getKernelId() {
    return launchConfiguration.getKernelId();
  }

  public String getKeyName() {
    return launchConfiguration.getKeyName();
  }

  public String getRamdiskId() {
    return launchConfiguration.getRamdiskId();
  }

  public String getUserData() {
    return launchConfiguration.getUserData();
  }

  @Override
  public OwnerFullName getOwner() {
    return launchConfiguration.getOwner();
  }

  public String getOwnerAccountNumber() {
    return launchConfiguration.getOwnerAccountNumber();
  }
}
