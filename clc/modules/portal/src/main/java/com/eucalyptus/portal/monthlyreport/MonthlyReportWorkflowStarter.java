/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.monthlyreport;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.portal.WorkflowClients;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflowClientExternal;
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarter;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;

public class MonthlyReportWorkflowStarter implements WorkflowStarter {
  private static Logger LOG     =
          Logger.getLogger(  MonthlyReportWorkflowStarter.class );

  public static final String BILLING_MONTHLY_REPORT_GENERATOR_WORKFLOW_ID =
          "billing-monthly-report-generator-workflow-01";
  @Override
  public void start() {
    try { // create or update monthly report
      // workflow runs in the beginning of a new day
      final Calendar yesterday = Calendar.getInstance();
      yesterday.set(Calendar.DAY_OF_YEAR, yesterday.get(Calendar.DAY_OF_YEAR) - 1);
      yesterday.set(Calendar.HOUR_OF_DAY, 23);
      yesterday.set(Calendar.MINUTE, 59);
      yesterday.set(Calendar.SECOND, 59);
      yesterday.set(Calendar.MILLISECOND, 0);

      final String year = String.format("%d", yesterday.get(Calendar.YEAR));
      final String month = String.format("%d", yesterday.get(Calendar.MONTH) + 1); /// calendar month starts from 0
      final Date reportUntil = yesterday.getTime();
      final MonthlyReportGeneratorWorkflowClientExternal workflow =
              WorkflowClients.getMonthlyReportGeneratorWorkflow(BILLING_MONTHLY_REPORT_GENERATOR_WORKFLOW_ID);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(720L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.generateMonthlyReport(year, month, reportUntil, options);
    } catch(final WorkflowExecutionAlreadyStartedException ex) {
      ;
    } catch(final Exception ex) {
      LOG.error("Failed to start the workflow that generates monthly usage reports");
    }
  }

  @Override
  public String name() {
    return "MONTHLY_REPORT_DAILY_WORKFLOW";
  }
}
