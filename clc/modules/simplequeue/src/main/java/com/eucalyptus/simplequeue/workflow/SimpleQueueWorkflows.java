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

import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.simplequeue.workflow.CloudWatchWorkflowClientExternal;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;


public class SimpleQueueWorkflows {
  private static Logger LOG  = Logger.getLogger( SimpleQueueWorkflows.class );

  public static void runCloudWatchWorkflow(final String workflowId) {
    try{
      final CloudWatchWorkflowClientExternal workflow =
        WorkflowClients.getCloudWatchWorkflowClient(workflowId);
      workflow.sendMetrics();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start simple queue service cloud watch metrics workflow", ex);
    }
  }

}
