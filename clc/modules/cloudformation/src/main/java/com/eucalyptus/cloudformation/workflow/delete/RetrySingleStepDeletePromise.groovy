package com.eucalyptus.cloudformation.workflow.delete

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl
import com.netflix.glisten.DoTry
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/30/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class RetrySingleStepDeletePromise {

  @Delegate
  private final DeleteStackWorkflowImpl deleteStackWorkflow;
  private final String stepId;
  private final RetryPolicy retryPolicy;

  RetrySingleStepDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String stepId, RetryPolicy retryPolicy) {
    this.deleteStackWorkflow = deleteStackWorkflow;
    this.stepId = stepId;
    this.retryPolicy = retryPolicy;
  }

  public Promise<String> getDeletePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {
    doTry {
      waitFor(
        retry(retryPolicy) {
          promiseFor(activities.performDeleteStep(stepId, resourceId, stackId, accountId, effectiveUserId));
        }
      ) {
        promiseFor(activities.finalizeDeleteResource(resourceId, stackId, accountId, effectiveUserId));
      }
    }.withCatch {Throwable t->
      promiseFor(activities.failDeleteResource(resourceId, stackId, accountId, effectiveUserId, t.getMessage()));
    }.getResult();
  }

}
