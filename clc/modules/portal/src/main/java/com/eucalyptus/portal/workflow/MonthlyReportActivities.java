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
package com.eucalyptus.portal.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.common.FlowConstants;

import java.util.Date;
import java.util.List;

@Activities(version="1.0")
@ActivityRegistrationOptions(
        defaultTaskHeartbeatTimeoutSeconds = FlowConstants.NONE,
        defaultTaskScheduleToCloseTimeoutSeconds = 180,
        defaultTaskScheduleToStartTimeoutSeconds = 120,
        defaultTaskStartToCloseTimeoutSeconds = 60)
public interface MonthlyReportActivities {
  List<String> listCandidateAccounts() throws BillingActivityException;
  List<AwsUsageRecord> queryMonthlyAwsUsage(final String accountId, final String service, final String year, final String month, final Date end) throws BillingActivityException;
  List<MonthlyUsageRecord> transform(List<AwsUsageRecord> awsUsage) throws BillingActivityException;
  void persist(final String accountId, final String year, final String month, List<MonthlyUsageRecord> monthly) throws BillingActivityException;
  void uploadToS3Bucket(final String accountId, final String year, final String month) throws BillingActivityException;
}