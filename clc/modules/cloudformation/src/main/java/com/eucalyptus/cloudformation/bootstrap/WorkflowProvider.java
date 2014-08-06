package com.eucalyptus.cloudformation.bootstrap;

import com.eucalyptus.cloudformation.workflow.CreateStackWorkflow;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflow;

/**
 * Created by ethomas on 8/5/14.
 */
public interface WorkflowProvider {

  public void startup() throws Exception;
  public void shutdown() throws Exception;
  public CreateStackWorkflow getCreateStackWorkflow();
  public DeleteStackWorkflow getDeleteStackWorkflow();

}
