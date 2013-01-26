package com.eucalyptus.cloudwatch;

import com.eucalyptus.cloudwatch.DeleteAlarmsResponseType;
import com.eucalyptus.cloudwatch.DeleteAlarmsType;
import com.eucalyptus.cloudwatch.DescribeAlarmHistoryResponseType;
import com.eucalyptus.cloudwatch.DescribeAlarmHistoryType;
import com.eucalyptus.cloudwatch.DescribeAlarmsForMetricResponseType;
import com.eucalyptus.cloudwatch.DescribeAlarmsForMetricType;
import com.eucalyptus.cloudwatch.DescribeAlarmsResponseType;
import com.eucalyptus.cloudwatch.DescribeAlarmsType;
import com.eucalyptus.cloudwatch.DisableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.DisableAlarmActionsType;
import com.eucalyptus.cloudwatch.EnableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.EnableAlarmActionsType;
import com.eucalyptus.cloudwatch.GetMetricStatisticsResponseType;
import com.eucalyptus.cloudwatch.GetMetricStatisticsType;
import com.eucalyptus.cloudwatch.ListMetricsResponseType;
import com.eucalyptus.cloudwatch.ListMetricsType;
import com.eucalyptus.cloudwatch.PutMetricAlarmResponseType;
import com.eucalyptus.cloudwatch.PutMetricAlarmType;
import com.eucalyptus.cloudwatch.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.PutMetricDataType;
import com.eucalyptus.cloudwatch.SetAlarmStateResponseType;
import com.eucalyptus.cloudwatch.SetAlarmStateType;
import com.eucalyptus.util.EucalyptusCloudException;


public class CloudWatchService {
  public PutMetricAlarmResponseType putMetricAlarm(PutMetricAlarmType request) throws EucalyptusCloudException {
    PutMetricAlarmResponseType reply = request.getReply( );
    return reply;
  }

  public PutMetricDataResponseType putMetricData(PutMetricDataType request) throws EucalyptusCloudException {
    PutMetricDataResponseType reply = request.getReply( );
    return reply;
  }

  public ListMetricsResponseType listMetrics(ListMetricsType request) throws EucalyptusCloudException {
    ListMetricsResponseType reply = request.getReply( );
    return reply;
  }

  public GetMetricStatisticsResponseType getMetricStatistics(GetMetricStatisticsType request) throws EucalyptusCloudException {
    GetMetricStatisticsResponseType reply = request.getReply( );
    return reply;
  }

  public DisableAlarmActionsResponseType disableAlarmActions(DisableAlarmActionsType request) throws EucalyptusCloudException {
    DisableAlarmActionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAlarmsResponseType describeAlarms(DescribeAlarmsType request) throws EucalyptusCloudException {
    DescribeAlarmsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAlarmsForMetricResponseType describeAlarmsForMetric(DescribeAlarmsForMetricType request) throws EucalyptusCloudException {
    DescribeAlarmsForMetricResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAlarmHistoryResponseType describeAlarmHistory(DescribeAlarmHistoryType request) throws EucalyptusCloudException {
    DescribeAlarmHistoryResponseType reply = request.getReply( );
    return reply;
  }

  public EnableAlarmActionsResponseType enableAlarmActions(EnableAlarmActionsType request) throws EucalyptusCloudException {
    EnableAlarmActionsResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteAlarmsResponseType deleteAlarms(DeleteAlarmsType request) throws EucalyptusCloudException {
    DeleteAlarmsResponseType reply = request.getReply( );
    return reply;
  }

  public SetAlarmStateResponseType setAlarmState(SetAlarmStateType request) throws EucalyptusCloudException {
    SetAlarmStateResponseType reply = request.getReply( );
    return reply;
  }

}
