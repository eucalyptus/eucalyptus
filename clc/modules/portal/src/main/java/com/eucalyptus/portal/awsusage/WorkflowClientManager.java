/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.awsusage;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;
import org.apache.log4j.Logger;

public class WorkflowClientManager {
  private static Logger LOG  = Logger.getLogger( WorkflowClientManager.class );

  private static volatile WorkflowClient workflowClient;
  public static synchronized AmazonSimpleWorkflow getSimpleWorkflowClient( ) {
    return workflowClient.getAmazonSimpleWorkflow( );
  }

  private static final String TASK_LIST = BillingProperties.SWF_TASKLIST;
  private static final String DOMAIN = BillingProperties.SWF_DOMAIN;

  public static boolean isRunning() {
    return workflowClient != null && workflowClient.isRunning();
  }

  public static void start( ) throws Exception {
    final AmazonSimpleWorkflow simpleWorkflowClient;
    simpleWorkflowClient = Config.buildClient(
            BillingAWSCredentialsProvider.BillingUserSupplier.INSTANCE,
            BillingProperties.SWF_CLIENT_CONFIG );

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
