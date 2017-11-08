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
package com.eucalyptus.cloudformation.workflow;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.DeleteStackWorkflowExtraInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowDescriptionTemplate;
import com.netflix.glisten.WorkflowTags;

/**
 * Created by ethomas on 4/18/17.
 */
public class CommonDeleteRollbackKickoff {

  public static void kickOffDeleteStackWorkflow(String effectiveUserId, String stackId, String stackName,
                                                String stackAccountId, String stackAccountAlias,
                                                String resourceDependencyManagerJson, int deletedStackVersion,
                                                String retainedResourcesStr) throws CloudFormationException {
    StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(stackId, stackName, stackAccountId, stackAccountAlias);

    StartTimeoutPassableWorkflowClientFactory workflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(),
      CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
      .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, stackWorkflowTags, null, null);
    DeleteStackWorkflow deleteStackWorkflow = new DeleteStackWorkflowClient(client);
    deleteStackWorkflow.deleteStack(stackId, stackAccountId, resourceDependencyManagerJson, effectiveUserId,
      deletedStackVersion, retainedResourcesStr);
    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.DELETE_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      client.getWorkflowExecution().getWorkflowId(),
      client.getWorkflowExecution().getRunId());

    StartTimeoutPassableWorkflowClientFactory monitorDeleteStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(),
      CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
    WorkflowDescriptionTemplate monitorDeleteStackWorkflowDescriptionTemplate = new MonitorDeleteStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<MonitorDeleteStackWorkflow> monitorDeleteStackWorkflowClient = monitorDeleteStackStartTimeoutPassableWorkflowClientFactory
      .getNewWorkflowClient(MonitorDeleteStackWorkflow.class, monitorDeleteStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);
    MonitorDeleteStackWorkflow monitorDeleteStackWorkflow = new MonitorDeleteStackWorkflowClient(monitorDeleteStackWorkflowClient);
    monitorDeleteStackWorkflow.monitorDeleteStack(stackId,  stackAccountId, effectiveUserId, deletedStackVersion);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_DELETE_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorDeleteStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorDeleteStackWorkflowClient.getWorkflowExecution().getRunId());
  }

    public static void kickOffRollbackStackWorkflow(String effectiveUserId, String stackId, String stackName, String accountId, String accountAlias, String resourceDependencyManagerJson, int rolledBackStackVersion) throws CloudFormationException {
      StackWorkflowTags stackWorkflowTags =
        new StackWorkflowTags(stackId, stackName, accountId, accountAlias );

      StartTimeoutPassableWorkflowClientFactory workflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
      WorkflowDescriptionTemplate workflowDescriptionTemplate = new RollbackStackWorkflowDescriptionTemplate();
      InterfaceBasedWorkflowClient<RollbackStackWorkflow> client = workflowClientFactory
        .getNewWorkflowClient(RollbackStackWorkflow.class, workflowDescriptionTemplate, stackWorkflowTags, null, null);
      RollbackStackWorkflow rollbackStackWorkflow = new RollbackStackWorkflowClient(client);
      rollbackStackWorkflow.rollbackStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);
      StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
        StackWorkflowEntity.WorkflowType.ROLLBACK_STACK_WORKFLOW,
        CloudFormationProperties.SWF_DOMAIN,
        client.getWorkflowExecution().getWorkflowId(),
        client.getWorkflowExecution().getRunId());

      StartTimeoutPassableWorkflowClientFactory monitorRollbackStackStartTimeoutPassableWorkflowClientFactory = new StartTimeoutPassableWorkflowClientFactory(WorkflowClientManager.getSimpleWorkflowClient(), CloudFormationProperties.SWF_DOMAIN, CloudFormationProperties.SWF_TASKLIST);
      WorkflowDescriptionTemplate monitorRollbackStackWorkflowDescriptionTemplate = new MonitorRollbackStackWorkflowDescriptionTemplate();
      InterfaceBasedWorkflowClient<MonitorRollbackStackWorkflow> monitorRollbackStackWorkflowClient = monitorRollbackStackStartTimeoutPassableWorkflowClientFactory
        .getNewWorkflowClient(MonitorRollbackStackWorkflow.class, monitorRollbackStackWorkflowDescriptionTemplate, stackWorkflowTags, null, null);
      MonitorRollbackStackWorkflow monitorRollbackStackWorkflow = new MonitorRollbackStackWorkflowClient(monitorRollbackStackWorkflowClient);
      monitorRollbackStackWorkflow.monitorRollbackStack(stackId,  accountId, effectiveUserId, rolledBackStackVersion);

      StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
        StackWorkflowEntity.WorkflowType.MONITOR_ROLLBACK_STACK_WORKFLOW,
        CloudFormationProperties.SWF_DOMAIN,
        monitorRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
        monitorRollbackStackWorkflowClient.getWorkflowExecution().getRunId());
    }

}
