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

import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.config.CloudFormationProperties;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntity;
import com.eucalyptus.cloudformation.entity.StackUpdateInfoEntityManager;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity;
import com.eucalyptus.cloudformation.entity.StackWorkflowEntityManager;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import io.vavr.Tuple2;

/**
 * Created by ethomas on 4/11/16.
 */
public class UpdateStackPartsWorkflowKickOff {

  public static void kickOffUpdateCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId) throws CloudFormationException, ResourceFailureException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ResourceFailureException("Unable to find update info record for stack " + stackId);
    }
    final StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(
        stackUpdateInfoEntity.getStackId(),
        stackUpdateInfoEntity.getStackName(),
        stackUpdateInfoEntity.getAccountId(),
        stackUpdateInfoEntity.getAccountAlias() );


    final Tuple2<WorkflowClientExternal,UpdateCleanupStackWorkflow> updateCleanupStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.UpdateCleanupStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal updateCleanupStackWorkflowClient = updateCleanupStackWorkflowClients._1();
    final UpdateCleanupStackWorkflow updateCleanupStackWorkflow = updateCleanupStackWorkflowClients._2( );
    updateCleanupStackWorkflow.performUpdateCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getOldResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion());

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateCleanupStackWorkflowClient.getWorkflowExecution().getRunId());

    final Tuple2<WorkflowClientExternal,MonitorUpdateCleanupStackWorkflow> monitorUpdateCleanupStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.MonitorUpdateCleanupStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal monitorUpdateCleanupStackWorkflowClient = monitorUpdateCleanupStackWorkflowClients._1();
    final MonitorUpdateCleanupStackWorkflow monitorUpdateCleanupStackWorkflow = monitorUpdateCleanupStackWorkflowClients._2( );
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

    final StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(
        stackUpdateInfoEntity.getStackId(),
        stackUpdateInfoEntity.getStackName(),
        stackUpdateInfoEntity.getAccountId(),
        stackUpdateInfoEntity.getAccountAlias());

    final Tuple2<WorkflowClientExternal,UpdateRollbackCleanupStackWorkflow> updateRollbackCleanupStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.UpdateRollbackCleanupStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal updateRollbackCleanupStackWorkflowClient = updateRollbackCleanupStackWorkflowClients._1();
    final UpdateRollbackCleanupStackWorkflow updateRollbackCleanupStackWorkflow = updateRollbackCleanupStackWorkflowClients._2( );
    updateRollbackCleanupStackWorkflow.performUpdateRollbackCleanupStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      stackUpdateInfoEntity.getResourceDependencyManagerJson(), effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackCleanupStackWorkflowClient.getWorkflowExecution().getRunId());

    final Tuple2<WorkflowClientExternal,MonitorUpdateRollbackCleanupStackWorkflow> monitorUpdateRollbackCleanupStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.MonitorUpdateRollbackCleanupStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal monitorUpdateRollbackCleanupStackWorkflowClient = monitorUpdateRollbackCleanupStackWorkflowClients._1();
    final MonitorUpdateRollbackCleanupStackWorkflow monitorUpdateRollbackCleanupStackWorkflow = monitorUpdateRollbackCleanupStackWorkflowClients._2( );
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

    final StackWorkflowTags stackWorkflowTags = new StackWorkflowTags(
        stackUpdateInfoEntity.getStackId(),
        stackUpdateInfoEntity.getStackName(),
        stackUpdateInfoEntity.getAccountId(),
        stackUpdateInfoEntity.getAccountAlias() );


    final Tuple2<WorkflowClientExternal,UpdateRollbackStackWorkflow> updateRollbackStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.UpdateRollbackStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal updateRollbackStackWorkflowClient = updateRollbackStackWorkflowClients._1();
    final UpdateRollbackStackWorkflow updateRollbackStackWorkflow = updateRollbackStackWorkflowClients._2( );
    updateRollbackStackWorkflow.performUpdateRollbackStack(stackUpdateInfoEntity.getStackId(), stackUpdateInfoEntity.getAccountId(),
      outerStackArn, stackUpdateInfoEntity.getOldResourceDependencyManagerJson(),
      effectiveUserId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackUpdateInfoEntity.getStackId(),
      StackWorkflowEntity.WorkflowType.UPDATE_ROLLBACK_STACK_WORKFLOW, CloudFormationProperties.SWF_DOMAIN,
      updateRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      updateRollbackStackWorkflowClient.getWorkflowExecution().getRunId());

    final Tuple2<WorkflowClientExternal,MonitorUpdateRollbackStackWorkflow> monitorUpdateRollbackStackWorkflowClients =
        WorkflowRegistry.getWorkflowClient( WorkflowRegistry.MonitorUpdateRollbackStackWorkflowKey, stackWorkflowTags );
    final WorkflowClientExternal monitorUpdateRollbackStackWorkflowClient = monitorUpdateRollbackStackWorkflowClients._1();
    final MonitorUpdateRollbackStackWorkflow monitorUpdateRollbackStackWorkflow = monitorUpdateRollbackStackWorkflowClients._2( );
    monitorUpdateRollbackStackWorkflow.monitorUpdateRollbackStack(stackId,  accountId, stackUpdateInfoEntity.getUpdatedStackVersion() + 1);

    StackWorkflowEntityManager.addOrUpdateStackWorkflowEntity(stackId,
      StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_ROLLBACK_STACK_WORKFLOW,
      CloudFormationProperties.SWF_DOMAIN,
      monitorUpdateRollbackStackWorkflowClient.getWorkflowExecution().getWorkflowId(),
      monitorUpdateRollbackStackWorkflowClient.getWorkflowExecution().getRunId());
  }

}
