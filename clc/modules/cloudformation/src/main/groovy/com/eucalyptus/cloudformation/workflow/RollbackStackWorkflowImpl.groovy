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
public class RollbackStackWorkflowImpl implements RollbackStackWorkflow {
  private static final Logger LOG = Logger.getLogger(RollbackStackWorkflowImpl.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient);

  @Override
  public void rollbackStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    doTry {
      // cancel existing create/monitor workflows...
      ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(10L).withMaximumRetryIntervalSeconds(10L).withExceptionsToRetry([RetryAfterConditionCheckFailedException.class])
      // TODO: maybe cancel other workflows?
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
                Status.ROLLBACK_IN_PROGRESS.toString(),
                "Create stack failed.  Rollback requested by user.",
                Status.ROLLBACK_FAILED.toString(),
                Status.ROLLBACK_COMPLETE.toString(),
                false).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion, "");
          }
        }
      }
    }.withCatch { Throwable ex ->
      RollbackStackWorkflowImpl.LOG.error(ex);
      RollbackStackWorkflowImpl.LOG.debug(ex, ex);
    }.getResult()
  }
}
