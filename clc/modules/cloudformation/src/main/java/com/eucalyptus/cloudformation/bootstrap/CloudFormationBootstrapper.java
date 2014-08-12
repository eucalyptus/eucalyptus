package com.eucalyptus.cloudformation.bootstrap;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudformation.CloudFormation;
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

  // TODO: centralize these items (change property names and move elsewhere)
  private static boolean USE_SWF = !"false".equalsIgnoreCase(System.getProperty("cloudformation.use_swf"));

  private static final WorkflowProvider workflowProvider = loadWorkflowProvider();

  private static WorkflowProvider loadWorkflowProvider() {
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

