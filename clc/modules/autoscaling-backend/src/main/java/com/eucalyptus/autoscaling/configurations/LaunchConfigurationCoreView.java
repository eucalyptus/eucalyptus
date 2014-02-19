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

import java.util.List;
import com.google.common.collect.ImmutableList;

/**
 * Immutable core view of a launch configuration.
 */
public class LaunchConfigurationCoreView extends LaunchConfigurationMinimumView {

  private final ImmutableList<BlockDeviceMapping> blockDeviceMappings;
  private final ImmutableList<String> securityGroups;

  public LaunchConfigurationCoreView( final LaunchConfiguration launchConfiguration ) {
    super( launchConfiguration );
    this.blockDeviceMappings = ImmutableList.copyOf( launchConfiguration.getBlockDeviceMappings() );
    this.securityGroups = ImmutableList.copyOf( launchConfiguration.getSecurityGroups() );
  }

  public List<String> getSecurityGroups() {
    return securityGroups;
  }

  public List<BlockDeviceMapping> getBlockDeviceMappings() {
    return blockDeviceMappings;
  }
}
