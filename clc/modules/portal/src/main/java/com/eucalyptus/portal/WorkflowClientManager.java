/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;
import org.apache.log4j.Logger;

public class WorkflowClientManager {
  private static Logger LOG  = Logger.getLogger( WorkflowClientManager.class );

  private static volatile WorkflowClient workflowClient;

  private static final String TASK_LIST = BillingProperties.SWF_TASKLIST;
  private static final String DOMAIN = BillingProperties.SWF_DOMAIN;

  public static boolean isRunning() {
    return workflowClient != null && workflowClient.isRunning();
  }

  public static void start( ) throws Exception {
    final AmazonSimpleWorkflow simpleWorkflowClient;
    simpleWorkflowClient = Config.buildClient(
            BillingAWSCredentialsProvider.BillingUserSupplier.INSTANCE
    );

    workflowClient = new WorkflowClient(
            Portal.class,
            simpleWorkflowClient,
            DOMAIN,
            TASK_LIST,
            BillingProperties.SWF_WORKFLOW_WORKER_CONFIG,
            BillingProperties.SWF_ACTIVITY_WORKER_CONFIG );
    workflowClient.start( );
    LOG.debug("Billing SWF client has started");
  }

  public static void stop( ) throws Exception {
    if ( workflowClient != null ) {
      workflowClient.stop( );
    }
  }
}
