package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientFactoryExternalBase
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType
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
    }
    if (taskStartToCloseTimeoutSeconds) {
      startWorkflowOptions.taskStartToCloseTimeoutSeconds = taskStartToCloseTimeoutSeconds;
    }
    if (tags) {
      startWorkflowOptions.tagList = tags.constructTags()
    }
    factory.startWorkflowOptions = startWorkflowOptions
    factory.client
  }

}
