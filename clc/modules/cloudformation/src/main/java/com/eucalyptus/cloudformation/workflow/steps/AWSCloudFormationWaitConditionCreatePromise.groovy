package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.workflow.StackActivity
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class AWSCloudFormationWaitConditionCreatePromise {

  public AWSCloudFormationWaitConditionCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String stepId, ResourceAction resourceAction) {
    this.workflowOperations = workflowOperations;
    this.stepId = stepId;
    this.resourceAction = resourceAction;
  }
  @Delegate
  private final WorkflowOperations<StackActivity> workflowOperations;
  private final String stepId;
  private final ResourceAction resourceAction;

  public Promise<String> getCreatePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {
    Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
    String stepIdLocal = new String(stepId);
    // If you access "stepId" from the wait for, it is executed after the for loop is finished.  You need the value during this iteration
    Step createStep = resourceAction.getCreateStep(stepIdLocal);
    Promise<String> timeoutPromise = promiseFor(activities.getAWSCloudFormationWaitConditionTimeout(resourceId, stackId, accountId, effectiveUserId));
    return waitFor(timeoutPromise) { String result ->
      RetryPolicy retryPolicy = new StandardResourceRetryPolicy(Integer.valueOf(result));
      retry(retryPolicy) {
        promiseFor(activities.performCreateStep(stepId, resourceId, stackId, accountId, effectiveUserId));
      }
    }
  }
}
