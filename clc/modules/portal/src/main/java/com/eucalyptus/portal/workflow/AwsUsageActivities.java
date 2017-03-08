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
package com.eucalyptus.portal.workflow;


import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.common.FlowConstants;

import java.util.List;
import java.util.Map;

@Activities(version="1.0")
@ActivityRegistrationOptions(
        defaultTaskHeartbeatTimeoutSeconds = FlowConstants.NONE,
        defaultTaskScheduleToCloseTimeoutSeconds = 180,
        defaultTaskScheduleToStartTimeoutSeconds = 120,
        defaultTaskStartToCloseTimeoutSeconds = 60)
public interface AwsUsageActivities {
  // key: accountId, value: queue name
  Map<String, String> createAccountQueues(final String globalQueue) throws BillingActivityException;
  List<AwsUsageRecord> getAwsReportHourlyUsageRecord(final String accountId, final String queueName ) throws BillingActivityException;
  List<AwsUsageRecord> getAwsReportDailyUsageRecord(final String accountId, final String queueName ) throws BillingActivityException;
  void writeAwsReportUsage(final String accountId, final List<AwsUsageRecord> records) throws BillingActivityException;
  void deleteAccountQueues(final List<String> queues) throws BillingActivityException;

  void fireVolumeUsage() throws BillingActivityException;
  void fireSnapshotUsage() throws BillingActivityException;
  void fireAddressUsage() throws BillingActivityException;
  void fireS3ObjectUsage() throws BillingActivityException;
  void fireLoadBalancerUsage() throws BillingActivityException;
}
