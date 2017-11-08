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
