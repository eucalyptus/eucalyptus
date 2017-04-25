/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
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