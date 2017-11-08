/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.cloudwatch.common.backend.msgs

import com.eucalyptus.cloudwatch.common.CloudWatchBackend
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

import java.lang.reflect.Field;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.annotation.ComponentMessage;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import com.google.common.collect.Lists;

public class GetMetricStatisticsType extends CloudWatchBackendMessage {
  String namespace;
  String metricName;
  Dimensions dimensions;
  Date startTime;
  Date endTime;
  Integer period;
  Statistics statistics;
  String unit;
  public GetMetricStatisticsType() {  }
}
public class DescribeAlarmsType extends CloudWatchBackendMessage {
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
public class PutMetricDataType extends CloudWatchBackendMessage {
  String namespace;
  @HttpEmbedded(multiple=true)
  MetricData metricData;
  public PutMetricDataType() {  }
}
public class EnableAlarmActionsResponseType extends CloudWatchBackendMessage {
  public EnableAlarmActionsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class DescribeAlarmsForMetricResponseType extends CloudWatchBackendMessage {
  public DescribeAlarmsForMetricResponseType() {  }
  DescribeAlarmsForMetricResult describeAlarmsForMetricResult = new DescribeAlarmsForMetricResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PutMetricAlarmResponseType extends CloudWatchBackendMessage {
  public PutMetricAlarmResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DimensionFilters extends EucalyptusData {
  public DimensionFilters() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<DimensionFilter> member = new ArrayList<DimensionFilter>();
}
public class PutMetricAlarmType extends CloudWatchBackendMessage {
  String alarmName;
  String alarmDescription;
  Boolean actionsEnabled;
  @HttpParameterMapping(parameter="OKActions")
  ResourceList okActions;
  ResourceList alarmActions;
  ResourceList insufficientDataActions;
  String metricName;
  String namespace;
  String statistic;
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
public class SetAlarmStateResponseType extends CloudWatchBackendMessage {
  public SetAlarmStateResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class DescribeAlarmsResponseType extends CloudWatchBackendMessage {
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
public class EnableAlarmActionsType extends CloudWatchBackendMessage {
  AlarmNames alarmNames;
  public EnableAlarmActionsType() {  }
}
@ComponentMessage(CloudWatchBackend.class)
public class CloudWatchBackendMessage extends BaseMessage {
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
public class DescribeAlarmsForMetricType extends CloudWatchBackendMessage {
  String metricName;
  String namespace;
  String statistic;
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
  Dimensions dimensions;
  Date timestamp;
  Double value;
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
public class DeleteAlarmsType extends CloudWatchBackendMessage {
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
public class DescribeAlarmHistoryResponseType extends CloudWatchBackendMessage {
  public DescribeAlarmHistoryResponseType() {  }
  DescribeAlarmHistoryResult describeAlarmHistoryResult = new DescribeAlarmHistoryResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteAlarmsResponseType extends CloudWatchBackendMessage {
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
public class ListMetricsType extends CloudWatchBackendMessage {
  String namespace;
  String metricName;
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
public class GetMetricStatisticsResponseType extends CloudWatchBackendMessage {
  public GetMetricStatisticsResponseType() {  }
  GetMetricStatisticsResult getMetricStatisticsResult = new GetMetricStatisticsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DisableAlarmActionsResponseType extends CloudWatchBackendMessage {
  public DisableAlarmActionsResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AlarmHistoryItems extends EucalyptusData {
  public AlarmHistoryItems() {  }
  ArrayList<AlarmHistoryItem> member = new ArrayList<AlarmHistoryItem>();
}
public class DisableAlarmActionsType extends CloudWatchBackendMessage {
  AlarmNames alarmNames;
  public DisableAlarmActionsType() {  }
}
public class Metrics extends EucalyptusData {
  public Metrics() {  }
  ArrayList<Metric> member = new ArrayList<Metric>();
}
public class ListMetricsResponseType extends CloudWatchBackendMessage {
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
public class PutMetricDataResponseType extends CloudWatchBackendMessage {
  public PutMetricDataResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeAlarmHistoryType extends CloudWatchBackendMessage {
  String alarmName;
  String historyItemType;
  Date startDate;
  Date endDate;
  Integer maxRecords;
  String nextToken;
  public DescribeAlarmHistoryType() {  }
}
public class SetAlarmStateType extends CloudWatchBackendMessage {
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
