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

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;

public class BillingWorkflows implements EventListener<Hertz> {
  private static Logger LOG  = Logger.getLogger( BillingWorkflows.class );
  private static final String BILLING_AWS_USAGE_AGGREGATE_WORKFLOW_ID =
          "billing-aws-usage-aggregate-workflow-01";
  public static final String BILLING_RESOURCE_USAGE_EVENT_WORKFLOW_ID =
          "billing-resource-usage-event-workflow-01";
  public static void register() {
    Listeners.register( Hertz.class, new BillingWorkflows() );
  }

  public static void runAwsUsageAggregateWorkflow(final String workflowId) {

    try{
      final AwsUsageHoulyAggregateWorkflowClientExternal workflow =
              WorkflowClients.getAwsUsageHourlyAggregateWorkflow(workflowId);
      workflow.aggregate();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start the workflow for aggregating AWS usage report", ex);
    }
  }

  public static void runResourceUsageEventWorkflow(final String workflowId) {
    try{
      final ResourceUsageEventWorkflowClientExternal workflow =
              WorkflowClients.getResourceUsageEventWorkflow(workflowId);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(7200L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.fireEvents(options);
    }catch(final WorkflowExecutionAlreadyStartedException ex) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start the workflow that fires resource usage events");
    }
  }

  private static long lastRecordedTime = System.currentTimeMillis();

  @Override
  public void fireEvent(Hertz event) {
    if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController()) {
      return;
    }

    final long currentTime = System.currentTimeMillis();
    final Calendar prev = Calendar.getInstance();
    final Calendar cur = Calendar.getInstance();
    prev.setTime(new Date(lastRecordedTime));
    cur.setTime(new Date(currentTime));
    lastRecordedTime = currentTime;
    if( cur.get(Calendar.HOUR_OF_DAY) != prev.get(Calendar.HOUR_OF_DAY) ) {
      try {
        runAwsUsageAggregateWorkflow( BILLING_AWS_USAGE_AGGREGATE_WORKFLOW_ID );
      } catch( final Exception ex) {
        LOG.error(ex, ex);
      }
    }
  }
}
