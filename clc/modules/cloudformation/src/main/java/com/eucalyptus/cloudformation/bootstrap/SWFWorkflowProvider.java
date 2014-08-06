package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflow;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowDescriptionTemplate;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflow;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowClient;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowDescriptionTemplate;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowClientFactory;
import com.netflix.glisten.WorkflowDescriptionTemplate;
import com.netflix.glisten.WorkflowTags;
import com.netflix.glisten.impl.local.LocalWorkflowOperations;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 8/5/14.
 */
public class SWFWorkflowProvider implements WorkflowProvider {

  WorkflowWorker createStackWorkflowWorker = null;
  WorkflowWorker deleteStackWorkflowWorker = null;
  ActivityWorker stackActivityWorker = null;

  // TODO: consider new variables...
  private static String SWF_DOMAIN = System.getProperty("cloudformation.swf_domain", "CloudFormationDomain");
  private static String SWF_TASKLIST = System.getProperty("cloudformation.swf_tasklist", "CloudFormationTaskList");
  private static final Logger LOG = Logger.getLogger(SWFWorkflowProvider.class);
  AmazonSimpleWorkflowClient simpleWorkflowClient;
  AWSCredentials creds;
  @Override
  public void startup() throws Exception {
    if (!Topology.isEnabled(SimpleWorkflow.class)) {
      throw new Exception("SimpleWorkflow must be enabled/registered to use Eucalyptus SWF");
    }
    creds = new CloudFormationAWSCredentialsProvider().getCredentials();
    if (creds == null) {
      throw new Exception("Unable to get credentials for cloudformation user");
    }
    simpleWorkflowClient = new AmazonSimpleWorkflowClient(creds);
    simpleWorkflowClient.setEndpoint(ServiceUris.remote(Topology.lookup(SimpleWorkflow.class)).toString());
    try {
      RegisterDomainRequest registerDomainRequest = new RegisterDomainRequest();
      registerDomainRequest.setName(SWF_DOMAIN);
      registerDomainRequest.setWorkflowExecutionRetentionPeriodInDays("1");
      simpleWorkflowClient.registerDomain(registerDomainRequest);
    } catch (DomainAlreadyExistsException ex) {
      LOG.debug("SWF domain " + SWF_DOMAIN + " already exists");
    }
    // Only here due to a bug in SWF not allowing two workflow workers at a time (TODO: remove)
    if (!"true".equalsIgnoreCase(System.getProperty("cloudformation.suppress_swf_create"))) { // TODO: remove!
      createStackWorkflowWorker = new WorkflowWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
      createStackWorkflowWorker.addWorkflowImplementationType(CreateStackWorkflowImpl.class);
      createStackWorkflowWorker.start();
    }
    // Only here due to a bug in SWF not allowing two workflow workers at a time (TODO: remove)
    if (!"true".equalsIgnoreCase(System.getProperty("cloudformation.suppress_swf_delete"))) {
      deleteStackWorkflowWorker = new WorkflowWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
      deleteStackWorkflowWorker.addWorkflowImplementationType(DeleteStackWorkflowImpl.class);
      deleteStackWorkflowWorker.start();
    }
    stackActivityWorker = new ActivityWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    stackActivityWorker.addActivitiesImplementation(new StackActivityImpl());
    stackActivityWorker.start();
  }

  @Override
  public void shutdown() {
    if (createStackWorkflowWorker != null) {
      createStackWorkflowWorker.shutdown();
    }
    if (deleteStackWorkflowWorker != null) {
      deleteStackWorkflowWorker.shutdown();
    }
    if (stackActivityWorker != null) {
      stackActivityWorker.shutdown();
    }
  }

  @Override
  public CreateStackWorkflow getCreateStackWorkflow() {
    CreateStackWorkflow createStackWorkflow;
    if (createStackWorkflowWorker != null) {
      LOG.info("Using SWF mode for create");
      WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
      WorkflowDescriptionTemplate workflowDescriptionTemplate = new CreateStackWorkflowDescriptionTemplate();
      InterfaceBasedWorkflowClient<CreateStackWorkflow> client = workflowClientFactory
        .getNewWorkflowClient(CreateStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());

      createStackWorkflow = new CreateStackWorkflowClient(client);
    } else {
      LOG.info("Using local mode for create (in SWF)");
      createStackWorkflow = new CreateStackWorkflowImpl();
      ((CreateStackWorkflowImpl) createStackWorkflow).setWorkflowOperations(LocalWorkflowOperations.<StackActivity>of(new StackActivityImpl()));
    }
    return createStackWorkflow;
  }

  @Override
  public DeleteStackWorkflow getDeleteStackWorkflow() {
    DeleteStackWorkflow deleteStackWorkflow;
    if (deleteStackWorkflowWorker != null) {
      LOG.info("Using SWF mode for delete");
      WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
      WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
      InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
        .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());

      deleteStackWorkflow = new DeleteStackWorkflowClient(client);
    } else {
      LOG.info("Using local mode for delete (in SWF)");
      deleteStackWorkflow = new DeleteStackWorkflowImpl();
      ((DeleteStackWorkflowImpl) deleteStackWorkflow).setWorkflowOperations(LocalWorkflowOperations.<StackActivity>of(new StackActivityImpl()));
    }
    return deleteStackWorkflow;
  }
}
