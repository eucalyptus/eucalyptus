/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static com.eucalyptus.cloudformation.workflow.WorkflowRegistry.WorkflowRegistration.create;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.cloudformation.ws.StackWorkflowTags;
import com.eucalyptus.util.TypedContext;
import com.eucalyptus.util.TypedKey;
import com.google.common.collect.Maps;
import io.vavr.Tuple2;

/**
 *
 */
public class WorkflowRegistry {

  public static WorkflowRegistration<CreateStackWorkflow> CreateStackWorkflowKey =
      create( "CreateStackWorkflowKey" );

  public static WorkflowRegistration<DeleteStackWorkflow> DeleteStackWorkflowKey =
      create( "DeleteStackWorkflowKey" );

  public static WorkflowRegistration<MonitorCreateStackWorkflow> MonitorCreateStackWorkflowKey =
      create( "MonitorCreateStackWorkflowKey" );

  public static WorkflowRegistration<MonitorDeleteStackWorkflow> MonitorDeleteStackWorkflowKey =
      create( "MonitorDeleteStackWorkflowKey" );

  public static WorkflowRegistration<MonitorRollbackStackWorkflow> MonitorRollbackStackWorkflowKey =
      create( "MonitorRollbackStackWorkflowKey" );

  public static WorkflowRegistration<MonitorUpdateCleanupStackWorkflow> MonitorUpdateCleanupStackWorkflowKey =
      create( "MonitorUpdateCleanupStackWorkflowKey" );

  public static WorkflowRegistration<MonitorUpdateRollbackCleanupStackWorkflow> MonitorUpdateRollbackCleanupStackWorkflowKey =
      create( "MonitorUpdateRollbackCleanupStackWorkflowKey" );

  public static WorkflowRegistration<MonitorUpdateRollbackStackWorkflow> MonitorUpdateRollbackStackWorkflowKey =
      create( "MonitorUpdateRollbackStackWorkflowKey" );

  public static WorkflowRegistration<MonitorUpdateStackWorkflow> MonitorUpdateStackWorkflowKey =
      create( "MonitorUpdateStackWorkflowKey" );

  public static WorkflowRegistration<RollbackStackWorkflow> RollbackStackWorkflowKey =
      create( "RollbackStackWorkflowKey" );

  public static WorkflowRegistration<UpdateCleanupStackWorkflow> UpdateCleanupStackWorkflowKey =
      create( "UpdateCleanupStackWorkflowKey" );

  public static WorkflowRegistration<UpdateRollbackCleanupStackWorkflow> UpdateRollbackCleanupStackWorkflowKey =
      create( "UpdateRollbackCleanupStackWorkflowKey" );

  public static WorkflowRegistration<UpdateRollbackStackWorkflow> UpdateRollbackStackWorkflowKey =
      create( "UpdateRollbackStackWorkflowKey" );

  public static WorkflowRegistration<UpdateStackWorkflow> UpdateStackWorkflowKey =
      create( "UpdateStackWorkflowKey" );

  private static final TypedContext workflows = TypedContext.newTypedContext( Maps.newConcurrentMap( ) );

  public static final class WorkflowRegistration<T> {
    private final TypedKey<WorkflowBuilder<T>> key;

    private WorkflowRegistration( final TypedKey<WorkflowBuilder<T>> key ) {
      this.key = key;
    }

    TypedKey<WorkflowBuilder<T>> key( ) {
      return key;
    }

    public static <T> WorkflowRegistration<T> create( final String description ) {
      return new WorkflowRegistration<>( TypedKey.create( description ) );
    }
  }

  @FunctionalInterface
  public interface WorkflowBuilder<T> {
    Tuple2<WorkflowClientExternal,T> build(
        StackWorkflowTags tags,
        Long timeoutInSeconds
    );
  }

  public static <T> Tuple2<WorkflowClientExternal,T> getWorkflowClient(
      final WorkflowRegistration<T> key,
      final StackWorkflowTags tags,
      final Long timeoutInSeconds
  ) {
    return workflows.get( key.key( ) ).build( tags, timeoutInSeconds );
  }

  public static <T> Tuple2<WorkflowClientExternal,T> getWorkflowClient(
      final WorkflowRegistration<T> key,
      final StackWorkflowTags tags
  ) {
    return getWorkflowClient( key, tags, null );
  }

  public static <T> void registerWorkflowClient(
      final WorkflowRegistration<T> registration,
      final WorkflowBuilder<T> builder
  ) {
    workflows.put( registration.key( ), builder );
  }
}
