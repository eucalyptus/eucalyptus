package com.eucalyptus.cloudformation.workflow

import com.netflix.glisten.WorkflowDescriptionTemplate

/**
 * Created by ethomas on 7/23/14.
 */
class MonitorCreateStackWorkflowDescriptionTemplate extends WorkflowDescriptionTemplate implements MonitorCreateStackWorkflow {

  @Override
  void monitorCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    description="MonitorCreateStackWorkflow";
  }
}
