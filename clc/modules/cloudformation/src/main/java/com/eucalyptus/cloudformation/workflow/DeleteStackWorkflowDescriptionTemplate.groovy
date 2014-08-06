package com.eucalyptus.cloudformation.workflow

import com.netflix.glisten.WorkflowDescriptionTemplate

/**
 * Created by ethomas on 7/23/14.
 */
class DeleteStackWorkflowDescriptionTemplate extends WorkflowDescriptionTemplate implements DeleteStackWorkflow {

  @Override
  void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId) {
    description="DeleteStackWorkflow";
  }
}
