package com.eucalyptus.cloudformation.workflow

import com.eucalyptus.bootstrap.Bootstrap
import com.eucalyptus.bootstrap.Bootstrapper
import com.eucalyptus.bootstrap.Provides
import com.eucalyptus.bootstrap.RunDuring
import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.config.CloudFormationProperties
import com.eucalyptus.cloudformation.ws.StackWorkflowTags
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowDescriptionTemplate
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.util.function.Function

/**
 *
 */
@SuppressWarnings("GroovyUnusedDeclaration")
@CompileStatic
class WorkflowRegistration {

  static void registerWorkflows( ) {
    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.CreateStackWorkflowKey,
        builder(
            CreateStackWorkflow,
            new CreateStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<CreateStackWorkflow> client ->
              new CreateStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.DeleteStackWorkflowKey,
        builder(
            DeleteStackWorkflow,
            new DeleteStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<DeleteStackWorkflow> client ->
              new DeleteStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorCreateStackWorkflowKey,
        builder(
            MonitorCreateStackWorkflow,
            new MonitorCreateStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorCreateStackWorkflow> client ->
              new MonitorCreateStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorDeleteStackWorkflowKey,
        builder(
            MonitorDeleteStackWorkflow,
            new MonitorDeleteStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorDeleteStackWorkflow> client ->
              new MonitorDeleteStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorRollbackStackWorkflowKey,
        builder(
            MonitorRollbackStackWorkflow,
            new MonitorRollbackStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorRollbackStackWorkflow> client ->
              new MonitorRollbackStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorUpdateCleanupStackWorkflowKey,
        builder(
            MonitorUpdateCleanupStackWorkflow,
            new MonitorUpdateCleanupStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorUpdateCleanupStackWorkflow> client ->
              new MonitorUpdateCleanupStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorUpdateRollbackCleanupStackWorkflowKey,
        builder(
            MonitorUpdateRollbackCleanupStackWorkflow,
            new MonitorUpdateRollbackCleanupStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorUpdateRollbackCleanupStackWorkflow> client ->
              new MonitorUpdateRollbackCleanupStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorUpdateRollbackStackWorkflowKey,
        builder(
            MonitorUpdateRollbackStackWorkflow,
            new MonitorUpdateRollbackStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorUpdateRollbackStackWorkflow> client ->
              new MonitorUpdateRollbackStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.MonitorUpdateStackWorkflowKey,
        builder(
            MonitorUpdateStackWorkflow,
            new MonitorUpdateStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<MonitorUpdateStackWorkflow> client ->
              new MonitorUpdateStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.RollbackStackWorkflowKey,
        builder(
            RollbackStackWorkflow,
            new RollbackStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<RollbackStackWorkflow> client ->
              new RollbackStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.UpdateCleanupStackWorkflowKey,
        builder(
            UpdateCleanupStackWorkflow,
            new UpdateCleanupStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<UpdateCleanupStackWorkflow> client ->
              new UpdateCleanupStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.UpdateRollbackCleanupStackWorkflowKey,
        builder(
            UpdateRollbackCleanupStackWorkflow,
            new UpdateRollbackCleanupStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<UpdateRollbackCleanupStackWorkflow> client ->
              new UpdateRollbackCleanupStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.UpdateRollbackStackWorkflowKey,
        builder(
            UpdateRollbackStackWorkflow,
            new UpdateRollbackStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<UpdateRollbackStackWorkflow> client ->
              new UpdateRollbackStackWorkflowClient( client ) } ) )

    WorkflowRegistry.registerWorkflowClient(
        WorkflowRegistry.UpdateStackWorkflowKey,
        builder(
            UpdateStackWorkflow,
            new UpdateStackWorkflowDescriptionTemplate( ),
            { InterfaceBasedWorkflowClient<UpdateStackWorkflow> client ->
              new UpdateStackWorkflowClient( client ) } ) )

  }

  private static <T> WorkflowRegistry.WorkflowBuilder<T> builder(
      Class<T> workflowType,
      WorkflowDescriptionTemplate template,
      Function<InterfaceBasedWorkflowClient<T>,T> clientFactory
  ){
    { StackWorkflowTags tags, Long timeout ->

      StartTimeoutPassableWorkflowClientFactory factory =
          new StartTimeoutPassableWorkflowClientFactory(
            WorkflowClientManager.getSimpleWorkflowClient(),
            CloudFormationProperties.SWF_DOMAIN,
            CloudFormationProperties.SWF_TASKLIST );

      InterfaceBasedWorkflowClient<T> client = factory.getNewWorkflowClient(
          workflowType,
          template,
          tags, timeout, null )
      io.vavr.Tuple.of( client, clientFactory.apply( client ) )
    } as WorkflowRegistry.WorkflowBuilder<T>
  }

  /**
   * Registers cloudformation workflows
   */
  @Provides( CloudFormation )
  @RunDuring( Bootstrap.Stage.UnprivilegedConfiguration )
  static class WorkflowRegistrationBootstrapper extends Bootstrapper.Simple {
    private static final Logger logger = Logger.getLogger( WorkflowRegistrationBootstrapper )

    @Override
    boolean load( ) throws Exception {
      logger.info( "Registering CloudFormation workflows" )
      registerWorkflows( )
      return true;
    }
  }
}
