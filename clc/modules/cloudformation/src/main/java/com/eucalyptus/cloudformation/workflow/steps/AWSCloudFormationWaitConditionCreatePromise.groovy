package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.workflow.StackActivity
import com.eucalyptus.cloudformation.workflow.ValidationFailedException
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.util.concurrent.TimeUnit

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class AWSCloudFormationWaitConditionCreatePromise {

  public AWSCloudFormationWaitConditionCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String stepId) {
    this.workflowOperations = workflowOperations
    this.workflowUtils = new WorkflowUtils( workflowOperations )
    this.stepId = stepId
  }
  @Delegate
  private final WorkflowOperations<StackActivity> workflowOperations
  private final WorkflowUtils workflowUtils
  private final String stepId

  public Promise<String> getCreatePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {
    Promise<Integer> timeoutPromise =
        promiseFor(activities.getAWSCloudFormationWaitConditionTimeout(resourceId, stackId, accountId, effectiveUserId))
    return waitFor( timeoutPromise ) { Integer timeout ->
      waitFor( workflowUtils.exponentialPollWithTimeout( timeout, 10, 1.15, (int)TimeUnit.MINUTES.toSeconds( 2 ) ) {
        promiseFor( activities.performCreateStep( stepId, resourceId, stackId, accountId, effectiveUserId ) )
      } ) { Boolean created ->
        if ( !created ) {
          throw new ValidationFailedException( "Resource ${resourceId} timeout for stack ${stackId}" )
        }
        promiseFor( "" )
      }
    }
  }
}
