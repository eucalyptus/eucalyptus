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