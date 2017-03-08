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
package com.eucalyptus.portal;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageDailyAggregateWorkflowClientExternal;
import com.eucalyptus.portal.workflow.AwsUsageHourlyAggregateWorkflowClientExternal;
import com.eucalyptus.portal.workflow.BillingWorkflowState;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflowClientExternal;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflowClientExternal;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Supplier;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class BillingWorkflows implements EventListener<Hertz> {
  private static Logger LOG  = Logger.getLogger( BillingWorkflows.class );
  private static final String BILLING_AWS_USAGE_HOURLY_AGGREGATE_WORKFLOW_ID =
          "billing-aws-usage-hourly-aggregate-workflow-01";
  private static final String BILLING_AWS_USAGE_DAILY_AGGREGATE_WORKFLOW_ID =
          "billing-aws-usage-daily-aggregate-workflow-01";
  public static final String BILLING_RESOURCE_USAGE_EVENT_WORKFLOW_ID =
          "billing-resource-usage-event-workflow-01";
  public static final String BILLING_MONTHLY_REPORT_GENERATOR_WORKFLOW_ID =
          "billing-monthly-report-generator-workflow-01";
  public static void register() {
    Listeners.register( Hertz.class, new BillingWorkflows() );
  }

  public static void runAwsUsageHourlyAggregateWorkflow(final String workflowId) {
    try{
      final AwsUsageHourlyAggregateWorkflowClientExternal workflow =
              WorkflowClients.getAwsUsageHourlyAggregateWorkflow(workflowId);
      workflow.aggregateHourly();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start the workflow for aggregating hourly AWS usage report", ex);
    }
  }

  public static void runAwsUsageDailyAggregateWorkflow(final String workflowId) {
    try{
      final AwsUsageDailyAggregateWorkflowClientExternal workflow =
              WorkflowClients.getAwsUsageDailyAggregateWorkflow(workflowId);
      workflow.aggregateDaily();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start the workflow for aggregating daily AWS usage report", ex);
    }
  }

  public static void runAwsUsageDailyAggregateWorkflowSync(final String workflowId) throws Exception {
    final Future<BillingWorkflowException> task =
            Threads.enqueue(Portal.class, BillingWorkflows.class,  runAwsUsageDailyAggregateWorkflowImpl( workflowId ));
    final BillingWorkflowException ex = task.get();
    if (ex != null) {
      throw ex;
    }
  }

  public static Callable<BillingWorkflowException> runAwsUsageDailyAggregateWorkflowImpl (final String workflowId) {
    return new Callable<BillingWorkflowException>() {
      @Override
      public BillingWorkflowException call() throws Exception {
        final AwsUsageDailyAggregateWorkflowClientExternal workflow =
                WorkflowClients.getAwsUsageDailyAggregateWorkflow(workflowId);
        workflow.aggregateDaily();
        try {
          waitForException(new Supplier<BillingWorkflowState>() {
            @Override
            public BillingWorkflowState get() {
              return workflow.getState();
            }
          });
        } catch (final BillingWorkflowException ex) {
          return ex;
        } catch (final Exception ex) {
          return new BillingWorkflowException(null, ex);
        }
        return null;
      }
    };
  }

  private static void waitForException(Supplier<BillingWorkflowState> supplier) throws BillingWorkflowException {
    BillingWorkflowState state = BillingWorkflowState.WORKFLOW_RUNNING;
    do {
      try{
        Thread.sleep(500);
      }catch(final Exception ex){
        ;
      }
      state = supplier.get();
    } while (state == null || state == BillingWorkflowState.WORKFLOW_RUNNING );

    if (BillingWorkflowState.WORKFLOW_FAILED.equals(state)) {
      final int statusCode = state.getStatusCode();
      final String reason = state.getReason();
      throw new BillingWorkflowException(reason, statusCode);
    } else if (BillingWorkflowState.WORKFLOW_CANCELLED.equals(state)) {
      throw new BillingWorkflowException("Cancelled workflow");
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

  public static void runMonthlyReportGeneratorWorkflow(final String workflowId,
                                                       final String year, final String month, final Date reportUntil) {
    try {
      final MonthlyReportGeneratorWorkflowClientExternal workflow =
              WorkflowClients.getMonthlyReportGeneratorWorkflow(workflowId);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(720L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.generateMonthlyReport(year, month, reportUntil, options);
    } catch(final WorkflowExecutionAlreadyStartedException ex) {
      ;
    }catch(final Exception ex) {
      throw Exceptions.toUndeclared("Failed to start the workflow that generates monthly usage reports");
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

    // IT'S A NEW DAY!
    if ( cur.get(Calendar.DAY_OF_MONTH) != prev.get(Calendar.DAY_OF_MONTH) ) {
      try {
        // Aws usage reports must be generated before monthly report is generated (hence sync)
        runAwsUsageDailyAggregateWorkflowSync( BILLING_AWS_USAGE_DAILY_AGGREGATE_WORKFLOW_ID );
      } catch ( final Exception ex) {
        LOG.error(ex, ex);
      }

      try { // create or update monthly report
        final String year = String.format("%d", prev.get(Calendar.YEAR));
        final String month = String.format("%d", prev.get(Calendar.MONTH) + 1); /// calendar month starts from 0
        final Date reportUntil = prev.getTime();
        runMonthlyReportGeneratorWorkflow(BILLING_MONTHLY_REPORT_GENERATOR_WORKFLOW_ID, year, month, reportUntil);
      } catch ( final Exception ex) {
        LOG.error(ex, ex);
      }
    } else if (cur.get(Calendar.HOUR_OF_DAY) != prev.get(Calendar.HOUR_OF_DAY) ) {
      // IT'S A NEW HOUR!
      try {
        runAwsUsageHourlyAggregateWorkflow( BILLING_AWS_USAGE_HOURLY_AGGREGATE_WORKFLOW_ID );
      } catch( final Exception ex) {
        LOG.error(ex, ex);
      }
    }
  }
}
