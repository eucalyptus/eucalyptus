package com.eucalyptus.cloudformation.bootstrap;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker;
import com.amazonaws.services.simpleworkflow.model.DomainAlreadyExistsException;
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivityImpl;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.client.GenericS3ClientFactory;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 7/30/14.
 */
@Provides(CloudFormation.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(CloudFormation.class)
public class CloudFormationBootstrapper extends Bootstrapper.Simple {

  // TODO: centralize these items (change property names and move elsewhere)
  private static boolean USE_SWF = "true".equalsIgnoreCase(System.getProperty("cloudformation.use_swf"));

  private static final WorkflowProvider workflowProvider = loadWorkflowProvider();

  private static final WorkflowProvider loadWorkflowProvider() {
    if (USE_SWF) {
      return new SWFWorkflowProvider();
    } else {
      return new LocalWorkflowProvider();
    }
  }

  public static synchronized WorkflowProvider getWorkflowProvider() {

    return workflowProvider;
  }

  private static final Logger LOG = Logger.getLogger(CloudFormationBootstrapper.class);

  @Override
  public boolean check() throws Exception {
    if (USE_SWF) {
      if (!Topology.isEnabled(SimpleWorkflow.class)) {
        throw new Exception("SimpleWorkflow must be enabled/registered to use Eucalyptus SWF");
      }
    }
    return true;
  }

  @Override
  public boolean enable() throws Exception {
    try {
      if (!super.enable()) return false;
      getWorkflowProvider().startup();
      return true;
    } catch (Exception ex) {
      LOG.error("Unable to enable CloudFormation Bootstrapper", ex);
      return false;
    }
  }

  @Override
  public boolean disable() throws Exception {
    try {
      if (!super.disable()) return false;
      getWorkflowProvider().shutdown();
      return true;
    } catch (Exception ex) {
      LOG.error("Unable to disable CloudFormation Bootstrapper", ex);
      return false;
    }
  }
}

