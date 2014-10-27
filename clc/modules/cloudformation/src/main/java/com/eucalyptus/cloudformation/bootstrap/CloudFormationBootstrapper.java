/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.bootstrap;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;
import com.google.common.reflect.AbstractInvocationHandler;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 7/30/14.
 */
@Provides(CloudFormation.class)
@RunDuring(Bootstrap.Stage.Final)
@DependsLocal(CloudFormation.class)
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class CloudFormationBootstrapper extends Bootstrapper.Simple {

  @ConfigurableField(
      initial = "CloudFormationDomain",
      description = "The simple workflow service domain for cloudformation",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_DOMAIN = "CloudFormationDomain";

  @ConfigurableField(
      initial = "CloudFormationTaskList",
      description = "The simple workflow service task list for cloudformation",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "CloudFormationTaskList";

  @ConfigurableField(
      initial = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}",
      description = "JSON configuration for the cloudformation simple workflow client",
      changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

  @ConfigurableField(
      initial = "",
      description = "JSON configuration for the cloudformation simple workflow activity worker",
      changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = "";

  @ConfigurableField(
      initial = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8 }",
      description = "JSON configuration for the cloudformation simple workflow decision worker",
      changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8 }";

  // In case we are using AWS SWF
  private static boolean USE_AWS_SWF = "true".equalsIgnoreCase(System.getProperty("cloudformation.use_aws_swf"));
  private static String AWS_ACCESS_KEY = System.getProperty("cloudformation.aws_access_key", "");
  private static String AWS_SECRET_KEY= System.getProperty("cloudformation.aws_secret_key", "");

  private static volatile WorkflowClient workflowClient;

  public static synchronized AmazonSimpleWorkflow getSimpleWorkflowClient( ) {
    return workflowClient.getAmazonSimpleWorkflow( );
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
      final AmazonSimpleWorkflow simpleWorkflowClient;
      if (USE_AWS_SWF) {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        final BasicAWSCredentials creds = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        simpleWorkflowClient = new AmazonSimpleWorkflowClient(creds);
        simpleWorkflowClient.setRegion(Region.getRegion(Regions.US_EAST_1));

      } else {
        simpleWorkflowClient = Config.buildClient(
            CloudFormationAWSCredentialsProvider.CloudFormationUserSupplier.INSTANCE,
            SWF_CLIENT_CONFIG );
      }

      final AmazonSimpleWorkflow oldClient =
          workflowClient != null ? workflowClient.getAmazonSimpleWorkflow( ) : null;

      workflowClient = new WorkflowClient(
          CloudFormation.class,
          simpleWorkflowClient,
          SWF_DOMAIN,
          SWF_TASKLIST,
          SWF_WORKFLOW_WORKER_CONFIG,
          SWF_ACTIVITY_WORKER_CONFIG );

      if ( oldClient != null ) try {
        oldClient.shutdown( );
      } catch ( final Exception e ) {
        LOG.error( "Error shutting down simple workflow client", e );
      }

      workflowClient.start( );

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
      if ( workflowClient != null ) {
        workflowClient.stop( );
      }
      return true;
    } catch (Exception ex) {
      LOG.error("Unable to disable CloudFormation Bootstrapper", ex);
      return false;
    }
  }

}

