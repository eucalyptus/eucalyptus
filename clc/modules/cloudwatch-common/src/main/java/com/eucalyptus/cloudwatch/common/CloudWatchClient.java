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
