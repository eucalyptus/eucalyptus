/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.junit.Test

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.cloudformation.ws.CloudFormationQueryBinding;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.ws.protocol.QueryBindingTestSupport

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/**
 * 
 */
class CloudFormationBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    CloudFormationQueryBinding asb = new CloudFormationQueryBinding() {
      @Override
      protected Binding getBindingWithElementClass(String operationName)
          throws BindingException {
            createTestBindingFromXml( resource, operationName )
     }

      @Override
      protected void validateBinding(Binding currentBinding,
          String operationName, Map<String, String> params, BaseMessage eucaMsg)
          throws BindingException {
          // Validation requires compiled bindings
      } 
    }

        // CancelUpdateStack
    bindAndAssertObject( asb, CancelUpdateStackType.class, "CancelUpdateStack", new CancelUpdateStackType(
      stackName: 'Stack-Name'
    ), 1)

    // CreateStack
    bindAndAssertObject( asb, CreateStackType.class, "CreateStack", new CreateStackType(
      capabilities: new ResourceList (
        member: [ 'capability1', 'capability2' ],
      ),
      disableRollback: true,
      notificationARNs: new ResourceList (
        member: [ 'notificationARN1', 'notificationARN2', 'notificationARN3' ],
      ),
      onFailure: 'fail',
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      stackName: 'Stack-Name',
      stackPolicyBody: 'Stack-Policy-Body',
      stackPolicyURL: 'Stack-Policy-URL',
      tags: new Tags (
        member: [
          new Tag(
            key: 'Key-1',
            value: 'Value-1'
          )
        ]
      ),
      templateBody: 'Template-Body',
      templateURL: 'Template-URL',
      timeoutInMinutes: 10
    ), 19)
    // DeleteStack
    bindAndAssertObject( asb, DeleteStackType.class, "DeleteStack", new DeleteStackType(
      stackName: 'Stack-Name'
    ), 1)
    // DeleteStack
    bindAndAssertObject( asb, DeleteStackType.class, "DeleteStack", new DeleteStackType(
      stackName: 'Stack-Name'
    ), 1)
    // DescribeStackEvents
    bindAndAssertObject( asb, DescribeStackEventsType.class, "DescribeStackEvents", new DescribeStackEventsType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // DescribeStackResource
    bindAndAssertObject( asb, DescribeStackResourceType.class, "DescribeStackResource", new DescribeStackResourceType(
      logicalResourceId: 'Logical-Resource-Id',
      stackName: 'Stack-Name'
    ), 2)
    // DescribeStackResources
    bindAndAssertObject( asb, DescribeStackResourcesType.class, "DescribeStackResources", new DescribeStackResourcesType(
      logicalResourceId: 'Logical-Resource-Id',
      physicalResourceId: 'Physical-Resource-Id',
      stackName: 'Stack-Name'
    ), 3)
    // DescribeStacks
    bindAndAssertObject( asb, DescribeStacksType.class, "DescribeStacks", new DescribeStacksType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // EstimateTemplateCost
    bindAndAssertObject( asb, EstimateTemplateCostType.class, "EstimateTemplateCost", new EstimateTemplateCostType(
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 6)
    // GetStackPolicy
    bindAndAssertObject( asb, GetStackPolicyType.class, "GetStackPolicy", new GetStackPolicyType(
      stackName: 'Stack-Name'
    ), 1)
    // GetTemplate
    bindAndAssertObject( asb, GetTemplateType.class, "GetTemplate", new GetTemplateType(
      stackName: 'Stack-Name'
    ), 1)
    // ListStackResources
    bindAndAssertObject( asb, ListStackResourcesType.class, "ListStackResources", new ListStackResourcesType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // ListStacks
    bindAndAssertObject( asb, ListStacksType.class, "ListStacks", new ListStacksType(
      nextToken: 'Next-Token',
      stackStatusFilter: new ResourceList(
        member: ['Stack-Status-Filter-1', 'Stack-Status-Filter-2']
      )
    ), 3)
    // SetStackPolicy
    bindAndAssertObject( asb, SetStackPolicyType.class, "SetStackPolicy", new SetStackPolicyType(
      stackName: 'Stack-Name',
      stackPolicyBody: 'Stack-Policy-Body',
      stackPolicyURL: 'Stack-Policy-URL'
    ), 3)
    // UpdateStack
    bindAndAssertObject( asb, UpdateStackType.class, "UpdateStack", new UpdateStackType(
      capabilities: new ResourceList (
        member: [ 'capability1', 'capability2' ],
      ),
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      stackName: 'Stack-Name',
      stackPolicyBody : 'Stack-Policy-Body',
      stackPolicyDuringUpdateBody: 'Stack-Policy-During-Update-Body',
      stackPolicyDuringUpdateURL: 'Stack-Policy-During-Update-URL',
      stackPolicyURL: 'Stack-Policy-URL',
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 13 ) 
    // ValidateTemplate
    bindAndAssertObject( asb, ValidateTemplateType.class, "ValidateTemplate", new ValidateTemplateType(
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 2 )
/*        
        // PutMetricData
    bindAndAssertObject( asb, PutMetricDataType.class, "PutMetricData", new PutMetricDataType(
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
    bindAndAssertObject( asb, ListMetricsType.class, "ListMetrics", new ListMetricsType(
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
    bindAndAssertObject( asb, GetMetricStatisticsType.class, "GetMetricStatistics", new GetMetricStatisticsType(
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
    bindAndAssertObject( asb, DescribeAlarmsType.class, "DescribeAlarms", new DescribeAlarmsType(
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
    bindAndAssertObject( asb, PutMetricAlarmType.class, "PutMetricAlarm", new PutMetricAlarmType(
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
    bindAndAssertObject( asb, EnableAlarmActionsType.class, "EnableAlarmActions", new EnableAlarmActionsType(
      alarmNames: new AlarmNames (
          member: [ 'a', 'b' ]
      ),
    ), 2 )

    // DescribeAlarmsForMetric
    bindAndAssertObject( asb, DescribeAlarmsForMetricType.class, "DescribeAlarmsForMetric", new DescribeAlarmsForMetricType(
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
    bindAndAssertObject( asb, DeleteAlarmsType.class, "DeleteAlarms", new DeleteAlarmsType(
      alarmNames: new AlarmNames (
          member: [ 'a', 'b' ]
      ),
    ), 2 )

    // DisableAlarmActions
    bindAndAssertObject( asb, DisableAlarmActionsType.class, "DisableAlarmActions", new DisableAlarmActionsType(
      alarmNames: new AlarmNames (
          member: [ 'a', 'b' ]
      ),
    ), 2 )

    // DescribeAlarmHistory
    bindAndAssertObject( asb, DescribeAlarmHistoryType.class, "DescribeAlarmHistory", new DescribeAlarmHistoryType(
      alarmName: 'alarm-name',
      historyItemType: 'history-item-type',
      startDate: new Date(),
      endDate: new Date(),
      maxRecords: 5,
      nextToken: 'token'
    ), 6 )

    // SetAlarmState
    bindAndAssertObject( asb, SetAlarmStateType.class, "SetAlarmState", new SetAlarmStateType(
      alarmName: 'alarm-name',
      stateValue: 'state-value',
      stateReason: 'state-reason',
      stateReasonData: 'state-reason-data'
    ), 4 )*/
  }
}

