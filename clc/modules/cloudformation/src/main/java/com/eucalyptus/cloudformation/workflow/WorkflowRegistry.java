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
