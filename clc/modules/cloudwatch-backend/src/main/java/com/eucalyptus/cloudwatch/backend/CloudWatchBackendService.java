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

package com.eucalyptus.cloudwatch.backend;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.CloudWatchMetadata;
import com.eucalyptus.cloudwatch.common.backend.msgs.AlarmHistoryItem;
import com.eucalyptus.cloudwatch.common.backend.msgs.AlarmHistoryItems;
import com.eucalyptus.cloudwatch.common.backend.msgs.AlarmNames;
import com.eucalyptus.cloudwatch.common.backend.msgs.Datapoint;
import com.eucalyptus.cloudwatch.common.backend.msgs.Datapoints;
import com.eucalyptus.cloudwatch.common.backend.msgs.DeleteAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DeleteAlarmsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmHistoryResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmHistoryType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmsForMetricResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmsForMetricType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DescribeAlarmsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.backend.msgs.DimensionFilter;
import com.eucalyptus.cloudwatch.common.backend.msgs.DimensionFilters;
import com.eucalyptus.cloudwatch.common.backend.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.backend.msgs.DisableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.DisableAlarmActionsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.EnableAlarmActionsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.EnableAlarmActionsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.GetMetricStatisticsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.GetMetricStatisticsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.ListMetricsResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.ListMetricsResult;
import com.eucalyptus.cloudwatch.common.backend.msgs.ListMetricsType;
import com.eucalyptus.cloudwatch.common.backend.msgs.Metric;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricAlarm;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricAlarms;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.backend.msgs.Metrics;
import com.eucalyptus.cloudwatch.common.backend.msgs.PutMetricAlarmResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.PutMetricAlarmType;
import com.eucalyptus.cloudwatch.common.backend.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.backend.msgs.ResourceList;
import com.eucalyptus.cloudwatch.common.backend.msgs.SetAlarmStateResponseType;
import com.eucalyptus.cloudwatch.common.backend.msgs.SetAlarmStateType;
import com.eucalyptus.cloudwatch.common.backend.msgs.StatisticSet;
import com.eucalyptus.cloudwatch.common.backend.msgs.Statistics;
import com.eucalyptus.cloudwatch.domain.DBCleanupService;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmHistory.HistoryItemType;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmManager.ActionManager;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmStateEvaluationDispatcher;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricDataQueue;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricUtils;
import com.eucalyptus.component.Faults;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "cloudwatch", description = "Parameters controlling cloud watch and reporting")
public class CloudWatchBackendService {

  @ConfigurableField(initial = "false", description = "Set this to true to stop cloud watch alarm evaluation and new alarm/metric data entry")
  public static volatile Boolean DISABLE_CLOUDWATCH_SERVICE = false;

  static {
    // TODO: make this configurable
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
    ScheduledExecutorService alarmWorkerService = Executors
        .newSingleThreadScheduledExecutor();
    alarmWorkerService.scheduleAtFixedRate(new AlarmStateEvaluationDispatcher(
        fixedThreadPool), 0, 1, TimeUnit.MINUTES);
    ScheduledExecutorService dbCleanupService = Executors
        .newSingleThreadScheduledExecutor();
    dbCleanupService.scheduleAtFixedRate(new DBCleanupService(), 0, 1,
        TimeUnit.DAYS);
  }

  private static final Logger LOG = Logger.getLogger(CloudWatchBackendService.class);

  private static final String SystemMetricPrefix = "AWS/";

  public PutMetricAlarmResponseType putMetricAlarm(PutMetricAlarmType request)
      throws CloudWatchException {
    PutMetricAlarmResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(PolicySpec.CLOUDWATCH_PUTMETRICALARM, ctx);
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final Boolean actionsEnabled = validateActionsEnabled(request.getActionsEnabled(), true);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionsToMap.INSTANCE
          .apply(validateDimensions(request.getDimensions()));
      final Collection<String> alarmActions = validateActions(
          request.getAlarmActions(), dimensionMap, "AlarmActions");
      final String alarmDescription = validateAlarmDescription(
          request.getAlarmDescription());
      final String alarmName = validateAlarmName(request.getAlarmName(), true);
      final ComparisonOperator comparisonOperator = validateComparisonOperator(
          request.getComparisonOperator(), true);
      Integer evaluationPeriods = validateEvaluationPeriods(
          request.getEvaluationPeriods(), true);
      final Integer period = validatePeriod(request.getPeriod(), true);
      validatePeriodAndEvaluationPeriodsNotAcrossDays(period, evaluationPeriods);
      final Collection<String> insufficientDataActions = validateActions(
          request.getInsufficientDataActions(), dimensionMap,
          "InsufficientDataActions");
      final String metricName = validateMetricName(request.getMetricName(),
          true);
      final String namespace = validateNamespace(request.getNamespace(), true);
      final Collection<String> okActions = validateActions(
          request.getOkActions(), dimensionMap, "OKActions");
      final Statistic statistic = validateStatistic(request.getStatistic(),
          true);
      final Double threshold = validateThreshold(request.getThreshold(), true);
      final Units unit = validateUnits(request.getUnit(), true);
      if (AlarmManager.countMetricAlarms(accountId) >= 5000) {
        throw new LimitExceededException("The maximum limit of 5000 alarms would be exceeded.");
      }
      AlarmManager.putMetricAlarm(accountId, actionsEnabled, alarmActions,
          alarmDescription, alarmName, comparisonOperator, dimensionMap,
          evaluationPeriods, insufficientDataActions, metricName,
          getMetricTypeFromNamespace(namespace), namespace, okActions, period,
          statistic, threshold, unit);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public PutMetricDataResponseType putMetricData(PutMetricDataType request)
      throws CloudWatchException {
    PutMetricDataResponseType reply = request.getReply();
    long before = System.currentTimeMillis();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(PolicySpec.CLOUDWATCH_PUTMETRICDATA, ctx);
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final List<MetricDatum> metricData = validateMetricData(request.getMetricData());
      final String namespace = validateNamespace(request.getNamespace(), true);
      final Boolean privileged = Contexts.lookup().isPrivileged();
      LOG.trace("Namespace=" + namespace);
      LOG.trace("metricData="+metricData);
      MetricType metricType = getMetricTypeFromNamespace(namespace);
      if (metricType == MetricType.System && !privileged) {
        throw new InvalidParameterValueException("The value AWS/ for parameter Namespace is invalid.");
      }
      MetricDataQueue.getInstance().insertMetricData(ownerFullName.getAccountNumber(), namespace, metricData, metricType);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public ListMetricsResponseType listMetrics(ListMetricsType request)
      throws CloudWatchException {
    ListMetricsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      // IAM Action Check
      checkActionPermission(PolicySpec.CLOUDWATCH_LISTMETRICS, ctx);

      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String namespace = validateNamespace(request.getNamespace(), false);
      final String metricName = validateMetricName(request.getMetricName(),
          false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionFiltersToMap.INSTANCE
          .apply(validateDimensionFilters(request.getDimensions()));

      // take all stats updated after two weeks ago
      final Date after = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60
          * 60 * 1000L);
      final Date before = null; // no bound on time before stats are updated
      // (though maybe 'now')
      final Integer maxRecords = 500; // per the API docs
      final String nextToken = request.getNextToken();
      final List<ListMetric> results = ListMetricManager.listMetrics(
          ownerFullName.getAccountNumber(), metricName, namespace,
          dimensionMap, after, before, maxRecords, nextToken);

      final Metrics metrics = new Metrics();
      metrics.setMember(Lists.newArrayList(Collections2
          .<ListMetric, Metric> transform(results,
              TransformationFunctions.ListMetricToMetric.INSTANCE)));
      final ListMetricsResult listMetricsResult = new ListMetricsResult();
      listMetricsResult.setMetrics(metrics);
      if (maxRecords != null && results.size() == maxRecords) {
        listMetricsResult.setNextToken(results.get(results.size() - 1)
            .getNaturalId());
      }
      reply.setListMetricsResult(listMetricsResult);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }
  public GetMetricStatisticsResponseType getMetricStatistics(
      GetMetricStatisticsType request) throws CloudWatchException {
    GetMetricStatisticsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    try {
      // IAM Action Check
      checkActionPermission(PolicySpec.CLOUDWATCH_GETMETRICSTATISTICS, ctx);

      // TODO: parse statistics separately()?
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      Statistics statistics = validateStatistics(request.getStatistics());
      final String namespace = validateNamespace(request.getNamespace(), true);
      final String metricName = validateMetricName(request.getMetricName(),
          true);
      final Date startTime = MetricManager.stripSeconds(validateStartTime(request.getStartTime(), true));
      final Date endTime = MetricManager.stripSeconds(validateEndTime(request.getEndTime(), true));
      final Integer period = validatePeriod(request.getPeriod(), true);
      validateDateOrder(startTime, endTime, "StartTime", "EndTime", true,
          true);
      validateNotTooManyDataPoints(startTime, endTime, period, 1440L);
      
      // TODO: null units here does not mean Units.NONE but basically a
      // wildcard.
      // Consider this case.
      final Units units = validateUnits(request.getUnit(), false);
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionsToMap.INSTANCE
          .apply(validateDimensions(request.getDimensions()));
      Collection<MetricStatistics> metrics;
      metrics = MetricManager.getMetricStatistics(
          ownerFullName.getAccountNumber(), metricName, namespace,
          dimensionMap, getMetricTypeFromNamespace(namespace), units,
          startTime, endTime, period);
      reply.getGetMetricStatisticsResult().setLabel(metricName);
      ArrayList<Datapoint> datapoints = convertMetricStatisticsToDataoints(
          statistics, metrics);
      if (datapoints.size() > 0) {
        Datapoints datapointsReply = new Datapoints();
        datapointsReply.setMember(datapoints);
        reply.getGetMetricStatisticsResult().setDatapoints(datapointsReply);
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DisableAlarmActionsResponseType disableAlarmActions(
      final DisableAlarmActionsType request
  ) throws EucalyptusCloudException {
    final DisableAlarmActionsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final Collection<String> alarmNames =
          validateAlarmNames( request.getAlarmNames(), true );
      if ( !AlarmManager.disableAlarmActions(
          accountId,
          alarmNames,
          RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged( ) ) ) {
        throw new EucalyptusCloudException("User does not have permission");
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeAlarmsResponseType describeAlarms(
      final DescribeAlarmsType request
  ) throws CloudWatchException {
    final DescribeAlarmsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      final boolean showAll = request.getAlarms().remove( "verbose" );
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ctx.isAdministrator() && showAll ? null : ownerFullName.getAccountNumber();
      final String actionPrefix = validateActionPrefix(
          request.getActionPrefix(), false);
      final String alarmNamePrefix = validateAlarmNamePrefix(
          request.getAlarmNamePrefix(), false);
      final Collection<String> alarmNames = validateAlarmNames(
          request.getAlarmNames(), false);
      validateNotBothAlarmNamesAndAlarmNamePrefix(alarmNames, alarmNamePrefix);
      final Integer maxRecords = validateMaxRecords(request.getMaxRecords());
      final String nextToken = request.getNextToken();
      final StateValue stateValue = validateStateValue(request.getStateValue(),
          false);
      final List<AlarmEntity> results = AlarmManager.describeAlarms(accountId,
          actionPrefix, alarmNamePrefix, alarmNames, maxRecords, stateValue,
          nextToken, RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged());
      if (maxRecords != null && results.size() == maxRecords) {
        reply.getDescribeAlarmsResult().setNextToken(
            results.get(results.size() - 1).getNaturalId());
      }
      final MetricAlarms metricAlarms = new MetricAlarms();
      metricAlarms.setMember(Lists.newArrayList(Collections2
          .<AlarmEntity, MetricAlarm> transform(results,
              TransformationFunctions.AlarmEntityToMetricAlarm.INSTANCE)));
      reply.getDescribeAlarmsResult().setMetricAlarms(metricAlarms);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeAlarmsForMetricResponseType describeAlarmsForMetric(
    final DescribeAlarmsForMetricType request
  ) throws CloudWatchException {
    final DescribeAlarmsForMetricResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final Map<String, String> dimensionMap = TransformationFunctions.DimensionsToMap.INSTANCE
          .apply(validateDimensions(request.getDimensions()));
      final String metricName = validateMetricName(request.getMetricName(),
          true);
      final String namespace = validateNamespace(request.getNamespace(), true);
      final Integer period = validatePeriod(request.getPeriod(), false);
      final Statistic statistic = validateStatistic(request.getStatistic(),
          false);
      final Units unit = validateUnits(request.getUnit(), true);
      final Collection<AlarmEntity> results = AlarmManager.describeAlarmsForMetric(
          accountId, dimensionMap, metricName, namespace, period, statistic,
          unit, RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged() );
      final MetricAlarms metricAlarms = new MetricAlarms();
      metricAlarms.setMember(Lists.newArrayList(Collections2
          .<AlarmEntity, MetricAlarm> transform(results,
              TransformationFunctions.AlarmEntityToMetricAlarm.INSTANCE)));
      reply.getDescribeAlarmsForMetricResult().setMetricAlarms(metricAlarms);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DescribeAlarmHistoryResponseType describeAlarmHistory(
      final DescribeAlarmHistoryType request
  ) throws CloudWatchException {
    final DescribeAlarmHistoryResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final String alarmName = validateAlarmName(request.getAlarmName(), false);
      final Date endDate = validateEndDate(request.getEndDate(), false);
      final Date startDate = validateStartDate(request.getStartDate(), false);
      validateDateOrder(startDate, endDate, "StartDate", "EndDate", false,
          false);
      final HistoryItemType historyItemType = validateHistoryItemType(
          request.getHistoryItemType(), false);
      final Integer maxRecords = validateMaxRecords(request.getMaxRecords());
      final String nextToken = request.getNextToken();
      final List<AlarmHistory> results = AlarmManager.describeAlarmHistory(
          accountId, alarmName, endDate, historyItemType,
          maxRecords, startDate, nextToken, Predicates.compose(
            RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged(),
            TransformationFunctions.AlarmHistoryToAlarmMetadata.INSTANCE ) );
      if (maxRecords != null && results.size() == maxRecords) {
        reply.getDescribeAlarmHistoryResult().setNextToken(
            results.get(results.size() - 1).getNaturalId());
      }
      final AlarmHistoryItems alarmHistoryItems = new AlarmHistoryItems();
      alarmHistoryItems
          .setMember(Lists.newArrayList(Collections2
              .<AlarmHistory, AlarmHistoryItem> transform(
                  results,
                  TransformationFunctions.AlarmHistoryToAlarmHistoryItem.INSTANCE)));
      reply.getDescribeAlarmHistoryResult().setAlarmHistoryItems(
          alarmHistoryItems);
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public EnableAlarmActionsResponseType enableAlarmActions(
      EnableAlarmActionsType request) throws CloudWatchException {
    final EnableAlarmActionsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    try {
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }

      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final Collection<String> alarmNames =
          validateAlarmNames( request.getAlarmNames(), true );
      if ( !AlarmManager.enableAlarmActions(
          accountId,
          alarmNames,
          RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged( )) ) {
        throw new EucalyptusCloudException("User does not have permission");
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public DeleteAlarmsResponseType deleteAlarms(DeleteAlarmsType request)
      throws CloudWatchException {
    DeleteAlarmsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    try {
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }

      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final Collection<String> alarmNames =
          validateAlarmNames( request.getAlarmNames(), true );
      if ( !AlarmManager.deleteAlarms(
          accountId,
          alarmNames,
          RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged( ) ) ) {
        throw new EucalyptusCloudException("User does not have permission");
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public SetAlarmStateResponseType setAlarmState(SetAlarmStateType request)
      throws CloudWatchException {
    final SetAlarmStateResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    try {
      if (DISABLE_CLOUDWATCH_SERVICE) {
        faultDisableCloudWatchServiceIfNecessary();
        throw new ServiceDisabledException("Service Disabled");
      }
      final OwnerFullName ownerFullName = ctx.getUserFullName();
      final String accountId = ownerFullName.getAccountNumber();
      final String alarmName = validateAlarmName(request.getAlarmName(), true);
      final String stateReason = validateStateReason(request.getStateReason(), true);
      final String stateReasonData = validateStateReasonData(request.getStateReasonData(), false);
      final StateValue stateValue = validateStateValue(request.getStateValue(), true);
      AlarmManager.setAlarmState(
          accountId, alarmName, stateReason, stateReasonData, stateValue,
          RestrictedTypes.<CloudWatchMetadata.AlarmMetadata>filterPrivileged( ) );
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }
  private static final int DISABLED_SERVICE_FAULT_ID = 1500;
  private boolean alreadyFaulted = false;
  private void faultDisableCloudWatchServiceIfNecessary() {
    // TODO Auto-generated method stub
    if (!alreadyFaulted) {
      Faults.forComponent(CloudWatchBackend.class).havingId(DISABLED_SERVICE_FAULT_ID).withVar("component", "cloudwatch").log();
      alreadyFaulted = true;
    }
    
  }

  private void validatePeriodAndEvaluationPeriodsNotAcrossDays(Integer period,
      Integer evaluationPeriods) throws CloudWatchException{
    if (period * evaluationPeriods > 86400) {
      throw new InvalidParameterCombinationException("Metrics cannot be checked across more than a day (EvaluationPeriods * Period must be <= 86400).");
    }
  }


  private void validateNotTooManyDataPoints(Date startTime, Date endTime,
      Integer period, long maxDataPoints) throws CloudWatchException {
    NumberFormat nf = NumberFormat.getInstance();
    long possibleRequestedDataPoints = (endTime.getTime() - startTime.getTime()) / (1000L * period);
    if (possibleRequestedDataPoints > maxDataPoints) {
      throw new InvalidParameterCombinationException("You have requested up to " + nf.format(possibleRequestedDataPoints)+ " datapoints, which exceeds the limit of " + nf.format(maxDataPoints) + ". You may reduce the datapoints requested by increasing Period, or decreasing the time range.");
    }
  }

  private List<MetricDatum> validateMetricData(MetricData metricData) throws CloudWatchException {
    List<MetricDatum> metricDataCollection = null;
    if (metricData != null) {
      metricDataCollection = metricData.getMember();
      ;
    }
    if (metricDataCollection == null) {
      throw new MissingParameterException(
         "The parameter MetricData is required.");
   }
    if (metricDataCollection.size() < 1) {
      throw new MissingParameterException(
          "The parameter MetricData is required.");
    }
    if (metricDataCollection.size() > 20) {
      throw new InvalidParameterValueException(
          "The collection MetricData must not have a size greater than 20.");
    }
    int ctr = 1;
    for (MetricDatum metricDatum : metricDataCollection) {
      validateMetricDatum(metricDatum, "MetricData.member." + ctr);
      ctr++;
    }
    return metricDataCollection;
  }

  private Date validateStartDate(Date startDate, boolean required)
      throws CloudWatchException {
    return validateTimestamp(startDate, "StartDate", required);
  }

  private Date validateEndDate(Date endDate, boolean required)
      throws CloudWatchException {
    return validateTimestamp(endDate, "EndDate", required);
  }

  private Date validateStartTime(Date startTime, boolean required)
      throws CloudWatchException {
    return validateTimestamp(startTime, "StartTime", required);
  }

  private Date validateEndTime(Date endTime, boolean required)
      throws CloudWatchException {
    return validateTimestamp(endTime, "EndTime", required);
  }

  private MetricDatum validateMetricDatum(MetricDatum metricDatum, String name) throws CloudWatchException {
    if (metricDatum == null) {
      throw new MissingParameterException("The parameter " + name + " is required.");
    }
    validateDimensions(metricDatum.getDimensions(), name + ".Dimensions");
    validateMetricName(metricDatum.getMetricName(), name + ".MetricName",
        true);
    validateWithinTwoWeeks(metricDatum.getTimestamp(), name + "Timestamp");
    validateUnits(metricDatum.getUnit(), name + ".Unit", true);
    validateValueAndStatisticSet(metricDatum.getValue(), name + ".Value",
          metricDatum.getStatisticValues(), name + ".StatisticValues");
    return metricDatum;
  }


  private void validateWithinTwoWeeks(Date timestamp, String name) throws CloudWatchException {
    if (timestamp == null) return;
    Date now = new Date();
    Date twoWeeksAgo = new Date(now.getTime() - 2 * 7 * 24 * 3600 * 1000L);
    long BUFFER = 2 * 3600 * 1000L; // two hours
    if (timestamp.getTime() > now.getTime() + BUFFER || timestamp.getTime() < twoWeeksAgo.getTime() - BUFFER) {
      throw new InvalidParameterValueException("The parameter " + name + ".Timestamp must specify a time within the past two weeks.");
    }
  }

  private void validateValueAndStatisticSet(Double value, String valueName, StatisticSet statisticValues, String statisticValuesName) throws CloudWatchException {
    if (value == null && statisticSetHasNoFields(statisticValues)) {
      throw new MissingParameterException("At least one of the parameters " + valueName + " or "
          + statisticValuesName + " must be specified.");
    }
    if (value != null && !statisticSetHasNoFields(statisticValues)) {
      throw new InvalidParameterCombinationException("The parameters " + valueName + " and "
          + statisticValuesName + " are mutually exclusive and you have specified both.");
    }
    if (value != null) return; // value is set
    validateAllStatisticSetFields(statisticValues, statisticValuesName);
    if (statisticValues.getMaximum() < statisticValues.getMinimum()) {
      throw new MissingParameterException("The parameter " + statisticValuesName+ ".Maximum must be greater than " + statisticValuesName + ".Minimum.");
      
    }
    if (statisticValues.getSampleCount() < 0) {
      throw new MissingParameterException("The parameter " + statisticValuesName+ ".SampleCount must be greater than 0.");
    }
    if (statisticValues.getSampleCount() == 0.0) {
      throw new MissingParameterException("The parameter " + statisticValuesName+ ".SampleCount must not equal 0.");
    }
  }

  private void validateAllStatisticSetFields(StatisticSet statisticValues, String statisticValuesName) throws CloudWatchException {
    StringBuilder errors = new StringBuilder();
    boolean haveErrors = false;
    if (statisticValues == null || statisticValues.getMaximum() == null) {
      if (haveErrors) {
        errors.append("\n");
      }
      errors.append("The parameter " + statisticValuesName + ".Maximum is required.");
      haveErrors = true;
    }
    if (statisticValues == null || statisticValues.getMinimum() == null) {
      if (haveErrors) {
        errors.append("\n");
      }
      errors.append("The parameter " + statisticValuesName + ".Minimum is required.");
      haveErrors = true;
    }
    if (statisticValues == null || statisticValues.getSampleCount() == null) {
      if (haveErrors) {
        errors.append("\n");
      }
      errors.append("The parameter " + statisticValuesName + ".SampleCount is required.");
      haveErrors = true;
    }
    if (statisticValues == null || statisticValues.getSum() == null) {
      if (haveErrors) {
        errors.append("\n");
      }
      errors.append("The parameter " + statisticValuesName + ".Sum is required.");
      haveErrors = true;
    }
    if (haveErrors) {
      throw new MissingParameterException(errors.toString());
    }
  }

  private boolean statisticSetHasNoFields(StatisticSet statisticValues) {
    return (statisticValues == null) || (
        (statisticValues.getMaximum()) == null && (statisticValues.getMinimum() == null) &&
        (statisticValues.getSampleCount()) == null && (statisticValues.getSum() == null));
  }

  private Units validateUnits(String unit, boolean useNoneIfNull) throws CloudWatchException {
    return validateUnits(unit, "Unit", useNoneIfNull);

  }

  private Date validateTimestamp(Date timestamp, String name, boolean required)
      throws CloudWatchException {
    if (timestamp == null) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      }
    }
    return timestamp;
  }

  private Integer validateEvaluationPeriods(Integer evaluationPeriods,
      boolean required) throws CloudWatchException {
    if (evaluationPeriods == null) {
      if (required) {
        throw new MissingParameterException(
            "The parameter EvaluationPeriods is required.");
      }
    }
    if (evaluationPeriods < 1) {
      throw new InvalidParameterValueException(
          "The parameter EvaluationPeriods must be greater than or equal to 1.");
    }
    return evaluationPeriods;
  }

  private String validateMetricName(String metricName, boolean required)
      throws CloudWatchException {
    return validateMetricName(metricName, "MetricName", required);
  }

  private Dimensions validateDimensions(Dimensions dimensions)
      throws CloudWatchException {
    return validateDimensions(dimensions, "Dimensions");
  }

  private DimensionFilters validateDimensionFilters(
      DimensionFilters dimensionFilters)
      throws CloudWatchException {
    return validateDimensionFilters(dimensionFilters, "Dimensions");
  }

  private Double validateDouble(Double value, String name, boolean required)
      throws CloudWatchException {
    if (value == null) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      }
    }
    return value;
  }

  private Double validateThreshold(Double threshold, boolean required)
      throws CloudWatchException {
    return validateDouble(threshold, "Threshold", required);
  }

  private ComparisonOperator validateComparisonOperator(
      String comparisonOperator, boolean required) throws CloudWatchException {
    return validateEnum(comparisonOperator, "ComparisonOperator",
        ComparisonOperator.class, required);
  }

  private String validateAlarmDescription(String alarmDescription) throws CloudWatchException {
    return validateStringLength(alarmDescription, "AlarmDescription", 0, 255, false);
  }

  private Boolean validateActionsEnabled(Boolean actionsEnabled, boolean useTrueIfNull) throws CloudWatchException {
    if (actionsEnabled == null) {
      if (useTrueIfNull) {
        return Boolean.TRUE;
      }
    }
    return actionsEnabled;
  }

  private Collection<String> validateActions(ResourceList actions,
      Map<String, String> dimensionMap, String name)
      throws CloudWatchException {
    Collection<String> actionsCollection = null;
    if (actions != null) {
      actionsCollection = actions.getMember();
      ;
    }
    if (actionsCollection == null) {
      return actionsCollection;
    }
    if (actionsCollection.size() > 5) {
      throw new InvalidParameterValueException("The collection " + name
          + " must not have a size greater than 5.");
    }
    int ctr = 1;
    for (String action : actionsCollection) {
      validateAction(action, dimensionMap, name + ".member." + ctr);
      ctr++;
    }
    return actionsCollection;
  }

  private void validateAction(String action, Map<String, String> dimensionMap,
      String name) throws CloudWatchException {
    if (ActionManager.getAction(action, dimensionMap) == null) {
      throw new InvalidParameterValueException("The parameter " + name + "'" + action
          + "' is an unsupported action for this metric and dimension list.");
    }
  }

  private void validateNotBothAlarmNamesAndAlarmNamePrefix(
      Collection<String> alarmNames, String alarmNamePrefix)
      throws CloudWatchException {
    if (alarmNames != null && alarmNamePrefix != null) {
      throw new InvalidParameterCombinationException(
          "AlarmNamePrefix and AlarmNames.member are mutually exclusive");
    }
  }

  private String validateAlarmNamePrefix(String alarmNamePrefix,
      boolean required) throws CloudWatchException {
    return validateStringLength(alarmNamePrefix, "AlarmNamePrefix", 1, 255,
        required);
  }

  private String validateActionPrefix(String actionPrefix, boolean required)
      throws CloudWatchException {
    return validateStringLength(actionPrefix, "ActionPrefix", 1, 1024, required);
  }

  private Statistics validateStatistics(Statistics statistics)
      throws CloudWatchException {
    Collection<String> statisticCollection = null;
    if (statistics != null) {
      statisticCollection = statistics.getMember();
    }
    if (statisticCollection == null) {
      throw new MissingParameterException("The parameter Statistics is required.");
    }
    if (statisticCollection.size() < 1) {
      throw new MissingParameterException("The parameter Statistics is required.");
    }
      
    if (statisticCollection.size() > 5) {
      throw new InvalidParameterValueException(
          "The collection MetricData must not have a size greater than 5.");
    }
    int ctr = 1;
    String[] statisticValues = new String[] { "Average", "Sum", "SampleCount",
        "Maximum", "Minimum" };
    for (String statistic : statisticCollection) {
      if (statistic == null) {
        throw new InvalidParameterValueException("The parameter Statistics.member." + ctr
            + " is required.");
      }
      if (!Arrays.asList(statisticValues).contains(statistic)) {
        throw new InvalidParameterValueException("The parameter Statistics.member." + ctr
            + " must be a value in the set " + Arrays.asList(statisticValues) + ".");
      }
      ctr++;
    }
    return statistics;
  }

  private DimensionFilters validateDimensionFilters(
      DimensionFilters dimensionFilters, String name)
      throws CloudWatchException {
    Collection<DimensionFilter> dimensionFiltersCollection = null;
    if (dimensionFilters != null) {
      dimensionFiltersCollection = dimensionFilters.getMember();
    }
    if (dimensionFiltersCollection == null) {
      return dimensionFilters;
    }
    if (dimensionFiltersCollection.size() > 10) {
      throw new InvalidParameterValueException("The collection " + name
          + " must not have a size greater than 10.");
    }
    int ctr = 1;
    for (DimensionFilter dimensionFilter : dimensionFiltersCollection) {
      validateStringLength(dimensionFilter.getName(), name + ".member." + (ctr)
          + ".Name", 1, 255, true);
      validateStringLength(dimensionFilter.getValue(), name + ".member."
          + (ctr) + ".Value", 1, 255, true);
      ctr++;
    }
    return dimensionFilters;
  }

  private Dimensions validateDimensions(Dimensions dimensions, String name) throws CloudWatchException {
    Collection<Dimension> dimensionsCollection = null;
    if (dimensions != null) {
      dimensionsCollection = dimensions.getMember();
    }
    if (dimensions == null) {
      return dimensions;
    }
    if (dimensionsCollection.size() > 10) {
      throw new InvalidParameterValueException("The collection " + name
          + " must not have a size greater than 10.");
    }
    int ctr = 1;
    for (Dimension dimension : dimensionsCollection) {
      validateStringLength(dimension.getName(), name + ".member." + (ctr)
          + ".Name", 1, 255, true);
      validateStringLength(dimension.getValue(), name + ".member." + (ctr)
          + ".Value", 1, 255, true);
      ctr++;
    }
    return dimensions;
  }

  private Units validateUnits(String unit, String name, boolean useNoneIfNull) throws CloudWatchException {
    if (unit == null) {
      if (useNoneIfNull) {
        return Units.None;
      } else {
        return null;
      }
    }
    try {
      return Units.fromValue(unit);
    } catch (IllegalArgumentException ex) {
      throw new InvalidParameterValueException("The parameter " + name + " must be a value in the set "
          + Arrays.asList(Units.values()) +".");
    }
  }

  private Statistic validateStatistic(String statistic, boolean required)
      throws CloudWatchException {
    return validateEnum(statistic, "Statistic", Statistic.class, required);
  }

  private Integer validatePeriod(Integer period, boolean required)
      throws CloudWatchException {
    if (period == null) {
      if (required) {
        throw new MissingParameterException("The parameter Period is required.");
      } else {
        return period;
      }
    }
    if (period < 0) {
      throw new InvalidParameterValueException("The parameter Period must be greater than 0.");
    }
    if (period == 0) {
      throw new InvalidParameterValueException("The parameter Period must not equal 0.");
    }
    if (period % 60 != 0) {
      throw new InvalidParameterValueException(
          "The parameter Period must be a multiple of 60.");
    }
    return period;
  }

  private String validateNamespace(String namespace, boolean required)
      throws CloudWatchException {
    namespace = validateStringLength(namespace, "Namespace", 1, 255, required);
    return namespace;
  }

  private String validateMetricName(String metricName, String name,
      boolean required) throws CloudWatchException {
    return validateStringLength(metricName, name, 1, 255, required);
  }

  private Integer validateMaxRecords(Integer maxRecords)
      throws CloudWatchException {
    if (maxRecords == null) {
      return 50; // this is from experimentation
    }
    if (maxRecords < 1) {
      throw new InvalidParameterValueException("The parameter MaxRecords must be greater than or equal to 1.");
    }
    if (maxRecords > 100) {
      throw new InvalidParameterValueException("The parameter MaxRecords must be less than or equal to 100.");
    }
    return maxRecords;
  }

  private void validateDateOrder(Date startDate, Date endDate,
      String startDateName, String endDateName, boolean startDateRequired,
      boolean endDateRequired) throws CloudWatchException {
    if (startDate != null && endDate != null) {
      if (startDate.after(endDate)) {
        throw new InvalidParameterValueException("The parameter " + endDateName 
            + " must be greater than " + startDateName + ".");
      }
      if (startDate.equals(endDate)) {
        throw new InvalidParameterValueException("The parameter " + startDateName 
            + " must not equal parameter " + endDateName + ".");
      }
    }
    if (startDate == null && startDateRequired) {
      throw new MissingParameterException("The parameter " + startDateName
          + " is required.");
    }
    if (endDate == null && endDateRequired) {
      throw new MissingParameterException("The parameter " + endDateName
          + " is required.");
    }
  }

  private HistoryItemType validateHistoryItemType(String historyItemType,
      boolean required) throws CloudWatchException {
    return validateEnum(historyItemType, "HistoryItemType",
        HistoryItemType.class, required);
  }

  private Collection<String> validateAlarmNames(AlarmNames alarmNames,
      boolean required) throws CloudWatchException {
    Collection<String> alarmNamesCollection = null;
    if (alarmNames != null) {
      alarmNamesCollection = alarmNames.getMember();
    }
    if (alarmNamesCollection == null) {
      if (required) {
        throw new MissingParameterException(
            "The parameter AlarmNames is required.");
      }
      return alarmNamesCollection;
    }
    if (alarmNamesCollection.size() < 1 && required) {
      throw new MissingParameterException(
          "The parameter AlarmNames is required.");
    }
        
    if (alarmNamesCollection.size() > 100) {
      throw new InvalidParameterValueException(
          "The collection AlarmNames must not have a size greater than 100.");
    }
    int ctr = 1;
    for (String alarmName : alarmNamesCollection) {
      validateAlarmName(alarmName, "AlarmName.member." + (ctr++), true);
    }
    return alarmNamesCollection;
  }

  private <T extends Enum<T>> T validateEnum(String value, String name,
      Class<T> enumType, boolean required) throws CloudWatchException {
    try {
      return Enum.valueOf(enumType, value);
    } catch (IllegalArgumentException ex) {
      throw new InvalidParameterValueException("The parameter " + name + " must be a value in the set "
          + Arrays.asList(enumType.getEnumConstants()) + ".");
    } catch (NullPointerException ex) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      }
      return null;
    }
  }

  private StateValue validateStateValue(String stateValue, boolean required)
      throws CloudWatchException {
    return validateEnum(stateValue, "StateValue", StateValue.class, required);
  }

  private String validateStateReasonData(String stateReasonData,
      boolean required) throws CloudWatchException {
    stateReasonData = validateStringLength(stateReasonData, "StateReasonData",
        0, 4000, required);
    stateReasonData = validateJSON(stateReasonData, "StateReasonData", required);
    return stateReasonData;
  }

  private String validateJSON(String value, String name, boolean required)
      throws CloudWatchException {
    if (value == null) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      } else {
        return value;
      }
    }
    try {
      JSONSerializer.toJSON(value);
    } catch (JSONException ex) {
      throw new InvalidFormatException(name
          + " was not syntactically valid JSON");
    }
    return value;
  }

  private String validateStringLength(String value, String name, int minLength,
      int maxLength, boolean required) throws CloudWatchException {
    if (value == null || value.isEmpty() ) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      } else {
        return value;
      }
    }
    if (value.length() < minLength) {
      throw new InvalidParameterValueException("The parameter " + name + " must be longer than " + (minLength - 1) + " character" + ((minLength == 2) ? "" : "s" ) + ".");
    }
    if (value.length() > maxLength) {
      throw new InvalidParameterValueException("The parameter " + name + " must be shorter than " + (maxLength + 1) + " character" + ((maxLength == 0) ? "" : "s" ) + ".");
    }
    return value;
  }

  private String validateAlarmName(String alarmName, boolean required)
      throws CloudWatchException {
    return validateAlarmName(alarmName, "AlarmName", required);
  }

  private String validateAlarmName(String alarmNameValue, String alarmNameKey,
      boolean required) throws CloudWatchException {
    return validateStringLength(alarmNameValue, alarmNameKey, 1, 255, required);
  }

  private String validateStateReason(String stateReason, boolean required)
      throws CloudWatchException {
    return validateStringLength(stateReason, "StateReason", 0, 1024, required);
  }

  private void checkActionPermission(final String actionType, final Context ctx)
      throws EucalyptusCloudException {
    if (!Permissions.isAuthorized(PolicySpec.VENDOR_CLOUDWATCH, actionType, "",
        ctx.getAccount(), actionType, ctx.getAuthContext())) {
      throw new EucalyptusCloudException("User does not have permission");
    }
  }

  private static void handleException(final Exception e)
      throws CloudWatchException {
    final CloudWatchException cause = Exceptions.findCause(e,
        CloudWatchException.class);
    if (cause != null) {
      throw cause;
    }

    final InternalFailureException exception = new InternalFailureException(
        String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }

  private MetricType getMetricTypeFromNamespace(String namespace) {
    return namespace.startsWith(SystemMetricPrefix) ? MetricType.System
        : MetricType.Custom;
  }

  private ArrayList<Datapoint> convertMetricStatisticsToDataoints(
      Statistics statistics, Collection<MetricStatistics> metrics) {
    ArrayList<Datapoint> datapoints = Lists.newArrayList();
    boolean wantsAverage = statistics.getMember().contains("Average");
    boolean wantsSum = statistics.getMember().contains("Sum");
    boolean wantsSampleCount = statistics.getMember().contains("SampleCount");
    boolean wantsMaximum = statistics.getMember().contains("Maximum");
    boolean wantsMinimum = statistics.getMember().contains("Minimum");
    for (MetricStatistics metricStatistics : metrics) {
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
        datapoint.setAverage(MetricUtils.average(
            metricStatistics.getSampleSum(), metricStatistics.getSampleSize()));
      }
      datapoints.add(datapoint);
    }
    return datapoints;
  }

}
