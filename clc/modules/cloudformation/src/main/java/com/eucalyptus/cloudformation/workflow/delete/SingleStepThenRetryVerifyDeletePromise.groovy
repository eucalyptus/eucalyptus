package com.eucalyptus.cloudformation.workflow.delete

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 10/1/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class SingleStepThenRetryVerifyDeletePromise {
  @Delegate
  private final DeleteStackWorkflowImpl deleteStackWorkflow;
  private final String initialStepId;
  private final String retryStepId;
  private final RetryPolicy retryPolicy;

  public SingleStepThenRetryVerifyDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String initialStepId, String retryStepId, RetryPolicy retryPolicy) {
    this.deleteStackWorkflow = deleteStackWorkflow;
    this.initialStepId = initialStepId;
    this.retryStepId = retryStepId;
    this.retryPolicy = retryPolicy;
  }

  public Promise<String> getDeletePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {

    doTry {
      waitFor(promiseFor(activities.performDeleteStep(initialStepId, resourceId, stackId, accountId, effectiveUserId))) {
        waitFor(
          retry(retryPolicy) {
            promiseFor(activities.performDeleteStep(retryStepId, resourceId, stackId, accountId, effectiveUserId));
          }
        ) {
          promiseFor(activities.finalizeDeleteResource(resourceId, stackId, accountId, effectiveUserId));
        }
      }
    }.withCatch {Throwable t->
      promiseFor(activities.failDeleteResource(resourceId, stackId, accountId, effectiveUserId, t.getMessage()));
    }.getResult();
  }
}
