package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl
import com.eucalyptus.cloudformation.workflow.StackActivity
import com.eucalyptus.cloudformation.workflow.steps.Step
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class MultiStepWithRetryDeletePromise {

  public MultiStepWithRetryDeletePromise(WorkflowOperations<StackActivity> workflowOperations, Collection<String> stepIds, ResourceAction resourceAction) {
    this.workflowOperations = workflowOperations;
    this.stepIds = stepIds;
    this.resourceAction = resourceAction;
  }
  @Delegate
  private final WorkflowOperations<StackActivity> workflowOperations;
  private final List<String> stepIds;
  private final ResourceAction resourceAction;

  public Promise<String> getDeletePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {
    Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
    for (String stepId: stepIds) {
      String stepIdLocal = new String(stepId); // If you access "stepId" from the wait for, it is executed after the for loop is finished.  You need the value during this iteration
      Step deleteStep = resourceAction.getDeleteStep(stepIdLocal);
      RetryPolicy retryPolicy = deleteStep.getRetryPolicy();
      if (retryPolicy != null) {
        previousPromise = waitFor(previousPromise) {
          retry(retryPolicy) {
            promiseFor(activities.performDeleteStep(stepIdLocal, resourceId, stackId, accountId, effectiveUserId));
          }
        }
      } else {
        previousPromise = waitFor(previousPromise) {
          promiseFor(activities.performDeleteStep(stepIdLocal, resourceId, stackId, accountId, effectiveUserId));
        }
      }
    }
    return previousPromise;
  }
}
