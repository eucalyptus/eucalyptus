/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.cloudformation.bootstrap.CloudFormationAWSCredentialsProvider;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;

import static com.eucalyptus.cloudformation.config.CloudFormationProperties.*;

/**
 *
 */
public class WorkflowClientManager {

  private static volatile WorkflowClient workflowClient;

  public static synchronized AmazonSimpleWorkflow getSimpleWorkflowClient( ) {
    return workflowClient.getAmazonSimpleWorkflow( );
  }

  public static void start( ) throws Exception {
    final AmazonSimpleWorkflow simpleWorkflowClient;
    if ( USE_AWS_SWF ) {
      System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
      final BasicAWSCredentials creds = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
      simpleWorkflowClient = new AmazonSimpleWorkflowClient(creds);
      simpleWorkflowClient.setRegion( Region.getRegion( Regions.US_EAST_1 ));

    } else {
      simpleWorkflowClient = Config.buildClient(
          CloudFormationAWSCredentialsProvider.CloudFormationUserSupplier.INSTANCE
      );
    }

    workflowClient = new WorkflowClient(
        CloudFormation.class,
        simpleWorkflowClient,
        SWF_DOMAIN,
        SWF_TASKLIST,
        SWF_WORKFLOW_WORKER_CONFIG,
        SWF_ACTIVITY_WORKER_CONFIG );

    workflowClient.start( );

  }

  public static void stop( ) throws Exception {
    if ( workflowClient != null ) {
      workflowClient.stop( );
    }
  }
}
