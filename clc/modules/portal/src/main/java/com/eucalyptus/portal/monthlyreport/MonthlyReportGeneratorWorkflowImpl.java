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

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.BillingWorkflowState;
import com.eucalyptus.portal.workflow.MonthlyReportActivitiesClient;
import com.eucalyptus.portal.workflow.MonthlyReportActivitiesClientImpl;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflow;
import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import com.eucalyptus.simpleworkflow.common.client.Daily;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import java.util.Date;
import java.util.List;

// Delay execution after 5 minute of a new day because monthly report depends on AWS usage report
// which is being updated at 0 minute of every hour.
@Daily(value = MonthlyReportWorkflowStarter.class, hour = 0, minute = 5)
@ComponentPart(Portal.class)
public class MonthlyReportGeneratorWorkflowImpl implements MonthlyReportGeneratorWorkflow {
  private static Logger LOG     =
          Logger.getLogger(  MonthlyReportGeneratorWorkflowImpl.class );
  final MonthlyReportActivitiesClient client =
          new MonthlyReportActivitiesClientImpl();
  private BillingWorkflowState state =
          BillingWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
  private String year = null;
  private String month = null;
  private Date reportUntil = null;

  @Override
  public void generateMonthlyReport(final String year, final String month, final Date until) {
    this.year = year;
    this.month = month;
    this.reportUntil = until;

    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        doGenerate();
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        // no cleanup is necessary
        state = BillingWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow generating monthly AWS usage report has failed: ", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == BillingWorkflowState.WORKFLOW_RUNNING)
          state = BillingWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  public void doGenerate() {
    final Promise<List<String>> accounts = client.listCandidateAccounts();
    generateForAccounts(accounts);
  }

  @Asynchronous
  public void generateForAccounts(Promise<List<String>> accounts) {
    for (final String accountId : accounts.get()) {
      Promise<Void> run = Promise.Void();
      for (final String service : Lists.newArrayList("AmazonEC2", "AmazonS3")) {
        final Promise<List<MonthlyUsageRecord>> records = client.transform(
                client.queryMonthlyAwsUsage(accountId, service, this.year, this.month, this.reportUntil)
        );
        run = client.persist(
                Promise.asPromise(accountId),
                Promise.asPromise(this.year),
                Promise.asPromise(this.month),
                records,
                run    // persist is serialized
        );
        client.uploadToS3Bucket(
                Promise.asPromise(accountId),
                Promise.asPromise(this.year),
                Promise.asPromise(this.month),
                run);
      }
    }
  }

  @Override
  public BillingWorkflowState getState() {
    return state;
  }
}
