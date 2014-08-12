package com.eucalyptus.cloudformation.bootstrap;

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
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.netflix.glisten.InterfaceBasedWorkflowClient;
import com.netflix.glisten.WorkflowClientFactory;
import com.netflix.glisten.WorkflowDescriptionTemplate;
import com.netflix.glisten.WorkflowTags;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 8/5/14.
 */
public class SWFWorkflowProvider implements WorkflowProvider {

  // TODO: consider new variables...
  private static String SWF_DOMAIN = System.getProperty("cloudformation.swf_domain", "CloudFormationDomain");
  private static String SWF_TASKLIST = System.getProperty("cloudformation.swf_tasklist", "CloudFormationTaskList");
  private static final Logger LOG = Logger.getLogger(SWFWorkflowProvider.class);

  private volatile AmazonSimpleWorkflowClient simpleWorkflowClient;
  private volatile WorkflowWorker stackWorkflowWorker;
  private volatile ActivityWorker stackActivityWorker;

  @Override
  public void startup() throws Exception {
    if (!Topology.isEnabled(SimpleWorkflow.class)) {
      throw new Exception("SimpleWorkflow must be enabled/registered to use Eucalyptus SWF");
    }
    simpleWorkflowClient = new AmazonSimpleWorkflowClient( new CloudFormationAWSCredentialsProvider( ) );
    simpleWorkflowClient.setEndpoint(ServiceUris.remote(Topology.lookup(SimpleWorkflow.class)).toString());
    try {
      simpleWorkflowClient.registerDomain( new RegisterDomainRequest( )
        .withName( SWF_DOMAIN )
        .withWorkflowExecutionRetentionPeriodInDays( "1" )
      );
    } catch (DomainAlreadyExistsException ex) {
      LOG.debug("SWF domain " + SWF_DOMAIN + " already exists");
    }

    stackWorkflowWorker = new WorkflowWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    stackWorkflowWorker.addWorkflowImplementationType(CreateStackWorkflowImpl.class);
    stackWorkflowWorker.addWorkflowImplementationType(DeleteStackWorkflowImpl.class);
    stackWorkflowWorker.start( );

    stackActivityWorker = new ActivityWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    stackActivityWorker.addActivitiesImplementation(new StackActivityImpl());
    stackActivityWorker.start( );
  }

  @Override
  public void shutdown() {
    if (stackWorkflowWorker != null) {
      stackWorkflowWorker.shutdown();
    }
    if (stackActivityWorker != null) {
      stackActivityWorker.shutdown();
    }
  }

  @Override
  public CreateStackWorkflow getCreateStackWorkflow() {
    final WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    final WorkflowDescriptionTemplate workflowDescriptionTemplate = new CreateStackWorkflowDescriptionTemplate();
    final InterfaceBasedWorkflowClient<CreateStackWorkflow> client = workflowClientFactory
      .getNewWorkflowClient(CreateStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());
    return new CreateStackWorkflowClient(client);
  }

  @Override
  public DeleteStackWorkflow getDeleteStackWorkflow() {
    final WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
    final WorkflowDescriptionTemplate workflowDescriptionTemplate = new DeleteStackWorkflowDescriptionTemplate();
    final InterfaceBasedWorkflowClient<DeleteStackWorkflow> client = workflowClientFactory
      .getNewWorkflowClient(DeleteStackWorkflow.class, workflowDescriptionTemplate, new WorkflowTags());
    return new DeleteStackWorkflowClient(client);
  }
}
