/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudformation.workflow

import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.component.annotation.ComponentPart
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class ContinueUpdateRollbackWorkflowImpl implements ContinueUpdateRollbackWorkflow {
  private static final Logger LOG = Logger.getLogger(ContinueUpdateRollbackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void continueUpdateRollback(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    try {
      doTry {
        return new CommonUpdateRollbackPromises(workflowOperations).performRollbackAndCleanup(stackId, accountId, oldResourceDependencyManagerJson, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion, true);
      } withCatch { Throwable t->
        ContinueUpdateRollbackWorkflowImpl.LOG.error(t);
        ContinueUpdateRollbackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      ContinueUpdateRollbackWorkflowImpl.LOG.error(ex);
      ContinueUpdateRollbackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

}
