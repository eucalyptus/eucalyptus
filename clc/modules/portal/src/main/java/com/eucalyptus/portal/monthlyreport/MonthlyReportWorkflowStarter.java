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
