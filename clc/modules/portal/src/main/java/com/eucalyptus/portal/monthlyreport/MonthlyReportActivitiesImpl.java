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

import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.awsusage.AwsUsageRecords;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.workflow.BillingActivityException;
import com.eucalyptus.portal.workflow.MonthlyReportActivities;
import com.eucalyptus.portal.workflow.MonthlyUsageRecord;
import com.google.common.collect.Lists;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ComponentPart(Portal.class)
public class MonthlyReportActivitiesImpl implements MonthlyReportActivities {
  @Override
  public List<String> listCandidateAccounts() throws BillingActivityException {
    try {
      return Accounts.listAllAccounts().stream() // should we filter out system accounts?
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
              .filter(opt -> opt.isPresent())
              .map(opt -> opt.get())
              .collect(Collectors.toList());
    } catch (final Exception ex) {
      throw new BillingActivityException("Failed to transform aws usage records to monthly records", ex);
    }
  }

  @Override
  public void persist(String accountId, String year, String month, List<MonthlyUsageRecord> monthly) throws BillingActivityException {
//  public void createOrUpdate(final OwnerFullName owner, final String year, final String month, List<MonthlyReportEntry> entries) {
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
}
