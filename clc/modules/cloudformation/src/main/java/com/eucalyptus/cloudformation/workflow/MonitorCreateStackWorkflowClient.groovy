package com.eucalyptus.cloudformation.workflow

import com.netflix.glisten.InterfaceBasedWorkflowClient

/**
 * Created by ethomas on 6/9/44.
 */
class MonitorCreateStackWorkflowClient implements MonitorCreateStackWorkflow {
  MonitorCreateStackWorkflow workflow

  InterfaceBasedWorkflowClient<MonitorCreateStackWorkflow> getClient() {
    return client
  }

  InterfaceBasedWorkflowClient<MonitorCreateStackWorkflow> client;

  MonitorCreateStackWorkflowClient( InterfaceBasedWorkflowClient<MonitorCreateStackWorkflow> client ) {
    this.client = client;
    workflow = client.asWorkflow( ) as MonitorCreateStackWorkflow
  }

  @Override
  void monitorCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    workflow.monitorCreateStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure);
  }


}
