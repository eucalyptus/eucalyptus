/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.common.FlowConstants;

import static com.amazonaws.services.simpleworkflow.flow.common.FlowConstants.NO_DEFAULT_TASK_LIST;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@Activities(version="1.0")
@ActivityRegistrationOptions(
        defaultTaskHeartbeatTimeoutSeconds = FlowConstants.NONE,
        defaultTaskScheduleToCloseTimeoutSeconds = 120,
        defaultTaskScheduleToStartTimeoutSeconds = 60,
        defaultTaskStartToCloseTimeoutSeconds = 60,
        defaultTaskList = NO_DEFAULT_TASK_LIST)
public interface LoadBalancingVmActivities {
  void setPolicy(String policy) throws LoadBalancingActivityException;
  void setLoadBalancer(String loadbalancer) throws LoadBalancingActivityException;
  String getCloudWatchMetrics() throws LoadBalancingActivityException;
  String getInstanceStatus() throws LoadBalancingActivityException;
}
