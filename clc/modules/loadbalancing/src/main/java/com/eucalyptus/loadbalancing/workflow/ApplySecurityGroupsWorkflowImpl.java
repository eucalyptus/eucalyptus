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

import java.util.Map;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class ApplySecurityGroupsWorkflowImpl
    implements ApplySecurityGroupsWorkflow {

  /* (non-Javadoc)
   * @see com.eucalyptus.loadbalancing.workflow.ApplySecurityGroupsWorkflow#createLoadBalancer(java.lang.String, java.lang.String, java.util.Map)
   */
  @Override
  public void applySecurityGroups(String accountId, String loadbalancer,
      Map<String, String> groupIdToNameMap) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see com.eucalyptus.loadbalancing.workflow.ApplySecurityGroupsWorkflow#getState()
   */
  @Override
  public ElbWorkflowState getState() {
    // TODO Auto-generated method stub
    return null;
  }

}
