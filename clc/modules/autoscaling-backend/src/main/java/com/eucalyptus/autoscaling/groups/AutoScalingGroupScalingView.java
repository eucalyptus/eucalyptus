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
import com.eucalyptus.autoscaling.configurations.LaunchConfigurationCoreView;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class AutoScalingGroupScalingView extends AutoScalingGroupCoreView {

  private final ImmutableList<TerminationPolicyType> terminationPolicies;
  private final LaunchConfigurationCoreView launchConfiguration;

  public AutoScalingGroupScalingView( final AutoScalingGroup group ) {
    super( group );
    terminationPolicies = ImmutableList.copyOf( group.getTerminationPolicies() );
    launchConfiguration = TypeMappers.transform( group.getLaunchConfiguration(), LaunchConfigurationCoreView.class );
  }

  public List<TerminationPolicyType> getTerminationPolicies() {
    return terminationPolicies;
  }

  public LaunchConfigurationCoreView getLaunchConfiguration() {
    return launchConfiguration;
  }
}
