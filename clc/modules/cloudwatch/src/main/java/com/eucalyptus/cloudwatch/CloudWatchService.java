package com.eucalyptus.cloudwatch;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.domain.dimension.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class CloudWatchService {
  private static final Logger LOG = Logger.getLogger(CloudWatchService.class);
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

    final Context ctx = Contexts.lookup( );
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
