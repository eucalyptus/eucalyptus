/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

import java.util.concurrent.TimeUnit

@CompileStatic(TypeCheckingMode.SKIP)
public class CommonMonitorPromises {
  private static final Logger LOG = Logger.getLogger(CommonMonitorPromises.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils(workflowOperations)


  CommonMonitorPromises() {
  }

  void monitor() {

    try {
      doTry {
        return performMonitor();
      } withCatch { Throwable t ->
        CommonMonitorPromises.LOG.error(t);
        CommonMonitorPromises.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      CommonMonitorPromises.LOG.error(ex);
      CommonMonitorPromises.LOG.debug(ex, ex);
    }
  }

  String accountId;
  String stackId;
  String baseWorkflowType;
  String monitoringWorkflowType;
  int stackVersion;

  String stackActionAsNoun;
  String expectedTimeoutStackStatus;

  Closure<Promise<String>> expectedTimeoutClosure;
  Map<String, Closure<Promise<String>>> otherSupportedStackStatusMap;

  private Promise<String> performMonitor() {
    Promise<String> closeStatusPromise = workflowUtils.fixedPollWithTimeout((int) TimeUnit.DAYS.toSeconds(365), 30) {
      retry(new ExponentialRetryPolicy(2L).withMaximumAttempts(6)) {
        activities.getWorkflowExecutionCloseStatus(stackId, baseWorkflowType)
      }
    }
    waitFor(closeStatusPromise) { String closedStatus ->
      if (!closedStatus) {
        waitFor(activities.logMessage("ERROR", "InternalFailureException: " + monitoringWorkflowType + " timed out before " + baseWorkflowType + " for stack id ${stackId}")) {
          promiseFor("");
        }
      }
      waitFor(activities.getStackStatusIfLatest(stackId, accountId, stackVersion)) { String stackStatus ->
        determineAction(closedStatus, stackStatus);
      }
    }

  }


  private Promise<String> determineAction(String closedStatus, String stackStatus) {
    waitFor(activities.logMessage("INFO", monitoringWorkflowType + " has detected a closed " + baseWorkflowType + ", determining stack status.  (Stack id: ${stackId})")) {
      waitFor(activities.logMessage("INFO", monitoringWorkflowType + " stack status is " + stackStatus + ".  (Stack id: ${stackId})")) {
        if (expectedTimeoutStackStatus.equals(stackStatus)) {
          waitFor(activities.logMessage("INFO", monitoringWorkflowType + " stack status " + stackStatus + " is handled by this workflow.  Determining next steps.  (Stack id: ${stackId})")) {
            waitFor(activities.logMessage("INFO", monitoringWorkflowType + " needs to determine " + baseWorkflowType + " closed status, which is ${closedStatus}. (Stack id: ${stackId})")) {
              boolean unsupportedClosedStatus = false;
              String statusReason = "";
              if ("CANCELED".equals(closedStatus)) {
                statusReason = "Stack ${stackActionAsNoun} was canceled by user.";
              } else if ("TERMINATED".equals(closedStatus)) {
                statusReason = "Stack ${stackActionAsNoun} was terminated by user.";
              } else if ("TIMED_OUT".equals(closedStatus)) {
                statusReason = "Stack ${stackActionAsNoun} timed out.";
              } else if ("COMPLETED".equals(closedStatus)) {
                statusReason = "";
              } else if ("FAILED".equals(closedStatus)) {
                statusReason = "Stack ${stackActionAsNoun} workflow failed.";
              } else {
                unsupportedClosedStatus = true;
              }
              if (unsupportedClosedStatus) {
                waitFor(activities.logMessage("INFO", baseWorkflowType + " closed status " + closedStatus + " is not supported. (Stack id: ${stackId})")) {
                  promiseFor("");
                }
              } else {
                waitFor(activities.logMessage("INFO", baseWorkflowType + " closed status " + closedStatus + " is supported. Determining next steps. (Stack id: ${stackId})")) {
                  expectedTimeoutClosure.call(statusReason);
                }
              }
            }
          }
        } else if (otherSupportedStackStatusMap != null && otherSupportedStackStatusMap.containsKey(stackStatus)) {
          waitFor(activities.logMessage("INFO", monitoringWorkflowType + " stack status " + stackStatus + " is handled by this workflow.  Determining next steps.  (Stack id: ${stackId})")) {
            otherSupportedStackStatusMap.get(stackStatus).call();
          }
        } else {
          waitFor(activities.logMessage("INFO", monitoringWorkflowType + " stack status " + stackStatus + " is not handled by this workflow.  (Stack id: ${stackId})")) {
            promiseFor("");
          }
        }
      }
    }
  }
}