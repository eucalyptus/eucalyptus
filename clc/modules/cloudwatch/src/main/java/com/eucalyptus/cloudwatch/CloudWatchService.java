package com.eucalyptus.cloudwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricStatistics;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CloudWatchService {
  private static final Logger LOG = Logger.getLogger(CloudWatchService.class);
  public PutMetricAlarmResponseType putMetricAlarm(PutMetricAlarmType request) throws EucalyptusCloudException {
    PutMetricAlarmResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_PUTMETRICALARM, ctx)) {
   	throw new EucalyptusCloudException();
    }
    
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
    //TODO: validate mon-put-data:  Malformed input-The parameter MetricData.member.1.StatisticValues.Maximum must
    //be greater than MetricData.member.1.StatisticValues.Minimum.
    //TODO: on-put-data:  Malformed input-The parameters MetricData.member.1.Value and
    // MetricData.member.1.StatisticValues are mutually exclusive and you have
    // specified both.
    MetricDataQueue.getInstance().insertMetricData(ownerFullName.getUserId(), ownerFullName.getAccountNumber(), nameSpace, metricDatum, MetricType.Custom);

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
    // TODO: null units here does not mean Units.NONE but basically a wildcard.  Consider this case.
    final Units units = (request.getUnit() != null) ? Units.fromValue(request.getUnit()) : null;
    final Map<String, String> dimensionMap = transform(request.getDimensions());
    boolean wantsAverage = statistics.getMember().contains("Average");
    boolean wantsSum = statistics.getMember().contains("Sum");
    boolean wantsSampleCount = statistics.getMember().contains("SampleCount");
    boolean wantsMaximum = statistics.getMember().contains("Maximum");
    boolean wantsMinimum = statistics.getMember().contains("Minimum");
    Collection<MetricStatistics> metrics = MetricManager.getMetricStatistics(ownerFullName.getAccountNumber(), ownerFullName.getUserId(), metricName, namespace, dimensionMap, MetricType.Custom, units, startTime, endTime, period);
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
        datapoint.setAverage(average(metricStatistics.getSampleSum(), metricStatistics.getSampleSize()));
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

  private Double average(Double sum, Double size) {
    if (Math.abs(size) < 0.9) return 0.0; // TODO: make sure size is really an int
    return sum/size;
  }

  public DisableAlarmActionsResponseType disableAlarmActions(DisableAlarmActionsType request) throws EucalyptusCloudException {
    DisableAlarmActionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DISABLEALARMACTIONS, ctx)) {
   	throw new EucalyptusCloudException();
    }
  
    return reply;
  }

  public DescribeAlarmsResponseType describeAlarms(DescribeAlarmsType request) throws EucalyptusCloudException {
    DescribeAlarmsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMS, ctx)) {
   	throw new EucalyptusCloudException();
    }
    
    return reply;
  }

  public DescribeAlarmsForMetricResponseType describeAlarmsForMetric(DescribeAlarmsForMetricType request) throws EucalyptusCloudException {
    DescribeAlarmsForMetricResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMSFORMETRIC, ctx)) {
	throw new EucalyptusCloudException();
    }
    
    return reply;
  }

  public DescribeAlarmHistoryResponseType describeAlarmHistory(DescribeAlarmHistoryType request) throws EucalyptusCloudException {
    DescribeAlarmHistoryResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DESCRIBEALARMHISTORY, ctx)) {
	throw new EucalyptusCloudException();
    }
   
    return reply;
  }

  public EnableAlarmActionsResponseType enableAlarmActions(EnableAlarmActionsType request) throws EucalyptusCloudException {
    EnableAlarmActionsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_ENABLEALARMACTIONS, ctx)) {
	throw new EucalyptusCloudException();
    }
    
    return reply;
  }

  public DeleteAlarmsResponseType deleteAlarms(DeleteAlarmsType request) throws EucalyptusCloudException {
    DeleteAlarmsResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_DELETEALARMS, ctx)) {
	throw new EucalyptusCloudException();
    }
	
    return reply;
  }

  public SetAlarmStateResponseType setAlarmState(SetAlarmStateType request) throws EucalyptusCloudException {
    SetAlarmStateResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    
    //IAM Action Check
    if(!hasActionPermission(PolicySpec.CLOUDWATCH_SETALARMSTATE, ctx)) {
	throw new EucalyptusCloudException();
    }
	
    return reply;
  }
  
  private Boolean hasActionPermission(final String actionType, final Context ctx) {
	return Permissions.isAuthorized(PolicySpec.VENDOR_CLOUDWATCH,
		actionType, "", ctx.getAccount(), actionType, ctx.getUser());
  }
  
}
