package com.eucalyptus.cloudformation.workflow.create

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class MultiStepCreatePromise {

  public MultiStepCreatePromise(CreateStackWorkflowImpl createStackWorkflow, Collection<String> stepIds) {
    this.createStackWorkflow = createStackWorkflow;
    this.stepIds = stepIds;
  }
  @Delegate
  private final CreateStackWorkflowImpl createStackWorkflow;
  private final List<String> stepIds;

  public Promise<String> getCreatePromise(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson) {
    Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
    for (String stepId: stepIds) {
      String stepIdLocal = new String(stepId); // If you access "stepId" from the wait for, it is executed after the for loop is finished.  You need the value during this iteration
      previousPromise = waitFor(previousPromise) {
        promiseFor(activities.performCreateStep(stepIdLocal, resourceId, stackId, accountId, effectiveUserId));
      }
    }
    return previousPromise;
  }
}
