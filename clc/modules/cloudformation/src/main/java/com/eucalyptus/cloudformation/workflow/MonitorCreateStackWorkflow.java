package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

/**
 * Created by ethomas on 10/6/14.
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 10800)
public interface MonitorCreateStackWorkflow {
  @Execute(version = "1.0")
  public void monitorCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure);
}
