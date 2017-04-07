/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientFactoryExternalBase
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import com.eucalyptus.system.Ats
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowClientFactory
import com.netflix.glisten.WorkflowDescriptionTemplate
import com.netflix.glisten.WorkflowTags
import com.netflix.glisten.impl.WorkflowMetaAttributes

/**
 * Created by ethomas on 10/6/14.
 */
class StartTimeoutPassableWorkflowClientFactory extends WorkflowClientFactory {
  StartTimeoutPassableWorkflowClientFactory(AmazonSimpleWorkflow simpleWorkflow, String domain, String taskList) {
    super(simpleWorkflow, domain, taskList)
  }

  StartTimeoutPassableWorkflowClientFactory(AmazonSimpleWorkflow simpleWorkflow, String domain) {
    super(simpleWorkflow, domain)
  }

  StartTimeoutPassableWorkflowClientFactory(AmazonSimpleWorkflow simpleWorkflow) {
    super(simpleWorkflow)
  }

  StartTimeoutPassableWorkflowClientFactory() {
  }

  public <T> InterfaceBasedWorkflowClient<T> getNewWorkflowClient(Class<T> workflow,
                                                                  WorkflowDescriptionTemplate workflowDescriptionTemplate, WorkflowTags tags = null, Long executionStartToCloseTimeoutSeconds = null, Long taskStartToCloseTimeoutSeconds = null) {
    WorkflowType workflowType = new WorkflowMetaAttributes(workflow).workflowType
    def factory = new WorkflowClientFactoryExternalBase<InterfaceBasedWorkflowClient>(simpleWorkflow, domain) {
      @Override
      protected InterfaceBasedWorkflowClient createClientInstance(WorkflowExecution workflowExecution,
                                                                  StartWorkflowOptions options, DataConverter dataConverter,
                                                                  GenericWorkflowClientExternal genericClient) {
        new InterfaceBasedWorkflowClient(workflow, workflowDescriptionTemplate, workflowExecution, workflowType,
          options, dataConverter, genericClient, tags)
      }
    }
    StartWorkflowOptions startWorkflowOptions = new StartWorkflowOptions(taskList: taskList)
    if (executionStartToCloseTimeoutSeconds) {
      startWorkflowOptions.executionStartToCloseTimeoutSeconds = executionStartToCloseTimeoutSeconds;
    } else if (Ats.from(workflow).has(WorkflowRegistrationOptions.class)) {
      startWorkflowOptions.executionStartToCloseTimeoutSeconds = Ats.from(workflow).get(WorkflowRegistrationOptions.class).defaultExecutionStartToCloseTimeoutSeconds();
    }
    if (taskStartToCloseTimeoutSeconds) {
      startWorkflowOptions.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
    } else if (Ats.from(workflow).has(WorkflowRegistrationOptions.class)) {
      startWorkflowOptions.taskStartToCloseTimeoutSeconds = Ats.from(workflow).get(WorkflowRegistrationOptions.class).defaultTaskStartToCloseTimeoutSeconds();
    }
    if (tags) {
      startWorkflowOptions.tagList = tags.constructTags()
    }
    factory.startWorkflowOptions = startWorkflowOptions
    factory.client
  }

}
