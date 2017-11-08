/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.workflow;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntity;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowClientFactory;
import com.netflix.glisten.WorkflowDescriptionTemplate;

/**
 * Created by ethomas on 4/11/16.
 */
public class UpdateStackPartsWorkflowKickOff {

  public static void kickOffUpdateCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getStackName(), stackUpdateInfoEntity.getAccountId(), stackUpdateInfoEntity.getAccountAlias());
    StartTimeoutPassableWorkflowClientFactory updateCleanupStackWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateCleanupStackWorkflowDescriptionTemplate = new UpdateCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateCleanupStackWorkflow> updateCleanupStackWorkflowClient = updateCleanupStackWorkflowClientFactory
      .getNewWorkflowClient(UpdateCleanupStackWorkflow.class, updateCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateCleanupStackWorkflow updateCleanupStackWorkflow = new UpdateCleanupStackWorkflowClient(updateCleanupStackWorkflowClient);
    updateCleanupStackWorkflow.performUpdateCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getOldResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion());
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateCleanupStackWorkflowClient.getWorkflowExecution().getRunId());
  }

  public static void kickOffUpdateRollbackCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getStackName(), stackUpdateInfoEntity.getAccountId(), stackUpdateInfoEntity.getAccountAlias());
    StartTimeoutPassableWorkflowClientFactory updateRollbackCleanupStackWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateRollbackCleanupStackWorkflowDescriptionTemplate = new UpdateRollbackCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateRollbackCleanupStackWorkflow> updateRollbackCleanupStackWorkflowClient = updateRollbackCleanupStackWorkflowClientFactory
      .getNewWorkflowClient(UpdateRollbackCleanupStackWorkflow.class, updateRollbackCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateRollbackCleanupStackWorkflow updateRollbackCleanupStackWorkflow = new UpdateRollbackCleanupStackWorkflowClient(updateRollbackCleanupStackWorkflowClient);
    updateRollbackCleanupStackWorkflow.performUpdateRollbackCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getRunId());
  }

  public static void kickOffUpdateRollbackStackWorkflow(String stackId, String accountId, String outerStackArn, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getStackName(), stackUpdateInfoEntity.getAccountId(), stackUpdateInfoEntity.getAccountAlias());
    StartTimeoutPassableWorkflowClientFactory updateRollbackStackWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateRollbackStackWorkflowDescriptionTemplate = new UpdateRollbackStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateRollbackStackWorkflow> updateRollbackStackWorkflowClient = updateRollbackStackWorkflowClientFactory
      .getNewWorkflowClient(UpdateRollbackStackWorkflow.class, updateRollbackStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateRollbackStackWorkflow updateRollbackStackWorkflow = new UpdateRollbackStackWorkflowClient(updateRollbackStackWorkflowClient);
    updateRollbackStackWorkflow.performUpdateRollbackStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      outerStackArn, stackUpdateInfoEntity.getOldResourceDependencyManagerJson(),
      effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackStackWorkflowClient.getWorkflowExecution().getRunId());
  }
  
  
}
