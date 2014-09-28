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
    Promise<String> initPromise = promiseFor(activities.initResource(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson));
    waitFor(initPromise) { String result ->
      if ("SKIP".equals(result)) {
        return promiseFor("");
      } else {
        Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
        for (String stepId: stepIds) {
          previousPromise = waitFor(previousPromise) {
            promiseFor(activities.performCreateStep(stepId, resourceId, stackId, accountId, effectiveUserId));
          }
        }
        return previousPromise;
      }
    }
  }
}
