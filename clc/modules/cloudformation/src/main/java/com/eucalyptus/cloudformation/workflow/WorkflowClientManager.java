/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.eucalyptus.cloudformation.common.CloudFormation;
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
