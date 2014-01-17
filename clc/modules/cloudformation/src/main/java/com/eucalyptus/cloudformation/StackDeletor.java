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
package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.AWSEC2Instance;
import com.eucalyptus.cloudformation.resources.Resource;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackDeletor extends Thread {
  private static final Logger LOG = Logger.getLogger(StackDeletor.class);
  private Stack stack;
  private String effectiveUserId;
    private String accountId;

  public StackDeletor(Stack stack, String effectiveUserId, String accountId) {
    this.stack = stack;
    this.effectiveUserId = effectiveUserId;
      this.accountId = accountId;
  }
  @Override
  public void run() {
    try {
      LOG.info("stackName=" + stack.getStackName());
      for (StackResourceEntity stackResourceEntity: StackResourceEntityManager.getStackResources(stack.getStackName(), accountId)) {
        Resource resource = null;
        LOG.info("resourceType="+stackResourceEntity.getResourceType());
        LOG.info("physicalResourceId="+stackResourceEntity.getPhysicalResourceId());
        if (stackResourceEntity.getResourceType().equals("AWS::EC2::Instance")) {
          LOG.info("It's an instance!");
          AWSEC2Instance awsec2Instance = new AWSEC2Instance();
          awsec2Instance.setEffectiveUserId(effectiveUserId);
          awsec2Instance.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
          awsec2Instance.setType(stackResourceEntity.getResourceType());
          awsec2Instance.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
          resource = awsec2Instance;
        }
        try {
          resource.delete();
        } catch (Throwable ex) {
          LOG.error(ex, ex);
        }
      }
      StackResourceEntityManager.deleteStackResources(stack.getStackName(), accountId);
      StackEventEntityManager.deleteStackEvents(stack.getStackName(), accountId);
      StackEntityManager.deleteStack(stack.getStackName(), accountId);
    } catch (Throwable ex) {
      LOG.error(ex, ex);
    }
  }
}

