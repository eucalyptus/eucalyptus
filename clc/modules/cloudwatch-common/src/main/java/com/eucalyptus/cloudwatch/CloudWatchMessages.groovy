
package com.eucalyptus.cloudwatch;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;
import java.math.BigInteger;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.eucalyptus.component.ComponentId;


public class GetMetricStatisticsType extends CloudWatchMessage {
  String namespace;
  String metricName;
  Dimensions dimensions;
  Date startTime;
  Date endTime;
  BigInteger period;
  Statistics statistics;
  String unit;
  public GetMetricStatisticsType() {  }
}
public class DescribeAlarmsType extends CloudWatchMessage {
  AlarmNames alarmNames;
  String alarmNamePrefix;
  String stateValue;
  String actionPrefix;
  BigInteger maxRecords;
  String nextToken;
  public DescribeAlarmsType() {  }
}
public class Dimensions extends EucalyptusData {
  public Dimensions() {  }
  ArrayList<Dimension> member = new ArrayList<Dimension>();
}
public class PutMetricDataType extends CloudWatchMessage {
  String namespace;
  MetricData metricData;
  public PutMetricDataType() {  }
}
public class EnableAlarmActionsResponseType extends CloudWatchMessage {
  public EnableAlarmActionsResponseType() {  }
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
  ResourceList okActions;
  ResourceList alarmActions;
  ResourceList insufficientDataActions;
  String metricName;
  String namespace;
  String statistic;
  Dimensions dimensions;
  BigInteger period;
  String unit;
  BigInteger evaluationPeriods;
  Double threshold;
  String comparisonOperator;
  public PutMetricAlarmType() {  }
}
public class DescribeAlarmHistoryResult extends EucalyptusData {
  AlarmHistoryItems alarmHistoryItems;
  String nextToken;
  public DescribeAlarmHistoryResult() {  }
}
@HttpEmbedded
public class DimensionFilter extends EucalyptusData {
  String name;
  String value;
  public DimensionFilter() {  }
}
public class Dimension extends EucalyptusData {
  String name;
  String value;
  public Dimension() {  }
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
public class MetricData extends CloudWatchMessage {
  public MetricData() {  }
  ArrayList<MetricDatum> member = new ArrayList<MetricDatum>();
}
public class EnableAlarmActionsType extends CloudWatchMessage {
  AlarmNames alarmNames;
  public EnableAlarmActionsType() {  }
}
@ComponentId.ComponentMessage(CloudWatch.class)
public class CloudWatchMessage extends BaseMessage {
    @Override
    def <TYPE extends BaseMessage> TYPE getReply() {
	TYPE type = super.getReply()
	if (type.properties.containsKey("responseMetadata")) {
	  ((ResponseMetadata) type.properties.get("responseMetadata")).requestId = getCorrelationId();
	}
	return type
      }
}
public class DescribeAlarmsForMetricType extends CloudWatchMessage {
  String metricName;
  String namespace;
  String statistic;
  Dimensions dimensions;
  BigInteger period;
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
}
public class StatisticSet extends EucalyptusData {
  Double sampleCount;
  Double sum;
  Double minimum;
  Double maximum;
  public StatisticSet() {  }
}
public class Datapoints extends EucalyptusData {
  public Datapoints() {  }
  ArrayList<Datapoint> member = new ArrayList<Datapoint>();
}
public class DeleteAlarmsType extends CloudWatchMessage {
  AlarmNames alarmNames;
  public DeleteAlarmsType() {  }
}
public class DescribeAlarmsResult extends EucalyptusData {
  MetricAlarms metricAlarms;
  String nextToken;
  public DescribeAlarmsResult() {  }
}
public class Metric extends CloudWatchMessage {
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
  ArrayList<String> member = new ArrayList<String>();
}
public class Statistics extends CloudWatchMessage {
  public Statistics() {  }
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
  AlarmNames alarmNames;
  public DisableAlarmActionsType() {  }
}
public class CloudWatchErrorResponse extends CloudWatchMessage {
  String requestId;
  public CloudWatchErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>();
}
public class Metrics extends CloudWatchMessage {
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
  BigInteger maxRecords;
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
public class MetricAlarm extends CloudWatchMessage {
  String alarmName;
  String alarmArn;
  String alarmDescription;
  Date alarmConfigurationUpdatedTimestamp;
  Boolean actionsEnabled;
  ResourceList okActions;
  ResourceList alarmActions;
  ResourceList unknownActions;
  String stateValue;
  String stateReason;
  String stateReasonData;
  Date stateUpdatedTimestamp;
  String metricName;
  String namespace;
  String statistic;
  Dimensions dimensions;
  BigInteger period;
  String unit;
  BigInteger evaluationPeriods;
  Double threshold;
  String comparisonOperator;
  public MetricAlarm() {  }
}
public class ListMetricsResult extends EucalyptusData {
  Metrics metrics;
  String nextToken;
  public ListMetricsResult() {  }
}
