package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

/**
 * Created by ethomas on 2/18/14.
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 3600)
public interface CreateStackWorkflow {
  public void createStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure);}
