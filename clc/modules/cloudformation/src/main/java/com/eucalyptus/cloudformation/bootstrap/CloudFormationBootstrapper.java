package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.MonitorCreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 7/30/14.
 */
@Provides(CloudFormation.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(CloudFormation.class)
public class CloudFormationBootstrapper extends Bootstrapper.Simple {

  public static String SWF_DOMAIN = System.getProperty("cloudformation.swf_domain", "CloudFormationDomain");
  public static String SWF_TASKLIST = System.getProperty("cloudformation.swf_tasklist", "CloudFormationTaskList");

  // In case we are using AWS SWF
  private static boolean USE_AWS_SWF = "true".equalsIgnoreCase(System.getProperty("cloudformation.use_aws_swf"));
  private static String AWS_ACCESS_KEY = System.getProperty("cloudformation.aws_access_key", "");
  private static String AWS_SECRET_KEY= System.getProperty("cloudformation.aws_secret_key", "");



  private static volatile AmazonSimpleWorkflowClient simpleWorkflowClient;
  private static volatile WorkflowWorker stackWorkflowWorker;
  private static volatile ActivityWorker stackActivityWorker;

  public static synchronized AmazonSimpleWorkflowClient getSimpleWorkflowClient() {
    return simpleWorkflowClient;
  }

  private static final Logger LOG = Logger.getLogger(CloudFormationBootstrapper.class);

  @Override
  public boolean check() throws Exception {
    if (!Topology.isEnabled(SimpleWorkflow.class)) {
      throw new Exception("SimpleWorkflow must be enabled/registered to use Eucalyptus SWF");
    }
    return true;
  }

  @Override
  public synchronized boolean enable() throws Exception {
    try {
      if (!super.enable()) return false;
      if (USE_AWS_SWF) {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        BasicAWSCredentials creds = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        if (creds == null) {
          throw new Exception("Unable to get credentials for cloudformation user");
        }
        simpleWorkflowClient = new AmazonSimpleWorkflowClient(creds);
        simpleWorkflowClient.setRegion(Region.getRegion(Regions.US_EAST_1));

      } else {
        simpleWorkflowClient = new AmazonSimpleWorkflowClient(new CloudFormationAWSCredentialsProvider());
        simpleWorkflowClient.setEndpoint(ServiceUris.remote(Topology.lookup(SimpleWorkflow.class)).toString());
      }
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
      stackWorkflowWorker.addWorkflowImplementationType(MonitorCreateStackWorkflowImpl.class);
      stackWorkflowWorker.addWorkflowImplementationType(DeleteStackWorkflowImpl.class);
      stackWorkflowWorker.start( );

      stackActivityWorker = new ActivityWorker(simpleWorkflowClient, SWF_DOMAIN, SWF_TASKLIST);
      stackActivityWorker.addActivitiesImplementation(new StackActivityImpl());
      stackActivityWorker.start( );
      return true;
    } catch (Exception ex) {
      LOG.error("Unable to enable CloudFormation Bootstrapper", ex);
      return false;
    }
  }

  @Override
  public synchronized boolean disable() throws Exception {
    try {
      if (!super.disable()) return false;
      if (stackWorkflowWorker != null) {
        stackWorkflowWorker.shutdown();
      }
      if (stackActivityWorker != null) {
        stackActivityWorker.shutdown();
      }
      return true;
    } catch (Exception ex) {
      LOG.error("Unable to disable CloudFormation Bootstrapper", ex);
      return false;
    }
  }
}

