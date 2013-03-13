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

package com.eucalyptus.cloudwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmStateEvaluationDispatcher;
import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricUtils;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CloudWatchService {
  
  static {
    // TODO: put this somewhere else...
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5); // TODO: make this configurable
    ScheduledExecutorService alarmWorkerService = Executors.newSingleThreadScheduledExecutor();
    alarmWorkerService.scheduleAtFixedRate(new AlarmStateEvaluationDispatcher(fixedThreadPool), 0, 1, TimeUnit.MINUTES);
  }
  
  private static final Logger LOG = Logger.getLogger(CloudWatchService.class);
  
  private static final String SystemMetricPrefix = "AWS/";
  
  public PutMetricAlarmResponseType putMetricAlarm(PutMetricAlarmType request) throws EucalyptusCloudException {
    PutMetricAlarmResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_PUTMETRICALARM, ctx)) {
   	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final Boolean actionsEnabled = (request.getActionsEnabled() == null) ? 
        Boolean.TRUE : request.getActionsEnabled();
    final Collection<String> alarmActions = (request.getAlarmActions() != null) ? 
        request.getAlarmActions().getMember() : null;
    final String alarmDescription = request.getAlarmDescription();
    final String alarmName = request.getAlarmName();
    final ComparisonOperator comparisonOperator = ComparisonOperator.valueOf(request.getComparisonOperator());
    final Map<String, String> dimensionMap = transform(request.getDimensions());
    //final Collection<> request.getDimensions() != null ;
    Integer evaluationPeriods = request.getEvaluationPeriods();
    final Collection<String> insufficientDataActions = (request.getInsufficientDataActions() != null) ? 
        request.getInsufficientDataActions().getMember() : null;
    final String metricName = request.getMetricName();
    final String namespace = request.getNamespace();
    final Collection<String> okActions = (request.getOkActions() != null) ? 
        request.getOkActions().getMember() : null;
    final Integer period = request.getPeriod();
    final Statistic statistic = Statistic.valueOf(request.getStatistic());
    final Double threshold = request.getThreshold();
    final Units unit = Units.fromValue(request.getUnit());
    AlarmManager.putMetricAlarm(accountId, actionsEnabled, alarmActions, alarmDescription,
        alarmName, comparisonOperator, dimensionMap, evaluationPeriods, 
        insufficientDataActions, metricName, MetricType.Custom, namespace, okActions, period,
        statistic, threshold, unit);
    return reply;
  }

  public PutMetricDataResponseType putMetricData(PutMetricDataType request) throws EucalyptusCloudException {
    PutMetricDataResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_PUTMETRICDATA, ctx)) {
   	throw new EucalyptusCloudException();
    }
    
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String nameSpace = request.getNamespace();
    final List<MetricDatum> metricDatum = Lists.newArrayList(request.getMetricData().getMember());
    final Boolean isUserAccountAdmin = ctx.getUser().isAccountAdmin();
    //TODO: validate mon-put-data:  Malformed input-The parameter MetricData.member.1.StatisticValues.Maximum must
    //be greater than MetricData.member.1.StatisticValues.Minimum.
    //TODO: on-put-data:  Malformed input-The parameters MetricData.member.1.Value and
    // MetricData.member.1.StatisticValues are mutually exclusive and you have
    // specified both.
    
    if (nameSpace.startsWith(SystemMetricPrefix) && isUserAccountAdmin ) {
	MetricDataQueue.getInstance().insertMetricData(ownerFullName.getUserId(), ownerFullName.getAccountNumber(), nameSpace, metricDatum, MetricType.System);
    } else if (!nameSpace.startsWith(SystemMetricPrefix) ) {
	MetricDataQueue.getInstance().insertMetricData(ownerFullName.getUserId(), ownerFullName.getAccountNumber(), nameSpace, metricDatum, MetricType.Custom);
    } else {
	LOG.debug("Unknown metric data type");
    }

    return reply;
  }

  public ListMetricsResponseType listMetrics(ListMetricsType request) throws EucalyptusCloudException {
    ListMetricsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_LISTMETRICS, ctx)) {
   	throw new EucalyptusCloudException();
    }
    
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String namespace = request.getNamespace();
    final String metricName = request.getMetricName();
    final Map<String, String> dimensionMap = transform(request.getDimensions());
    
    // take all stats updated after two weeks ago
    final Date after = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60 * 60 * 1000L);
    final Date before = null; // no bound on time before stats are updated  (though maybe 'now') 
    
    final Collection<ListMetric> results = ListMetricManager.listMetrics(ownerFullName.getAccountNumber(), 
        metricName, 
        namespace, 
        dimensionMap, 
        after, 
        before);

    final Metrics metrics = new Metrics();
    metrics.setMember(Lists.newArrayList(Collections2.<ListMetric, Metric>transform(results, ListMetricFunction.INSTANCE)));
    final ListMetricsResult listMetricsResult = new ListMetricsResult();
    listMetricsResult.setMetrics(metrics);
    reply.setListMetricsResult(listMetricsResult);
    return reply;
  }

  private enum ListMetricFunction implements Function<ListMetric, Metric> {
    INSTANCE {
      @Override
      public Metric apply(@Nullable ListMetric listMetric) {
        Metric metric = new Metric();
        metric.setMetricName(listMetric.getMetricName());
        metric.setNamespace(listMetric.getNamespace());
        Dimensions dimensions = new Dimensions();
        dimensions.setMember(Lists.newArrayList(Collections2.<DimensionEntity, Dimension>transform(listMetric.getDimensions(), ListMetricDimensionFunction.INSTANCE)));
        metric.setDimensions(dimensions);
        return metric;
      }
    }
  }
  private enum ListMetricDimensionFunction implements Function<DimensionEntity, Dimension> {
    INSTANCE {
      @Override
      public Dimension apply(@Nullable DimensionEntity listMetricDimension) {
        Dimension dimension = new Dimension();
        dimension.setName(listMetricDimension.getName());
        dimension.setValue(listMetricDimension.getValue());
        return dimension;
      }
    }
  }

  private enum AlarmEntityFunction implements Function<AlarmEntity, MetricAlarm> {
    INSTANCE {
      @Override
      public MetricAlarm apply(@Nullable AlarmEntity alarmEntity) {
        LOG.debug("OK_ACTIONS="+alarmEntity.getOkActions());
        LOG.debug("ALARM_ACTIONS="+alarmEntity.getAlarmActions());
        LOG.debug("INSUFFICIENT_DATA_ACTIONS="+alarmEntity.getInsufficientDataActions());
        MetricAlarm metricAlarm = new MetricAlarm();
        metricAlarm.setActionsEnabled(alarmEntity.getActionsEnabled());

        ResourceList alarmActions = new ResourceList();
        ArrayList<String> alarmActionsMember = new ArrayList<String>();
        if (alarmEntity.getAlarmActions() != null) {
          alarmActionsMember.addAll(alarmEntity.getAlarmActions());
        }
        alarmActions.setMember(alarmActionsMember);
        metricAlarm.setAlarmActions(alarmActions);
        
        metricAlarm.setAlarmArn(alarmEntity.getResourceName());
        metricAlarm.setAlarmConfigurationUpdatedTimestamp(alarmEntity.getAlarmConfigurationUpdatedTimestamp());
        metricAlarm.setAlarmDescription(alarmEntity.getAlarmDescription());
        metricAlarm.setAlarmName(alarmEntity.getAlarmName());
        metricAlarm.setComparisonOperator(alarmEntity.getComparisonOperator() == null ? null : alarmEntity.getComparisonOperator().toString());
        Dimensions dimensions = new Dimensions();
        dimensions.setMember(Lists.newArrayList(Collections2.<DimensionEntity, Dimension>transform(alarmEntity.getDimensions(), ListMetricDimensionFunction.INSTANCE)));
        metricAlarm.setDimensions(dimensions);
        metricAlarm.setEvaluationPeriods(alarmEntity.getEvaluationPeriods());
        metricAlarm.setMetricName(alarmEntity.getMetricName());
        metricAlarm.setNamespace(alarmEntity.getNamespace());

        ResourceList okActions = new ResourceList();
        ArrayList<String> okActionsMember = new ArrayList<String>();
        if (alarmEntity.getOkActions() != null) {
          okActionsMember.addAll(alarmEntity.getOkActions());
        }
        okActions.setMember(okActionsMember);
        metricAlarm.setOkActions(okActions);

        metricAlarm.setPeriod(alarmEntity.getPeriod());
        metricAlarm.setStateReason(alarmEntity.getStateReason());
        metricAlarm.setStateReasonData(alarmEntity.getStateReasonData());
        metricAlarm.setStateUpdatedTimestamp(alarmEntity.getStateUpdatedTimestamp());
        metricAlarm.setStateValue(alarmEntity.getStateValue() == null ? null : alarmEntity.getStateValue().toString());
        metricAlarm.setStatistic(alarmEntity.getStatistic() == null ? null : alarmEntity.getStatistic().toString());
        metricAlarm.setThreshold(alarmEntity.getThreshold());
        metricAlarm.setUnit(alarmEntity.getUnit() == null ? null : alarmEntity.getUnit().toString());

        ResourceList insufficientDataActions = new ResourceList();
        ArrayList<String> insufficientDataActionsMember = new ArrayList<String>();
        if (alarmEntity.getInsufficientDataActions() != null) {
          insufficientDataActionsMember.addAll(alarmEntity.getInsufficientDataActions());
        }
        insufficientDataActions.setMember(insufficientDataActionsMember);
        metricAlarm.setInsufficientDataActions(insufficientDataActions);
        return metricAlarm;
      }
    }
  }
  
  private enum AlarmHistoryFunction implements Function<AlarmHistory, AlarmHistoryItem> {
    INSTANCE {
      @Override
      public AlarmHistoryItem apply(@Nullable AlarmHistory alarmHistory) {
        AlarmHistoryItem alarmHistoryItem = new AlarmHistoryItem();
        alarmHistoryItem.setAlarmName(alarmHistory.getAlarmName());
        alarmHistoryItem.setHistoryData(alarmHistory.getHistoryData());
        alarmHistoryItem.setHistoryItemType(alarmHistory.getHistoryItemType() == null ? null : alarmHistory.getHistoryItemType().toString());
        alarmHistoryItem.setHistorySummary(alarmHistory.getHistorySummary());
        alarmHistoryItem.setTimestamp(alarmHistory.getTimestamp());
        return alarmHistoryItem;
      }
    }
  }

  private Map<String, String> transform(DimensionFilters dimensionFilters) {
    final Map<String, String> result = Maps.newHashMap();
    if (dimensionFilters != null && dimensionFilters.getMember() != null) {
      for (DimensionFilter dimensionFilter: dimensionFilters.getMember()) {
        result.put(dimensionFilter.getName(), dimensionFilter.getValue());
      }
    }
    return result;
  }

  private Map<String, String> transform(Dimensions dimensions) {
    final Map<String, String> result = Maps.newHashMap();
    if (dimensions != null && dimensions.getMember() != null) {
      for (Dimension dimension: dimensions.getMember()) {
        result.put(dimension.getName(), dimension.getValue());
      }
    }
    return result;
  }

  public GetMetricStatisticsResponseType getMetricStatistics(GetMetricStatisticsType request) throws EucalyptusCloudException {
    GetMetricStatisticsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_GETMETRICSTATISTICS, ctx)) {
   	throw new EucalyptusCloudException();
    }
    // TODO: parse statistics separately()?
    Statistics statistics = request.getStatistics();
    if (statistics == null || statistics.getMember() == null) {
      throw new EucalyptusCloudException("Statistics is a required field");
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String namespace = request.getNamespace();
    final String metricName = request.getMetricName();
    final Date startTime = request.getStartTime();
    final Date endTime = request.getEndTime();
    final Integer period = request.getPeriod();
    LOG.debug("namespace="+namespace);
    LOG.debug("metricName="+metricName);
    LOG.debug("startTime="+startTime);
    LOG.debug("endTime="+endTime);
    LOG.debug("period="+period);
    // TODO: null units here does not mean Units.NONE but basically a wildcard.  Consider this case.
    final Units units = (request.getUnit() != null) ? Units.fromValue(request.getUnit()) : null;
    final Map<String, String> dimensionMap = transform(request.getDimensions());
    boolean wantsAverage = statistics.getMember().contains("Average");
    boolean wantsSum = statistics.getMember().contains("Sum");
    boolean wantsSampleCount = statistics.getMember().contains("SampleCount");
    boolean wantsMaximum = statistics.getMember().contains("Maximum");
    boolean wantsMinimum = statistics.getMember().contains("Minimum");
    Collection<MetricStatistics> metrics;
    if (namespace.startsWith("AWS/")) {
	metrics = MetricManager.getMetricStatistics(ownerFullName.getAccountNumber(), metricName, namespace, dimensionMap, MetricType.System, units, startTime, endTime, period);
    } else {
	metrics = MetricManager.getMetricStatistics(ownerFullName.getAccountNumber(), metricName, namespace, dimensionMap, MetricType.Custom, units, startTime, endTime, period);
    }
    reply.getGetMetricStatisticsResult().setLabel(metricName);
    ArrayList<Datapoint> datapoints = Lists.newArrayList();
    for (MetricStatistics metricStatistics: metrics) {
      Datapoint datapoint = new Datapoint();
      datapoint.setTimestamp(metricStatistics.getTimestamp());
      datapoint.setUnit(metricStatistics.getUnits().toString());
      if (wantsSum) {
        datapoint.setSum(metricStatistics.getSampleSum());
      }
      if (wantsSampleCount) {
        datapoint.setSampleCount(metricStatistics.getSampleSize());
      }
      if (wantsMaximum) {
        datapoint.setMaximum(metricStatistics.getSampleMax());
      }
      if (wantsMinimum) {
        datapoint.setMinimum(metricStatistics.getSampleMin());
      }
      if (wantsAverage) {
        datapoint.setAverage(MetricUtils.average(metricStatistics.getSampleSum(), metricStatistics.getSampleSize()));
      }
      datapoints.add(datapoint);
    }
    if (datapoints.size() > 0) {
      Datapoints datapointsReply = new Datapoints();
      datapointsReply.setMember(datapoints);
      reply.getGetMetricStatisticsResult().setDatapoints(datapointsReply);
    }
    return reply;
  }


  public DisableAlarmActionsResponseType disableAlarmActions(DisableAlarmActionsType request) throws EucalyptusCloudException {
    DisableAlarmActionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DISABLEALARMACTIONS, ctx)) {
   	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final Collection<String> alarmNames = (request.getAlarmNames() != null) ? request.getAlarmNames().getMember() : null;
    AlarmManager.disableAlarmActions(accountId, alarmNames);
    
    return reply;
  }

  public DescribeAlarmsResponseType describeAlarms(DescribeAlarmsType request) throws EucalyptusCloudException {
    DescribeAlarmsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMS, ctx)) {
   	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final String actionPrefix = request.getActionPrefix();
    final String alarmNamePrefix = request.getAlarmNamePrefix();
    final Collection<String> alarmNames = (request.getAlarmNames() != null) ? request.getAlarmNames().getMember() : null;
    final Integer maxRecords = request.getMaxRecords();
    final StateValue stateValue = (request.getStateValue() != null) ? StateValue.valueOf(request.getStateValue()) : null;
    Collection<AlarmEntity> results = AlarmManager.describeAlarms(accountId, actionPrefix, alarmNamePrefix, alarmNames, maxRecords, stateValue);
    MetricAlarms metricAlarms = new MetricAlarms();
    metricAlarms.setMember(Lists.newArrayList(Collections2.<AlarmEntity, MetricAlarm>transform(results, AlarmEntityFunction.INSTANCE)));
    reply.getDescribeAlarmsResult().setMetricAlarms(metricAlarms);
    return reply;
  }

  public DescribeAlarmsForMetricResponseType describeAlarmsForMetric(DescribeAlarmsForMetricType request) throws EucalyptusCloudException {
    DescribeAlarmsForMetricResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMSFORMETRIC, ctx)) {
	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final Map<String, String> dimensionMap = transform(request.getDimensions());
    final String metricName = request.getMetricName();
    final String namespace = request.getNamespace();
    final Integer period = request.getPeriod();
    final Statistic statistic = request.getStatistic() == null ? null: Statistic.valueOf(request.getStatistic());
    final Units unit = Units.fromValue(request.getUnit());
    Collection<AlarmEntity> results = AlarmManager.describeAlarmsForMetric(accountId, dimensionMap, metricName, namespace, period, statistic, unit);
    MetricAlarms metricAlarms = new MetricAlarms();
    metricAlarms.setMember(Lists.newArrayList(Collections2.<AlarmEntity, MetricAlarm>transform(results, AlarmEntityFunction.INSTANCE)));
    reply.getDescribeAlarmsForMetricResult().setMetricAlarms(metricAlarms);
    return reply;
  }

  public DescribeAlarmHistoryResponseType describeAlarmHistory(DescribeAlarmHistoryType request) throws EucalyptusCloudException {
    DescribeAlarmHistoryResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMHISTORY, ctx)) {
	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final String alarmName = request.getAlarmName();
    final Date endDate = request.getEndDate();
    final HistoryItemType historyItemType = request.getHistoryItemType() == null ? null : HistoryItemType.valueOf(request.getHistoryItemType());
    final Integer maxRecords = request.getMaxRecords();
    final Date startDate = request.getStartDate();
    Collection<AlarmHistory> results = AlarmManager.describeAlarmHistory(accountId, alarmName, endDate, historyItemType, maxRecords, startDate);
    AlarmHistoryItems alarmHistoryItems = new AlarmHistoryItems();
    alarmHistoryItems.setMember(Lists.newArrayList(Collections2.<AlarmHistory, AlarmHistoryItem>transform(results, AlarmHistoryFunction.INSTANCE)));
    reply.getDescribeAlarmHistoryResult().setAlarmHistoryItems(alarmHistoryItems);
    return reply;
  }

  public EnableAlarmActionsResponseType enableAlarmActions(EnableAlarmActionsType request) throws EucalyptusCloudException {
    EnableAlarmActionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_ENABLEALARMACTIONS, ctx)) {
	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final Collection<String> alarmNames = (request.getAlarmNames() != null) ? request.getAlarmNames().getMember() : null;
    AlarmManager.enableAlarmActions(accountId, alarmNames);
    
    return reply;
  }

  public DeleteAlarmsResponseType deleteAlarms(DeleteAlarmsType request) throws EucalyptusCloudException {
    DeleteAlarmsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DELETEALARMS, ctx)) {
	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    Collection<String> alarmNames = (request.getAlarmNames() != null) ? 
        request.getAlarmNames().getMember() : null;
    AlarmManager.deleteAlarms(accountId, alarmNames);
    return reply;
  }

  public SetAlarmStateResponseType setAlarmState(SetAlarmStateType request) throws EucalyptusCloudException {
    SetAlarmStateResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_SETALARMSTATE, ctx)) {
	throw new EucalyptusCloudException();
    }
    final OwnerFullName ownerFullName = ctx.getUserFullName();
    final String accountId = ownerFullName.getAccountNumber(); 
    final String alarmName = request.getAlarmName();
    final String stateReason = request.getStateReason();
    final String stateReasonData = request.getStateReasonData(); 
    final StateValue stateValue = StateValue.valueOf(request.getStateValue());
    AlarmManager.setAlarmState(accountId, alarmName, stateReason, stateReasonData, stateValue);
    return reply;
  }
  
  private Boolean hasActionPermission(final String actionType, final Context ctx) {
	return Permissions.isAuthorized(PolicySpec.VENDOR_CLOUDWATCH,
		actionType, "", ctx.getAccount(), actionType, ctx.getUser());
  }
  
}
