package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
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
public class AWSSWFWorkflowProvider implements WorkflowProvider {

  WorkflowWorker stackWorkflowWorker = null;
  ActivityWorker stackActivityWorker = null;

  // TODO: consider new variables...
  private static String SWF_DOMAIN = System.getProperty("cloudformation.swf_domain", "CloudFormationDomain");
  private static String SWF_TASKLIST = System.getProperty("cloudformation.swf_tasklist", "CloudFormationTaskList");
  private static String AWS_ACCESS_KEY = System.getProperty("cloudformation.aws_access_key", "");
  private static String AWS_SECRET_KEY= System.getProperty("cloudformation.aws_secret_key", "");

  private static final Logger LOG = Logger.getLogger(AWSSWFWorkflowProvider.class);
  AmazonSimpleWorkflowClient simpleWorkflowClient;
  AWSCredentials creds;
  @Override
  public void startup() throws Exception {
    System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
    creds = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
    if (creds == null) {
      throw new Exception("Unable to get credentials for cloudformation user");
    }
    simpleWorkflowClient = new AmazonSimpleWorkflowClient(creds);
    simpleWorkflowClient.setRegion(Region.getRegion(Regions.US_EAST_1));
    try {
      RegisterDomainRequest registerDomainRequest = new RegisterDomainRequest();
      registerDomainRequest.setName(SWF_DOMAIN);
      registerDomainRequest.setWorkflowExecutionRetentionPeriodInDays("1");
      simpleWorkflowClient.registerDomain(registerDomainRequest);
    } catch (DomainAlreadyExistsException ex) {
      LOG.debug("SWF domain " + SWF_DOMAIN + " already exists");
    }
    stackWorkflowWorker = new WorkflowWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    stackWorkflowWorker.addWorkflowImplementationType(CreateStackWorkflowImpl.class);
    stackWorkflowWorker.addWorkflowImplementationType(DeleteStackWorkflowImpl.class);
    stackWorkflowWorker.start();

    stackActivityWorker = new ActivityWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    stackActivityWorker.addActivitiesImplementation(new StackActivityImpl());
    stackActivityWorker.start();
  }

  @Override
  public void shutdown() {
    stackWorkflowWorker.shutdown();
    stackActivityWorker.shutdown();
  }

  @Override
  public CreateStackWorkflow getCreateStackWorkflow() {
    CreateStackWorkflow createStackWorkflow;
    LOG.info("Using AWS SWF mode for create");
    WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    WorkflowDescriptionTemplate workflowDescriptionTemplate = new CreateStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<CreateStackWorkflow> client = workflowClientFactory
      .getNewWorkflowClient(CreateStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());

    createStackWorkflow = new CreateStackWorkflowClient(client);
    return createStackWorkflow;
  }

  @Override
  public DeleteStackWorkflow getDeleteStackWorkflow() {
    DeleteStackWorkflow deleteStackWorkflow;
    LOG.info("Using AWS SWF mode for delete");
    WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
    InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
      .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());
    deleteStackWorkflow = new DeleteStackWorkflowClient(client);
    return deleteStackWorkflow;
  }
}