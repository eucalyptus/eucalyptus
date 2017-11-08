/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
