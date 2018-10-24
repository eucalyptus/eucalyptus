/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class DeleteStackWorkflowImpl implements DeleteStackWorkflow {
  private static final Logger LOG = Logger.getLogger(DeleteStackWorkflowImpl.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient);

  @Override
  public void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int deletedStackVersion, String retainedResourcesStr) {
    try {
      doTry {
        return performDeleteStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, deletedStackVersion, retainedResourcesStr);
      } withCatch { Throwable t->
        DeleteStackWorkflowImpl.LOG.error(t);
        DeleteStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      DeleteStackWorkflowImpl.LOG.error(ex);
      DeleteStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> performDeleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int deletedStackVersion, String retainedResourcesStr) {
    // cancel existing creae/monitor workflows...
    ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(10L).withMaximumRetryIntervalSeconds(10L).withExceptionsToRetry([RetryAfterConditionCheckFailedException.class])
    Promise<String> cancelWorkflowsPromise = activities.cancelCreateAndMonitorWorkflows(stackId);
    waitFor(cancelWorkflowsPromise) {
      waitFor(
        retry(retryPolicy) {
          activities.verifyCreateAndMonitorWorkflowsClosed(stackId);
        }
      ) {
        Promise<String> flattenStackPromise = activities.flattenStackForDelete(stackId, accountId);
        waitFor(flattenStackPromise) {
          new CommonDeleteRollbackPromises(workflowOperations,
            Status.DELETE_IN_PROGRESS.toString(),
            "User Initiated",
            Status.DELETE_FAILED.toString(),
            Status.DELETE_COMPLETE.toString(),
            true).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, deletedStackVersion, retainedResourcesStr);
        }
      }
    }
  }
}
