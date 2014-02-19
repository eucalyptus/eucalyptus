/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudwatch.service.ws

import com.eucalyptus.binding.BindingException
import com.eucalyptus.cloudwatch.common.msgs.AlarmNames
import com.eucalyptus.cloudwatch.common.msgs.DeleteAlarmsType
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmHistoryType
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsForMetricType
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsType
import com.eucalyptus.cloudwatch.common.msgs.Dimension
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilter
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilters
import com.eucalyptus.cloudwatch.common.msgs.Dimensions
import com.eucalyptus.cloudwatch.common.msgs.DisableAlarmActionsType
import com.eucalyptus.cloudwatch.common.msgs.EnableAlarmActionsType
import com.eucalyptus.cloudwatch.common.msgs.GetMetricStatisticsType
import com.eucalyptus.cloudwatch.common.msgs.ListMetricsType
import com.eucalyptus.cloudwatch.common.msgs.MetricData
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum
import com.eucalyptus.cloudwatch.common.msgs.PutMetricAlarmType
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType
import com.eucalyptus.cloudwatch.common.msgs.ResourceList
import com.eucalyptus.cloudwatch.common.msgs.SetAlarmStateType
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet
import com.eucalyptus.cloudwatch.common.msgs.Statistics
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 *
 */
class CloudWatchQueryBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = CloudWatchQueryBindingTest.class.getResource( '/cloudwatch-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = CloudWatchQueryBindingTest.class.getResource( '/cloudwatch-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = CloudWatchQueryBindingTest.class.getResource( '/cloudwatch-binding.xml' )
    CloudWatchQueryBinding mb = new CloudWatchQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass(String operationName)
          throws BindingException {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      protected void validateBinding(com.eucalyptus.binding.Binding currentBinding,
                                     String operationName, Map<String, String> params, BaseMessage eucaMsg)
          throws BindingException {
        // Validation requires compiled bindings
      }
    }

    // PutMetricData
    bindAndAssertObject( mb, PutMetricDataType.class, "PutMetricData", new PutMetricDataType(
        namespace:  'Test',
        metricData: new MetricData(
            member: [
                new MetricDatum(
                    metricName: 'metric-name',
                    dimensions: new Dimensions(
                        member: [
                            new Dimension(
                                name:  'a',
                                value:  'b',
                            )
                        ]
                    ),
                    timestamp: new Date(),
                    value: 1.5d,
                    statisticValues: new StatisticSet(
                        sampleCount: 1.1,
                        sum: 1.2,
                        minimum: 1.3,
                        maximum: 1.4,
                    ),
                    unit: 'unit'
                )
            ]
        )
    ), 11 )

    // ListMetrics
    bindAndAssertObject( mb, ListMetricsType.class, "ListMetrics", new ListMetricsType(
        namespace:  'Test',
        metricName: 'metric-name',
        nextToken: 'token',
        dimensions: new DimensionFilters (
            member: [
                new DimensionFilter(
                    name:  'a',
                    value:  'b',
                ),
                new DimensionFilter(
                    name:  'c',
                    value:  'd',
                )
            ]
        )
    ), 7 )

    // GetMetricStatistics
    bindAndAssertObject( mb, GetMetricStatisticsType.class, "GetMetricStatistics", new GetMetricStatisticsType(
        namespace:  'Test',
        metricName: 'metric-name',
        dimensions: new Dimensions (
            member: [
                new Dimension(
                    name:  'a',
                    value:  'b',
                ),
                new Dimension(
                    name:  'c',
                    value:  'd',
                )
            ]
        ),
        startTime: new Date(),
        endTime: new Date(),
        period: 5,
        statistics: new Statistics (
            member: [ 'a', 'b' ]
        ),
        unit: 'None'
    ), 12 )

    // DescribeAlarms
    bindAndAssertObject( mb, DescribeAlarmsType.class, "DescribeAlarms", new DescribeAlarmsType(
        alarmNames: new AlarmNames (
            member: [ 'a', 'b' ]
        ),
        alarmNamePrefix:  'alarm-name-prefix',
        stateValue: 'state-value',
        actionPrefix: 'action-prefix',
        maxRecords: 5,
        nextToken: 'token'
    ), 7 )

    // PutMetricAlarm
    bindAndAssertObject( mb, PutMetricAlarmType.class, "PutMetricAlarm", new PutMetricAlarmType(
        alarmName:  'alarm-name',
        alarmDescription:  'alarm-description',
        actionsEnabled: true,
        okActions: new ResourceList (
            member: [ 'a', 'b' ]
        ),
        alarmActions: new ResourceList (
            member: [ 'a', 'b' ]
        ),
        insufficientDataActions: new ResourceList (
            member: [ 'a', 'b' ]
        ),
        metricName: 'metric-name',
        namespace: 'test',
        statistic: 'statistic',
        dimensions: new Dimensions (
            member: [
                new Dimension(
                    name:  'a',
                    value:  'b',
                ),
                new Dimension(
                    name:  'c',
                    value:  'd',
                )
            ]
        ),
        period: 5,
        unit: 'None',
        evaluationPeriods: 5,
        threshold: 4.0,
        comparisonOperator: 'LessThanThreshold'
    ), 21 )

    // EnableAlarmActions
    bindAndAssertObject( mb, EnableAlarmActionsType.class, "EnableAlarmActions", new EnableAlarmActionsType(
        alarmNames: new AlarmNames (
            member: [ 'a', 'b' ]
        ),
    ), 2 )

    // DescribeAlarmsForMetric
    bindAndAssertObject( mb, DescribeAlarmsForMetricType.class, "DescribeAlarmsForMetric", new DescribeAlarmsForMetricType(
        metricName: 'metric-name',
        namespace: 'test',
        statistic: 'statistic',
        dimensions: new Dimensions (
            member: [
                new Dimension(
                    name:  'a',
                    value:  'b',
                ),
                new Dimension(
                    name:  'c',
                    value:  'd',
                )
            ]
        ),
        period: 5,
        unit: 'None'
    ), 9 )

    // DeleteAlarms
    bindAndAssertObject( mb, DeleteAlarmsType.class, "DeleteAlarms", new DeleteAlarmsType(
        alarmNames: new AlarmNames (
            member: [ 'a', 'b' ]
        ),
    ), 2 )

    // DisableAlarmActions
    bindAndAssertObject( mb, DisableAlarmActionsType.class, "DisableAlarmActions", new DisableAlarmActionsType(
        alarmNames: new AlarmNames (
            member: [ 'a', 'b' ]
        ),
    ), 2 )

    // DescribeAlarmHistory
    bindAndAssertObject( mb, DescribeAlarmHistoryType.class, "DescribeAlarmHistory", new DescribeAlarmHistoryType(
        alarmName: 'alarm-name',
        historyItemType: 'history-item-type',
        startDate: new Date(),
        endDate: new Date(),
        maxRecords: 5,
        nextToken: 'token'
    ), 6 )

    // SetAlarmState
    bindAndAssertObject( mb, SetAlarmStateType.class, "SetAlarmState", new SetAlarmStateType(
        alarmName: 'alarm-name',
        stateValue: 'state-value',
        stateReason: 'state-reason',
        stateReasonData: 'state-reason-data'
    ), 4 )
  }
}
