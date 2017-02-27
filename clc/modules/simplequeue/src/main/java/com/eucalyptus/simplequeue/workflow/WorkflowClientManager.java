/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/

package com.eucalyptus.simplequeue.workflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.bootstrap.SimpleQueueAWSCredentialsProvider;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.simpleworkflow.common.client.WorkflowClient;
import org.apache.log4j.Logger;

/**
 *
 */
public class WorkflowClientManager {
  private static Logger LOG  = Logger.getLogger( WorkflowClientManager.class );

  private static volatile WorkflowClient workflowClient;
  public static synchronized AmazonSimpleWorkflow getSimpleWorkflowClient( ) {
    return workflowClient.getAmazonSimpleWorkflow( );
  }

  public static boolean isRunning() {
    return workflowClient != null && workflowClient.isRunning();
  }

  public static void start( ) throws Exception {
    final AmazonSimpleWorkflow simpleWorkflowClient = Config.buildClient(
      SimpleQueueAWSCredentialsProvider.SimpleQueueUserSupplier.INSTANCE
    );

  workflowClient = new WorkflowClient(
    SimpleQueue.class,
    simpleWorkflowClient,
    SimpleQueueProperties.SWF_DOMAIN,
    SimpleQueueProperties.SWF_TASKLIST,
    SimpleQueueProperties.SWF_WORKFLOW_WORKER_CONFIG,
    SimpleQueueProperties.SWF_ACTIVITY_WORKER_CONFIG );

  workflowClient.start( );

    LOG.debug("SimpleQueueService SWF client has started");
  }

  public static void stop( ) throws Exception {
    if ( workflowClient != null ) {
      workflowClient.stop( );
    }
  }

}
