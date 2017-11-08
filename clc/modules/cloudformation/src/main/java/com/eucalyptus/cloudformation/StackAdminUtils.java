/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

