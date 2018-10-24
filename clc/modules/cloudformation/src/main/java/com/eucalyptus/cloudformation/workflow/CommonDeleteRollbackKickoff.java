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

import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import io.vavr.Tuple2;

/**
 * Created by ethomas on 4/18/17.
 */
public class CommonDeleteRollbackKickoff {

  public static void kickOffDeleteStackWorkflow(String effectiveUserId, String stackId, String stackName,
                                                String stackAccountId, String stackAccountAlias,
                                                String resourceDependencyManagerJson, int deletedStackVersion,
                                                String retainedResourcesStr) throws CloudFormationException {
    final StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(
        stackId, stackName, stackAccountId, stackAccountAlias);

    final Tuple2<WorkflowClientExternal,DeleteStackWorkflow> deleteStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.DeleteStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal client = deleteStackWorkflowClients._1();
    final DeleteStackWorkflow deleteStackWorkflow = deleteStackWorkflowClients._2( );
    deleteStackWorkflow.deleteStack(stackId, stackAccountId, resourceDependencyManagerJson, effectiveUserId,
      deletedStackVersion, retainedResourcesStr);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.DELETE_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      client.getWorkflowExecution().getWorkflowId(),
      client.getWorkflowExecution().getRunId());

    final Tuple2<WorkflowClientExternal,MonitorDeleteStackWorkflow> monitorDeleteStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.MonitorDeleteStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal monitorDeleteStackWorkflowClient = monitorDeleteStackWorkflowClients._1();
    final MonitorDeleteStackWorkflow monitorDeleteStackWorkflow = monitorDeleteStackWorkflowClients._2( );
    monitorDeleteStackWorkflow.monitorDeleteStack(stackId,  stackAccountId, effectiveUserId, deletedStackVersion);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_DELETE_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorDeleteStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorDeleteStackWorkflowClient.getWorkflowExecution().getRunId());
  }

    public static void kickOffRollbackStackWorkflow(String effectiveUserId, String stackId, String stackName, String accountId, String accountAlias, String resourceDependencyManagerJson, int rolledBackStackVersion) throws CloudFormationException {
      final StackWorkflowTags stackWorkflowTags =
        new StackWorkflowTags(stackId, stackName, accountId, accountAlias );

      final Tuple2<WorkflowClientExternal,RollbackStackWorkflow> rollbackStackWorkflowClients =
          WorkflowRegistry.getWorkflowClient( WorkflowRegistry.RollbackStackWorkflowKey, stackWorkflowTags );
      final WorkflowClientExternal client = rollbackStackWorkflowClients._1();
      final RollbackStackWorkflow rollbackStackWorkflow = rollbackStackWorkflowClients._2( );
      rollbackStackWorkflow.rollbackStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);

      StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
        StackWorkflowEntity.WorkflowType.ROLLBACK_STACK_WORKFLOW,
        CloudFormationProperties.SWF_DOMAIN,
        client.getWorkflowExecution().getWorkflowId(),
        client.getWorkflowExecution().getRunId());

      final Tuple2<WorkflowClientExternal,MonitorRollbackStackWorkflow> monitorRollbackStackWorkflowClients =
          WorkflowRegistry.getWorkflowClient( WorkflowRegistry.MonitorRollbackStackWorkflowKey, stackWorkflowTags );
      final WorkflowClientExternal monitorRollbackStackWorkflowClient = monitorRollbackStackWorkflowClients._1();
      final MonitorRollbackStackWorkflow monitorRollbackStackWorkflow = monitorRollbackStackWorkflowClients._2( );
      monitorRollbackStackWorkflow.monitorRollbackStack(stackId,  accountId, effectiveUserId, rolledBackStackVersion);

      StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
        StackWorkflowEntity.WorkflowType.MONITOR_ROLLBACK_STACK_WORKFLOW,
        CloudFormationProperties.SWF_DOMAIN,
        monitorRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
        monitorRollbackStackWorkflowClient.getWorkflowExecution().getRunId());
    }

}
