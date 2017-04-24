/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.workflow;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntity;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
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
    StartTimeoutPassableWorkflowClientFactory updateCleanupStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateCleanupStackWorkflowDescriptionTemplate = new UpdateCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateCleanupStackWorkflow> updateCleanupStackWorkflowClient = updateCleanupStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(UpdateCleanupStackWorkflow.class, updateCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateCleanupStackWorkflow updateCleanupStackWorkflow = new UpdateCleanupStackWorkflowClient(updateCleanupStackWorkflowClient);
    updateCleanupStackWorkflow.performUpdateCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getOldResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion());
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateCleanupStackWorkflowClient.getWorkflowExecution().getRunId());


    StartTimeoutPassableWorkflowClientFactory monitorUpdateCleanupStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate monitorUpdateCleanupStackWorkflowDescriptionTemplate = new MonitorUpdateCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<MonitorUpdateCleanupStackWorkflow> monitorUpdateCleanupStackWorkflowClient = monitorUpdateCleanupStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(MonitorUpdateCleanupStackWorkflow.class, monitorUpdateCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);
    MonitorUpdateCleanupStackWorkflow monitorUpdateCleanupStackWorkflow = new MonitorUpdateCleanupStackWorkflowClient(monitorUpdateCleanupStackWorkflowClient);
    monitorUpdateCleanupStackWorkflow.monitorUpdateCleanupStack(stackId,  accountId, stackUpdateInfoEntity.getUpdatedStackVersion());

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_CLEANUP_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorUpdateCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorUpdateCleanupStackWorkflowClient.getWorkflowExecution().getRunId());
  }

  public static void kickOffUpdateRollbackCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getStackName(), stackUpdateInfoEntity.getAccountId(), stackUpdateInfoEntity.getAccountAlias());
    StartTimeoutPassableWorkflowClientFactory updateRollbackCleanupStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateRollbackCleanupStackWorkflowDescriptionTemplate = new UpdateRollbackCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateRollbackCleanupStackWorkflow> updateRollbackCleanupStackWorkflowClient = updateRollbackCleanupStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(UpdateRollbackCleanupStackWorkflow.class, updateRollbackCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateRollbackCleanupStackWorkflow updateRollbackCleanupStackWorkflow = new UpdateRollbackCleanupStackWorkflowClient(updateRollbackCleanupStackWorkflowClient);
    updateRollbackCleanupStackWorkflow.performUpdateRollbackCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getRunId());

    StartTimeoutPassableWorkflowClientFactory monitorUpdateRollbackCleanupStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate monitorUpdateRollbackCleanupStackWorkflowDescriptionTemplate = new MonitorUpdateRollbackCleanupStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<MonitorUpdateRollbackCleanupStackWorkflow> monitorUpdateRollbackCleanupStackWorkflowClient = monitorUpdateRollbackCleanupStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(MonitorUpdateRollbackCleanupStackWorkflow.class, monitorUpdateRollbackCleanupStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);
    MonitorUpdateRollbackCleanupStackWorkflow monitorUpdateRollbackCleanupStackWorkflow = new MonitorUpdateRollbackCleanupStackWorkflowClient(monitorUpdateRollbackCleanupStackWorkflowClient);
    monitorUpdateRollbackCleanupStackWorkflow.monitorUpdateRollbackCleanupStack(stackId,  accountId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorUpdateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorUpdateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getRunId());

  }

  public static void kickOffUpdateRollbackStackWorkflow(String stackId, String accountId, String outerStackArn, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getStackName(), stackUpdateInfoEntity.getAccountId(), stackUpdateInfoEntity.getAccountAlias());
    StartTimeoutPassableWorkflowClientFactory updateRollbackStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate updateRollbackStackWorkflowDescriptionTemplate = new UpdateRollbackStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<UpdateRollbackStackWorkflow> updateRollbackStackWorkflowClient = updateRollbackStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(UpdateRollbackStackWorkflow.class, updateRollbackStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);

    UpdateRollbackStackWorkflow updateRollbackStackWorkflow = new UpdateRollbackStackWorkflowClient(updateRollbackStackWorkflowClient);
    updateRollbackStackWorkflow.performUpdateRollbackStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      outerStackArn, stackUpdateInfoEntity.getOldResourceDependencyManagerJson(),
      effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackStackWorkflowClient.getWorkflowExecution().getRunId());

    StartTimeoutPassableWorkflowClientFactory monitorUpdateRollbackStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate monitorUpdateRollbackStackWorkflowDescriptionTemplate = new MonitorUpdateRollbackStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<MonitorUpdateRollbackStackWorkflow> monitorUpdateRollbackStackWorkflowClient = monitorUpdateRollbackStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(MonitorUpdateRollbackStackWorkflow.class, monitorUpdateRollbackStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);
    MonitorUpdateRollbackStackWorkflow monitorUpdateRollbackStackWorkflow = new MonitorUpdateRollbackStackWorkflowClient(monitorUpdateRollbackStackWorkflowClient);
    monitorUpdateRollbackStackWorkflow.monitorUpdateRollbackStack(stackId,  accountId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_ROLLBACK_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorUpdateRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorUpdateRollbackStackWorkflowClient.getWorkflowExecution().getRunId());

  }

}
