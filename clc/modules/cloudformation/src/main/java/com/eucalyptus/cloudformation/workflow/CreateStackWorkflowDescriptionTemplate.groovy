package com.eucalyptus.cloudformation.workflow

import com.netflix.glisten.WorkflowDescriptionTemplate

/**
 * Created by ethomas on 7/23/14.
 */
class CreateStackWorkflowDescriptionTemplate extends WorkflowDescriptionTemplate implements CreateStackWorkflow {

  @Override
  void createStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    description="CreateStackWorkflow";
  }
}
