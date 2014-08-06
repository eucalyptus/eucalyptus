package com.eucalyptus.cloudformation.bootstrap;

import com.eucalyptus.cloudformation.workflow.CreateStackWorkflow;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflow;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.netflix.glisten.impl.local.LocalWorkflowOperations;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 8/5/14.
 */
public class LocalWorkflowProvider implements WorkflowProvider {

  private static final Logger LOG = Logger.getLogger(LocalWorkflowProvider.class);
  @Override
  public void startup() throws Exception {
    // Nothing to do in local mode...
  }

  @Override
  public void shutdown() throws Exception {
    // Nothing to do in local mode...
  }

  @Override
  public CreateStackWorkflow getCreateStackWorkflow() {
    LOG.info("Using local mode for create");
    CreateStackWorkflowImpl createStackWorkflow = new CreateStackWorkflowImpl();
    createStackWorkflow.setWorkflowOperations(LocalWorkflowOperations.<StackActivity>of(new StackActivityImpl()));
    return createStackWorkflow;
  }

  @Override
  public DeleteStackWorkflow getDeleteStackWorkflow() {
    LOG.info("Using local mode for delete");
    DeleteStackWorkflowImpl deleteStackWorkflow = new DeleteStackWorkflowImpl();
    deleteStackWorkflow.setWorkflowOperations(LocalWorkflowOperations.<StackActivity>of(new StackActivityImpl()));
    return deleteStackWorkflow;
  }
}
