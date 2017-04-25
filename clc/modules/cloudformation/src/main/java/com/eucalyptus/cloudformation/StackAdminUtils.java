/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudformation;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.RequestCancelWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.UnknownResourceException;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloudformation.bootstrap.CloudFormationAWSCredentialsProvider;
import com.eucalyptus.cloudformation.entity.SignalEntityManager;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventHelper;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.entity.StacksWithNoUpdateToPerformEntityManager;
import com.eucalyptus.cloudformation.entity.Status;
import com.eucalyptus.cloudformation.workflow.WorkflowClientManager;
import com.eucalyptus.simpleworkflow.common.client.Config;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 4/21/17.
 */
public class StackAdminUtils {

  private final static Logger LOG = Logger.getLogger(StackAdminUtils.class);

  public static void removeStack(String stackId) throws CloudFormationException, AuthException {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId);
    if (stackEntity == null) {
      throw new ValidationErrorException("Can not find undeleted stack " + stackId);
    }
    String stackAccountId = stackEntity.getAccountId();
    cancelWorkflows(stackId);
    StackWorkflowEntityManager.deleteStackWorkflowEntities(stackId);
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, stackAccountId);
    // increase stack version to consolidate stack
    StackResourceEntityManager.flattenResources(stackId, stackAccountId, stackEntity.getStackVersion());
    StackEntityManager.reallyDeleteAllStackVersionsExcept(stackId, stackAccountId, stackEntity.getStackVersion());
    for (StackResourceEntity stackResourceEntity : StackResourceEntityManager.describeStackResources(stackAccountId, stackId)) {
      if (stackResourceEntity.getResourceStatus() != Status.DELETE_COMPLETE && stackResourceEntity.getResourceStatus() != Status.DELETE_SKIPPED) {
        stackResourceEntity.setResourceStatus(Status.DELETE_SKIPPED);
        stackResourceEntity.setResourceStatusReason("Forced delete immediately, skipping resources");
        StackResourceEntityManager.updateStackResource(stackResourceEntity);
        StackEventEntityManager.addStackEvent(stackResourceEntity);
      }
    }
    StackEventHelper.createGlobalStackEvent(stackId, stackAccountId, Status.DELETE_COMPLETE.toString(), "Forced delete immediately", stackEntity.getStackVersion());
    StackResourceEntityManager.deleteStackResources(stackId, stackAccountId);
    StackEventEntityManager.deleteStackEvents(stackId, stackAccountId);
    StackEntityManager.deleteStack(stackId, stackAccountId);
    StackWorkflowEntityManager.deleteStackWorkflowEntities(stackId);
    StackUpdateInfoEntityManager.deleteStackUpdateInfo(stackId, stackAccountId);
    StacksWithNoUpdateToPerformEntityManager.deleteStackWithNoUpdateToPerform(stackId, stackAccountId);
    SignalEntityManager.deleteSignals(stackId, stackAccountId);
  }

  public static void cancelWorkflows(String stackId) throws CloudFormationException, AuthException {
    StackEntity stackEntity = StackEntityManager.getNonDeletedStackById(stackId);
    if (stackEntity == null) {
      throw new ValidationErrorException("Can not find undeleted stack " + stackId);
    }
    String stackAccountId = stackEntity.getAccountId();
    AmazonSimpleWorkflow simpleWorkflowClient = Config.buildClient(
      CloudFormationAWSCredentialsProvider.CloudFormationUserSupplier.INSTANCE
    );
    try {
      // first cancel all outstanding workflows
      for (StackWorkflowEntity stackWorkflowEntity : StackWorkflowEntityManager.getStackWorkflowEntities(stackId)) {
        try {
          RequestCancelWorkflowExecutionRequest requestCancelWorkflowExecutionRequest = new RequestCancelWorkflowExecutionRequest();
          requestCancelWorkflowExecutionRequest.setWorkflowId(stackWorkflowEntity.getWorkflowId());
          requestCancelWorkflowExecutionRequest.setRunId(stackWorkflowEntity.getRunId());
          requestCancelWorkflowExecutionRequest.setDomain(stackWorkflowEntity.getDomain());
          simpleWorkflowClient.requestCancelWorkflowExecution(requestCancelWorkflowExecutionRequest);
        } catch (UnknownResourceException ex) {
          ; // don't bother
        }
      }
    } finally {
      simpleWorkflowClient.shutdown();
    }
  }
}

