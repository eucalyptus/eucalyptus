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
package com.eucalyptus.cloudwatch.common.msgs

import com.eucalyptus.cloudwatch.common.CloudWatch

import java.lang.reflect.Field;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.annotation.ComponentMessage;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import com.google.common.collect.Lists;

public class GetMetricStatisticsType extends CloudWatchMessage {
  String namespace;
  String metricName;
  @HttpEmbedded
  Dimensions dimensions;
  Date startTime;
  Date endTime;
  Integer period;
  @HttpEmbedded
  Statistics statistics;
  String unit;
  public GetMetricStatisticsType() {  }
}
public class DescribeAlarmsType extends CloudWatchMessage {
  @HttpEmbedded
  AlarmNames alarmNames;
  String alarmNamePrefix;
  String stateValue;
  String actionPrefix;
  Integer maxRecords;
  String nextToken;
  public DescribeAlarmsType() {  }
  List<String> getAlarms( ) {
    List<String> names = Lists.newArrayList()
    if ( alarmNames != null ) {
      names = alarmNames.getMember()
    }
    return names
  }
}
public class Dimensions extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Dimension> member = new ArrayList<Dimension>();
  public Dimensions() {  }
  public Dimensions( Dimension dimension ) {
    member.add( dimension  )
  }
  @Override
  public String toString() {
    return "Dimensions [member=" + member + "]";
  }
}
public class PutMetricDataType extends CloudWatchMessage {
  String namespace;
  @HttpEmbedded(multiple=true)
  MetricData metricData;
  public PutMetricDataType() {  }
}
public class EnableAlarmActionsResponseType extends CloudWatchMessage {
  public EnableAlarmActionsResponseType() {  }
  @HttpEmbedded
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class DescribeAlarmsForMetricResponseType extends CloudWatchMessage {
  public DescribeAlarmsForMetricResponseType() {  }
  DescribeAlarmsForMetricResult describeAlarmsForMetricResult = new DescribeAlarmsForMetricResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PutMetricAlarmResponseType extends CloudWatchMessage {
  public PutMetricAlarmResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DimensionFilters extends EucalyptusData {
  public DimensionFilters() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<DimensionFilter> member = new ArrayList<DimensionFilter>();
}
public class PutMetricAlarmType extends CloudWatchMessage {
  String alarmName;
  String alarmDescription;
  Boolean actionsEnabled;
  @HttpEmbedded
  @HttpParameterMapping(parameter="OKActions")
  ResourceList okActions;
  @HttpEmbedded
  ResourceList alarmActions;
  @HttpEmbedded
  ResourceList insufficientDataActions;
  String metricName;
  String namespace;
  String statistic;
  @HttpEmbedded
  Dimensions dimensions;
  Integer period;
  String unit;
  Integer evaluationPeriods;
  Double threshold;
  String comparisonOperator;
  public PutMetricAlarmType() {  }
}
public class DescribeAlarmHistoryResult extends EucalyptusData {
  AlarmHistoryItems alarmHistoryItems;
  String nextToken;
  public DescribeAlarmHistoryResult() {  }
}
public class DimensionFilter extends EucalyptusData {
  String name;
  String value;
  public DimensionFilter() {  }
}
public class Dimension extends EucalyptusData {
  String name;
  String value;
  public Dimension() {  }
  public Dimension( String name, String value ) {
    this.name = name
    this.value = value
  }
  @Override
  public String toString() {
    return "Dimension [name=" + name + ", value=" + value + "]";
  }
}
public class SetAlarmStateResponseType extends CloudWatchMessage {
  public SetAlarmStateResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class DescribeAlarmsResponseType extends CloudWatchMessage {
  public DescribeAlarmsResponseType() {  }
  DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class MetricData extends EucalyptusData {
  public MetricData() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<MetricDatum> member = new ArrayList<MetricDatum>();
}
public class EnableAlarmActionsType extends CloudWatchMessage {
  @HttpEmbedded
  AlarmNames alarmNames;
  public EnableAlarmActionsType() {  }
}
@ComponentMessage(CloudWatch.class)
public class CloudWatchMessage extends BaseMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    try {
      Field responseMetadataField = type.class.getDeclaredField("responseMetadata")
      responseMetadataField.setAccessible( true ) 
      ((ResponseMetadata) responseMetadataField.get( type )).requestId = getCorrelationId()
    } catch ( Exception e ) {       
    }
    return type
  }
}
public class DescribeAlarmsForMetricType extends CloudWatchMessage {
  String metricName;
  String namespace;
  String statistic;
  @HttpEmbedded
  Dimensions dimensions;
  Integer period;
  String unit;
  public DescribeAlarmsForMetricType() {  }
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  public Error() {  }
  ErrorDetail detail = new ErrorDetail();
}
public class ResourceList extends EucalyptusData {
  public ResourceList() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class MetricDatum extends EucalyptusData {
  String metricName;
  @HttpEmbedded
  Dimensions dimensions;
  Date timestamp;
  Double value;
  @HttpEmbedded
  StatisticSet statisticValues;
  String unit;
  public MetricDatum() {  }
  @Override
  public String toString() {
    return "MetricDatum [metricName=" + metricName + ", dimensions=" +
        dimensions + ", timestamp=" + timestamp + ", value=" + value +
        ", statisticValues=" + statisticValues + ", unit=" + unit + "]";
  }

}

public class StatisticSet extends EucalyptusData {
  Double sampleCount;
  Double sum;
  Double minimum;
  Double maximum;
  public StatisticSet() {  }
  @Override
  public String toString() {
    return "StatisticSet [sampleCount=" + sampleCount + ", sum=" + sum +
        ", minimum=" + minimum + ", maximum=" + maximum + "]";
  }
}
public class Datapoints extends EucalyptusData {
  public Datapoints() {  }
  ArrayList<Datapoint> member = new ArrayList<Datapoint>();
}
public class DeleteAlarmsType extends CloudWatchMessage {
  @HttpEmbedded
  AlarmNames alarmNames;
  public DeleteAlarmsType() {  }
}
public class DescribeAlarmsResult extends EucalyptusData {
  MetricAlarms metricAlarms;
  String nextToken;
  public DescribeAlarmsResult() {  }
}
public class Metric extends EucalyptusData {
  String namespace;
  String metricName;
  Dimensions dimensions;
  public Metric() {  }
}
public class DescribeAlarmHistoryResponseType extends CloudWatchMessage {
  public DescribeAlarmHistoryResponseType() {  }
  DescribeAlarmHistoryResult describeAlarmHistoryResult = new DescribeAlarmHistoryResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteAlarmsResponseType extends CloudWatchMessage {
  public DeleteAlarmsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AlarmNames extends EucalyptusData {
  public AlarmNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class Statistics extends EucalyptusData {
  public Statistics() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class ListMetricsType extends CloudWatchMessage {
  String namespace;
  String metricName;
  @HttpEmbedded
  DimensionFilters dimensions;
  String nextToken;
  public ListMetricsType() {  }
}
public class AlarmHistoryItem extends EucalyptusData {
  String alarmName;
  Date timestamp;
  String historyItemType;
  String historySummary;
  String historyData;
  public AlarmHistoryItem() {  }
}
public class GetMetricStatisticsResponseType extends CloudWatchMessage {
  public GetMetricStatisticsResponseType() {  }
  GetMetricStatisticsResult getMetricStatisticsResult = new GetMetricStatisticsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DisableAlarmActionsResponseType extends CloudWatchMessage {
  public DisableAlarmActionsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AlarmHistoryItems extends EucalyptusData {
  public AlarmHistoryItems() {  }
  ArrayList<AlarmHistoryItem> member = new ArrayList<AlarmHistoryItem>();
}
public class DisableAlarmActionsType extends CloudWatchMessage {
  @HttpEmbedded
  AlarmNames alarmNames;
  public DisableAlarmActionsType() {  }
}
public class CloudWatchErrorResponse extends CloudWatchMessage {
  String requestId;
  public CloudWatchErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>();
}
public class Metrics extends EucalyptusData {
  public Metrics() {  }
  ArrayList<Metric> member = new ArrayList<Metric>();
}
public class ListMetricsResponseType extends CloudWatchMessage {
  public ListMetricsResponseType() {  }
  ListMetricsResult listMetricsResult = new ListMetricsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetMetricStatisticsResult extends EucalyptusData {
  String label;
  Datapoints datapoints;
  public GetMetricStatisticsResult() {  }
}
public class Datapoint extends EucalyptusData {
  Date timestamp;
  Double sampleCount;
  Double average;
  Double sum;
  Double minimum;
  Double maximum;
  String unit;
  public Datapoint() {  }
}
public class MetricAlarms extends EucalyptusData {
  public MetricAlarms() {  }
  ArrayList<MetricAlarm> member = new ArrayList<MetricAlarm>();
}
public class PutMetricDataResponseType extends CloudWatchMessage {
  public PutMetricDataResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeAlarmHistoryType extends CloudWatchMessage {
  String alarmName;
  String historyItemType;
  Date startDate;
  Date endDate;
  Integer maxRecords;
  String nextToken;
  public DescribeAlarmHistoryType() {  }
}
public class SetAlarmStateType extends CloudWatchMessage {
  String alarmName;
  String stateValue;
  String stateReason;
  String stateReasonData;
  public SetAlarmStateType() {  }
}
public class DescribeAlarmsForMetricResult extends EucalyptusData {
  MetricAlarms metricAlarms;
  public DescribeAlarmsForMetricResult() {  }
}
public class MetricAlarm extends EucalyptusData {
  String alarmName;
  String alarmArn;
  String alarmDescription;
  Date alarmConfigurationUpdatedTimestamp;
  Boolean actionsEnabled;
  ResourceList okActions;
  ResourceList alarmActions;
  ResourceList insufficientDataActions;
  String stateValue;
  String stateReason;
  String stateReasonData;
  Date stateUpdatedTimestamp;
  String metricName;
  String namespace;
  String statistic;
  Dimensions dimensions;
  Integer period;
  String unit;
  Integer evaluationPeriods;
  Double threshold;
  String comparisonOperator;
  public MetricAlarm() {  }
}
public class ListMetricsResult extends EucalyptusData {
  Metrics metrics;
  String nextToken;
  public ListMetricsResult() {  }
}
