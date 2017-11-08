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
 ************************************************************************/package com.eucalyptus.cloudwatch.common;

import com.eucalyptus.cloudwatch.common.msgs.DeleteAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DeleteAlarmsType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmHistoryResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmHistoryType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsForMetricResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsForMetricType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsType;
import com.eucalyptus.cloudwatch.common.msgs.DisableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.DisableAlarmActionsType;
import com.eucalyptus.cloudwatch.common.msgs.EnableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.EnableAlarmActionsType;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsResponseType;
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricAlarmResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricAlarmType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.msgs.SetAlarmStateResponseType;
import com.eucalyptus.cloudwatch.common.msgs.SetAlarmStateType;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.util.async.CheckedListenableFuture;

/**
 *
 */
@ComponentPart( CloudWatch.class )
public interface CloudWatchClient {

  // sync
  DeleteAlarmsResponseType deleteAlarms( DeleteAlarmsType request );

  DescribeAlarmHistoryResponseType describeAlarmHistory( DescribeAlarmHistoryType request );

  DescribeAlarmsResponseType describeAlarms( DescribeAlarmsType request );

  DescribeAlarmsForMetricResponseType describeAlarmsForMetric( DescribeAlarmsForMetricType request );

  DisableAlarmActionsResponseType disableAlarmActions( DisableAlarmActionsType request );

  EnableAlarmActionsResponseType enableAlarmActions( EnableAlarmActionsType request );

  GetMetricStatisticsResponseType getMetricStatistics( GetMetricStatisticsType request );

  ListMetricsResponseType listMetrics( ListMetricsType request );

  PutMetricAlarmResponseType putMetricAlarm( PutMetricAlarmType request );

  PutMetricDataResponseType putMetricData( PutMetricDataType request );

  SetAlarmStateResponseType setAlarmState( SetAlarmStateType request );

  // select async
  CheckedListenableFuture<PutMetricDataResponseType> putMetricDataAsync( PutMetricDataType request );
}
