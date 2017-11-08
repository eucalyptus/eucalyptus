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

import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.BucketUploadableActivities;
import com.eucalyptus.portal.S3UploadException;
import com.eucalyptus.portal.awsusage.AwsUsageRecords;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.workflow.BillingActivityException;
import com.eucalyptus.portal.workflow.MonthlyReportActivities;
import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@ComponentPart(Portal.class)
public class MonthlyReportActivitiesImpl extends BucketUploadableActivities implements MonthlyReportActivities {
  private static Logger LOG     =
          Logger.getLogger(  MonthlyReportActivitiesImpl.class );

  @Override
  public List<String> listCandidateAccounts() throws BillingActivityException {
    try {
      return Accounts.listAllAccounts().stream() // TODO: should we filter out system accounts?
              .map(e -> e.getAccountNumber())
              .collect(Collectors.toList());
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to list candidate accounts");
    }
  }

  @Override
  public List<AwsUsageRecord> queryMonthlyAwsUsage(final String accountId, final String service,
                                                   final String year, final String month, final Date queryEnd) throws BillingActivityException {
    try {
      /// query aws usage records from the beginning of month, to until this moment
      final Calendar calFrom = Calendar.getInstance();
      calFrom.set(Integer.parseInt(year), Integer.parseInt(month)-1, 1, 0, 0);
      final Date queryBegin = calFrom.getTime();
      return Lists.newArrayList(AwsUsageRecords.getInstance().queryMonthly(
              accountId, service, null, null, queryBegin, queryEnd));
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to query the latest aws usage reports", ex);
    }
  }

  @Override
  public List<MonthlyUsageRecord> transform(List<AwsUsageRecord> awsUsage) throws BillingActivityException {
    try {
      return awsUsage.stream()
              .map(MonthlyReports.transform)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to transform aws usage records to monthly records", ex);
    }
  }

  @Override
  public void persist(String accountId, String year, String month, List<MonthlyUsageRecord> monthly) throws BillingActivityException {
    try {
      final AccountFullName owner = AccountFullName.getInstance(accountId);
      final List<MonthlyReportEntry> entries =
              monthly.stream()
                      .map(MonthlyReports.instantiate)
                      .collect(Collectors.toList());
      MonthlyReports.getInstance().createOrUpdate(owner, year, month, entries);
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to persist monthly report entries", ex);
    }
  }

  @Override
  public void uploadToS3Bucket(String accountId, String year, String month) throws BillingActivityException {
    final StringBuilder sb = new StringBuilder();
    sb.append("\"InvoiceID\",\"PayerAccountId\",\"LinkedAccountId\",\"RecordType\",\"RecordID\",\"BillingPeriodStartDate\"," +
            "\"BillingPeriodEndDate\",\"InvoiceDate\",\"PayerAccountName\",\"LinkedAccountName\",\"TaxationAddress\"," +
            "\"PayerPONumber\",\"ProductCode\",\"ProductName\",\"SellerOfRecord\",\"UsageType\",\"Operation\",\"RateId\"," +
            "\"ItemDescription\",\"UsageStartDate\",\"UsageEndDate\",\"UsageQuantity\",\"BlendedRate\",\"CurrencyCode\"," +
            "\"CostBeforeTax\",\"Credits\",\"TaxAmount\",\"TaxType\",\"TotalCost\"");
    try {
      MonthlyReports.getInstance()
              .lookupReport(AccountFullName.getInstance(accountId), year, month).stream()
              .forEach( r -> sb.append(String.format("\n%s", r.toString())) );
    } catch (final NoSuchElementException ex) {
      LOG.warn("No monthly record is found in persistence storage");
      return;
    }

    try {
      if (month!=null && month.length()<=1) {
        month = "0"+month;
      }
      final String keyName = String.format("%s-aws-billing-csv-%s-%s.csv", accountId, year, month);
      if (this.upload(accountId, keyName, new ByteArrayInputStream( sb.toString().getBytes("UTF-8")))) {
        LOG.debug(String.format("Monthly report for account " + accountId + " has been uploaded"));
      } else {
        LOG.warn("Failed to upload monthly report to s3 bucket for account " + accountId);
      }
    } catch (final UnsupportedEncodingException ex) {
      LOG.warn("Failed to create input stream out of report");
    } catch (final S3UploadException ex) {
      LOG.warn("Failed to upload monthly report to s3 bucket", ex);
    }
  }
}
