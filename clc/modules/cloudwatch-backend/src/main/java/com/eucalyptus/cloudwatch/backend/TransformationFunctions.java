/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch.backend;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.cloudwatch.common.CloudWatchMetadata;
import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.common.msgs.AlarmHistoryItem;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilter;
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilters;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.Metric;
import com.eucalyptus.cloudwatch.common.msgs.MetricAlarm;
import com.eucalyptus.cloudwatch.common.msgs.ResourceList;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TransformationFunctions {

  private static final Logger LOG = Logger
      .getLogger(TransformationFunctions.class);

  public TransformationFunctions() {
  }

  enum DimensionEntityToDimension implements
  Function<DimensionEntity, Dimension> {
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

  enum ListMetricToMetric implements Function<ListMetric, Metric> {
    INSTANCE {
      @Override
      public Metric apply(@Nullable ListMetric listMetric) {
        Metric metric = new Metric();
        metric.setMetricName(listMetric.getMetricName());
        metric.setNamespace(listMetric.getNamespace());
        Dimensions dimensions = new Dimensions();
        dimensions.setMember(Lists.newArrayList(Collections2
            .<DimensionEntity, Dimension> transform(listMetric.getDimensions(),
                TransformationFunctions.DimensionEntityToDimension.INSTANCE)));
        metric.setDimensions(dimensions);
        return metric;
      }
    }
  }

  enum AlarmEntityToMetricAlarm implements Function<AlarmEntity, MetricAlarm> {
    INSTANCE {
      @Override
      public MetricAlarm apply(@Nullable AlarmEntity alarmEntity) {
        LOG.trace("OK_ACTIONS=" + alarmEntity.getOkActions());
        LOG.trace("ALARM_ACTIONS=" + alarmEntity.getAlarmActions());
        LOG.trace("INSUFFICIENT_DATA_ACTIONS="
            + alarmEntity.getInsufficientDataActions());
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
        metricAlarm.setAlarmConfigurationUpdatedTimestamp(alarmEntity
            .getAlarmConfigurationUpdatedTimestamp());
        metricAlarm.setAlarmDescription(alarmEntity.getAlarmDescription());
        metricAlarm.setAlarmName(alarmEntity.getAlarmName());
        metricAlarm
        .setComparisonOperator(alarmEntity.getComparisonOperator() == null ? null
            : alarmEntity.getComparisonOperator().toString());
        Dimensions dimensions = new Dimensions();
        dimensions.setMember(Lists.newArrayList(Collections2
            .<DimensionEntity, Dimension> transform(
                alarmEntity.getDimensions(),
                TransformationFunctions.DimensionEntityToDimension.INSTANCE)));
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
        metricAlarm.setStateUpdatedTimestamp(alarmEntity
            .getStateUpdatedTimestamp());
        metricAlarm.setStateValue(alarmEntity.getStateValue() == null ? null
            : alarmEntity.getStateValue().toString());
        metricAlarm.setStatistic(alarmEntity.getStatistic() == null ? null
            : alarmEntity.getStatistic().toString());
        metricAlarm.setThreshold(alarmEntity.getThreshold());
        metricAlarm.setUnit(alarmEntity.getUnit() == null ? null : alarmEntity
            .getUnit().toString());

        ResourceList insufficientDataActions = new ResourceList();
        ArrayList<String> insufficientDataActionsMember = new ArrayList<String>();
        if (alarmEntity.getInsufficientDataActions() != null) {
          insufficientDataActionsMember.addAll(alarmEntity
              .getInsufficientDataActions());
        }
        insufficientDataActions.setMember(insufficientDataActionsMember);
        metricAlarm.setInsufficientDataActions(insufficientDataActions);
        return metricAlarm;
      }
    }
  }

  enum AlarmHistoryToAlarmMetadata implements
      Function<AlarmHistory, CloudWatchMetadata.AlarmMetadata> {
    INSTANCE {
      @Override
      public CloudWatchMetadata.AlarmMetadata apply( final AlarmHistory alarmHistory ) {
        return new CloudWatchMetadata.AlarmMetadata( ){
          @Override
          public String getDisplayName() {
            return alarmHistory.getAlarmName();
          }

          @Override
          public OwnerFullName getOwner() {
            return AccountFullName.getInstance( alarmHistory.getAccountId() );
          }
        };
      }
    }
  }

  enum AlarmHistoryToAlarmHistoryItem implements
  Function<AlarmHistory, AlarmHistoryItem> {
    INSTANCE {
      @Override
      public AlarmHistoryItem apply(@Nullable AlarmHistory alarmHistory) {
        AlarmHistoryItem alarmHistoryItem = new AlarmHistoryItem();
        alarmHistoryItem.setAlarmName(alarmHistory.getAlarmName());
        alarmHistoryItem.setHistoryData(alarmHistory.getHistoryData());
        alarmHistoryItem
        .setHistoryItemType(alarmHistory.getHistoryItemType() == null ? null
            : alarmHistory.getHistoryItemType().toString());
        alarmHistoryItem.setHistorySummary(alarmHistory.getHistorySummary());
        alarmHistoryItem.setTimestamp(alarmHistory.getTimestamp());
        return alarmHistoryItem;
      }
    }
  }

  enum DimensionFiltersToMap implements
  Function<DimensionFilters, Map<String, String>> {
    INSTANCE {
      @Override
      public Map<String, String> apply(
          @Nullable DimensionFilters dimensionFilters) {
        final Map<String, String> result = Maps.newHashMap();
        if (dimensionFilters != null && dimensionFilters.getMember() != null) {
          for (DimensionFilter dimensionFilter : dimensionFilters.getMember()) {
            result.put(dimensionFilter.getName(), dimensionFilter.getValue());
          }
        }
        return result;
      }
    }
  }

  enum DimensionsToMap implements Function<Dimensions, Map<String, String>> {
    INSTANCE;

    @Nonnull
    @Override
    public Map<String, String> apply(@Nullable Dimensions dimensions) {
      final Map<String, String> result = Maps.newHashMap();
      if (dimensions != null && dimensions.getMember() != null) {
        for (Dimension dimension : dimensions.getMember()) {
          result.put(dimension.getName(), dimension.getValue());
        }
      }
      return result;
    }
  }
}
