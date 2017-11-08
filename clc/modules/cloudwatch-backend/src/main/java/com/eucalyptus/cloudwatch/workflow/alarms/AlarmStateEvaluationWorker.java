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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.cloudwatch.workflow.alarms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmState;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.component.Topology;

public class AlarmStateEvaluationWorker implements Runnable {
  private Collection<AlarmEntity> alarmEntities;
  private static final Logger LOG = Logger.getLogger(AlarmStateEvaluationWorker.class);
  public AlarmStateEvaluationWorker(Collection<AlarmEntity> alarmEntities) {
    super();
    this.alarmEntities = alarmEntities;
  }
  @Override
  public void run() {
    try {
      if (!CloudWatchConfigProperties.isDisabledCloudWatchService() && Bootstrap.isOperational() && Topology.isEnabledLocally(CloudWatchBackend.class)) {
        Date evaluationDate = new Date();
        Map<AlarmEntity, AlarmState> currentStates = evaluateStates(alarmEntities);
        Map<String, AlarmState> statesToUpdate = Maps.newHashMap();
        List<AlarmHistory> historyList = Lists.newArrayList();
        for (AlarmEntity alarmEntity : currentStates.keySet()) {
          AlarmState currentState = currentStates.get(alarmEntity);
          if (currentState.getStateValue() != alarmEntity.getStateValue()) {
            statesToUpdate.put(alarmEntity.getNaturalId(), currentState);
            historyList.add(AlarmManager.createChangeAlarmStateHistoryItem(alarmEntity, currentState, evaluationDate));
            historyList.addAll(AlarmManager.executeActionsAndRecord(alarmEntity, currentState, true, evaluationDate, historyList));
          } else if (moreThanOnePeriodHasPassed(alarmEntity, evaluationDate)) {
            historyList.addAll(AlarmManager.executeActionsAndRecord(alarmEntity, currentState, false, evaluationDate, historyList));
          }
        }
        AlarmManager.changeAlarmStateBatch(statesToUpdate, evaluationDate);
        AlarmManager.addAlarmHistoryEvents(historyList);
      }
    } catch(Exception e) {
      LOG.error(e);
    }
  }

  private boolean moreThanOnePeriodHasPassed(AlarmEntity alarmEntity, Date now) {
    now = MetricUtils.stripSeconds(now);
    Date then = MetricUtils.stripSeconds(alarmEntity.getLastActionsUpdatedTimestamp());
    return now.getTime() - then.getTime() >= 1000L * alarmEntity.getPeriod();
  }

  private Map<AlarmEntity, AlarmState> evaluateStates(Collection<AlarmEntity> alarmEntities) {
    Map<AlarmEntity, AlarmState> returnValue = Maps.newLinkedHashMap();
    Date queryDate = new Date();
    Date endDate = MetricUtils.stripSeconds(queryDate);
    List<MetricManager.GetMetricStatisticsParams> getMetricStatisticsParamses = Lists.newArrayList();
    for (AlarmEntity alarmEntity: alarmEntities) {
      Date startDate = new Date(endDate.getTime() - 1000L * alarmEntity.getPeriod() * alarmEntity.getEvaluationPeriods());
      // We put in a slight buffer in addition to the regular window time (two additional periods or 5 minutes, whichever is greater) to delay
      // insufficient data from going down...
      Date bufferStartDate = new Date(startDate.getTime() - 1000L * alarmEntity.getPeriod() * numBufferPeriods(alarmEntity.getPeriod()));
      getMetricStatisticsParamses.add(new MetricManager.GetMetricStatisticsParams(alarmEntity.getAccountId(), alarmEntity.getMetricName(), alarmEntity.getNamespace(), alarmEntity.getDimensionMap(), alarmEntity.getMetricType(), alarmEntity.getUnit(), bufferStartDate, endDate, alarmEntity.getPeriod()));
    }
    List<Collection<MetricStatistics>> manyMetricsStatisticsList = MetricManager.getManyMetricStatistics(getMetricStatisticsParamses);
    int count = 0;
    for (AlarmEntity alarmEntity: alarmEntities) {
      Date startDate = new Date(endDate.getTime() - 1000L * alarmEntity.getPeriod() * alarmEntity.getEvaluationPeriods());
      // We put in a slight buffer in addition to the regular window time (two additional periods or 5 minutes, whichever is greater) to delay
      // insufficient data from going down...
      Date bufferStartDate = new Date(startDate.getTime() - 1000L * alarmEntity.getPeriod() * numBufferPeriods(alarmEntity.getPeriod()));
      Collection<MetricStatistics> metricStatisticsList = manyMetricsStatisticsList.get(count++);
      TreeMap<Long, StateAndMetricValue> dataPointMap = new TreeMap<Long, StateAndMetricValue>();
      for (long L = bufferStartDate.getTime(); L < endDate.getTime(); L += alarmEntity.getPeriod() * 1000L) {
        dataPointMap.put(L, new StateAndMetricValue(StateValue.INSUFFICIENT_DATA, null));
      }
      // now populate based on items from the returned values
      for (MetricStatistics metricStatistics : metricStatisticsList) {
        Long dateAsLong = metricStatistics.getTimestamp().getTime();
        if (!dataPointMap.containsKey(dateAsLong)) {
          LOG.warn("Data point does not fall in interval, skipping");
        } else {
          dataPointMap.put(dateAsLong, calculateLocalStateAndMetricValue(alarmEntity, metricStatistics));
        }
      }
      // Rules
      // 1) If at least one "OK" interval found in previous "evaluationPeriods" intervals, state is OK
      // 2) If every interval found in previous "evaluationPeriods" intervals is ALARM, state is ALARM
      // 3) If the oldest interval within the previous "evaluationPeriods" intervals is ALARM, and all
      //    intervals since then are either ALARM or INSUFFICIENT data, an alarm state was entered at
      //    the proper time, and we have not yet seen an OK to clear it, so state is ALARM.
      // 4) If the oldest interval within the previous "evaluationPeriods" interval is INSUFFICIENT_DATA,
      //    look backwards into the buffer period.  Set the state to the last known value.  If there are
      //    no known values, set the state to INSUFFICIENT_DATA.  In particular, this means if there is
      //    some ALARM data in the later states, do not set it to ALARM yet, as the time threshold has
      //    not yet passed.
      List<Double> okPoints = new ArrayList<Double>();
      List<Double> alarmPoints = new ArrayList<Double>();
      List<Double> insufficientDataPoints = new ArrayList<Double>();
      LinkedList<Double> relevantDataPoints = new LinkedList<Double>(); // we will add at the beginning sometimes
      StateValue oldestStateValue = null;
      for (long L = startDate.getTime(); L < endDate.getTime(); L += alarmEntity.getPeriod() * 1000L) {
        StateAndMetricValue stateAndMetricValue = dataPointMap.get(L);
        relevantDataPoints.addLast(stateAndMetricValue.getMetricValue()); // newer ones go at the end?
        if (oldestStateValue == null) {
          oldestStateValue = stateAndMetricValue.getStateValue();
        }
        if (stateAndMetricValue.getStateValue() == StateValue.OK) {
          okPoints.add(stateAndMetricValue.getMetricValue());
        } else if (stateAndMetricValue.getStateValue() == StateValue.ALARM) {
          alarmPoints.add(stateAndMetricValue.getMetricValue());
        } else if (stateAndMetricValue.getStateValue() == StateValue.INSUFFICIENT_DATA) {
          insufficientDataPoints.add(stateAndMetricValue.getMetricValue());
        }
      }
      // TODO: we really need to get better reasons, but these are like Amazon's reasons for now.
      AlarmState alarmState;
      if (okPoints.size() > 0) {
        alarmState = AlarmManager.createAlarmState(StateValue.OK, okPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
        // it's ok
      } else if (oldestStateValue == StateValue.ALARM) {
        alarmState = AlarmManager.createAlarmState(StateValue.ALARM, alarmPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
      } else {
        // go back earlier
        StateValue lastNonInsufficientDataStateValue = null;
        for (long L = startDate.getTime() - alarmEntity.getPeriod() * 1000L; L >= bufferStartDate.getTime(); L -= alarmEntity.getPeriod() * 1000L) {
          StateAndMetricValue stateAndMetricValue = dataPointMap.get(L);
          relevantDataPoints.addFirst(stateAndMetricValue.getMetricValue()); // older ones go at the beginning?
          if (stateAndMetricValue.getStateValue() == StateValue.OK) {
            okPoints.add(stateAndMetricValue.getMetricValue());
            lastNonInsufficientDataStateValue = StateValue.OK;
            break;
          } else if (stateAndMetricValue.getStateValue() == StateValue.ALARM) {
            alarmPoints.add(stateAndMetricValue.getMetricValue());
            lastNonInsufficientDataStateValue = StateValue.ALARM;
            break;
          }
        }
        if (lastNonInsufficientDataStateValue == StateValue.OK) {
          alarmState = AlarmManager.createAlarmState(StateValue.OK, okPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
        } else if (lastNonInsufficientDataStateValue == StateValue.ALARM) {
          alarmState = AlarmManager.createAlarmState(StateValue.ALARM, alarmPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
        } else {
          alarmState = AlarmManager.createAlarmState(StateValue.INSUFFICIENT_DATA, insufficientDataPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
          // (TODO: distinguish the case of complete insufficient data
          // from that of some insufficient data and some alarm, where alarm has not been seen "long enough"
        }
      }
      returnValue.put(alarmEntity, alarmState);
    }
    return returnValue;
  }

  private StateAndMetricValue calculateLocalStateAndMetricValue(
    AlarmEntity alarmEntity, MetricStatistics metricStatistics) {
    Double metricValue = AlarmUtils.calculateMetricValue(alarmEntity.getStatistic(), metricStatistics);

    StateValue stateValue = AlarmUtils.calculateStateValue(alarmEntity.getThreshold(), alarmEntity.getComparisonOperator(), metricValue);
    return new StateAndMetricValue(stateValue, metricValue);
  }

  private static class StateAndMetricValue {
    private StateValue stateValue;
    private Double metricValue;

    public StateAndMetricValue(StateValue stateValue, Double metricValue) {
      super();
      this.stateValue = stateValue;
      this.metricValue = metricValue;
    }
    public StateValue getStateValue() {
      return stateValue;
    }
    public Double getMetricValue() {
      return metricValue;
    }
  }
  private Integer numBufferPeriods(Integer period) {
    // it is the greater of 5 minutes or two periods, but it should be a whole number of periods.
    Integer periodMinutes = period / 60;
    if (periodMinutes == 1) return 5;
    if (periodMinutes == 2) return 3;
    return 2;
  }

}
