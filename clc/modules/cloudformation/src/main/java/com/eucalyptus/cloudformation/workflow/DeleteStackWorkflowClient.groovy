package com.eucalyptus.cloudformation.workflow

import com.netflix.glisten.InterfaceBasedWorkflowClient

/**
 * Created by ethomas on 6/9/44.
 */
class DeleteStackWorkflowClient implements DeleteStackWorkflow {
  DeleteStackWorkflow workflow

  DeleteStackWorkflowClient( InterfaceBasedWorkflowClient<DeleteStackWorkflow> client ) {
    workflow = client.asWorkflow( ) as DeleteStackWorkflow
  }

  @Override
  void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId) {
    workflow.deleteStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId);
  }
}
