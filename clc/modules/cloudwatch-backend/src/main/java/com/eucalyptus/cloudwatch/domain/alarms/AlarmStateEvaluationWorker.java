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
package com.eucalyptus.cloudwatch.domain.alarms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.backend.CloudWatchBackendService;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricStatistics;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class AlarmStateEvaluationWorker implements Runnable {
  private String accountId;
  private String alarmName;
  private static final Logger LOG = Logger.getLogger(AlarmStateEvaluationWorker.class);
  public AlarmStateEvaluationWorker(String accountId, String alarmName) {
    super();
    this.accountId = accountId;
    this.alarmName = alarmName;
  }
  @Override
  public void run() {
    if (!CloudWatchBackendService.DISABLE_CLOUDWATCH_SERVICE && Bootstrap.isOperational( ) && Topology.isEnabledLocally( CloudWatchBackend.class )) {
      LOG.debug("Kicking off alarm state evaluation for " + alarmName);
      EntityTransaction db = Entities.get(AlarmEntity.class);
      try {
        Criteria criteria = Entities.createCriteria(AlarmEntity.class)
            .add( Restrictions.eq( "accountId" , accountId ) )
            .add( Restrictions.eq( "alarmName" , alarmName ) );
        AlarmEntity alarmEntity = (AlarmEntity) criteria.uniqueResult();
        if (alarmEntity == null) return; // TODO: didn't find it, not good.
        AlarmState currentState = evaluateState(alarmEntity);
        Date evaluationDate = new Date();
        if (currentState.getStateValue() != alarmEntity.getStateValue()) {
          AlarmManager.changeAlarmState(alarmEntity, currentState, evaluationDate);
          AlarmManager.executeActions(alarmEntity, currentState, true, evaluationDate);
        } else if (moreThanOnePeriodHasPassed(alarmEntity, evaluationDate)) {
          AlarmManager.executeActions(alarmEntity, currentState, false, evaluationDate);
        }
        db.commit();
      } catch (RuntimeException ex) { // TODO: exception in a Runnable gets lost...
        Logs.extreme().error(ex, ex);
        throw ex;
      } finally {
        if (db.isActive())
          db.rollback();
      }
    }
  }

  private boolean moreThanOnePeriodHasPassed(AlarmEntity alarmEntity, Date now) {
    now = MetricManager.stripSeconds(now);
    Date then = MetricManager.stripSeconds(alarmEntity.getLastActionsUpdatedTimestamp());
    return now.getTime() - then.getTime() >= 1000L * alarmEntity.getPeriod();
  }

  private AlarmState evaluateState(AlarmEntity alarmEntity) {
    Date queryDate = new Date();
    Date endDate = MetricManager.stripSeconds(queryDate);
    Date startDate = new Date(endDate.getTime() - 1000L * alarmEntity.getPeriod() * alarmEntity.getEvaluationPeriods());
    // We put in a slight buffer in addition to the regular window time (two additional periods or 5 minutes, whichever is greater) to delay
    // insufficient data from going down... 
    Date bufferStartDate = new Date(startDate.getTime() - 1000L * alarmEntity.getPeriod() * numBufferPeriods(alarmEntity.getPeriod()));
    Collection<MetricStatistics> metricStatisticsList = MetricManager.getMetricStatistics(accountId, alarmEntity.getMetricName(), alarmEntity.getNamespace(), alarmEntity.getDimensionMap(), alarmEntity.getMetricType(), alarmEntity.getUnit(), bufferStartDate, endDate, alarmEntity.getPeriod());
    TreeMap<Long, StateAndMetricValue> dataPointMap = new TreeMap<Long, StateAndMetricValue>();
    for (long L = bufferStartDate.getTime(); L < endDate.getTime(); L += alarmEntity.getPeriod() * 1000L) {
      dataPointMap.put(L, new StateAndMetricValue(StateValue.INSUFFICIENT_DATA, null));
    }
    // now populate based on items from the returned values
    for (MetricStatistics metricStatistics: metricStatisticsList) {
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
    if (okPoints.size() > 0) {
      return AlarmManager.createAlarmState(StateValue.OK, okPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
      // it's ok
    } else if (oldestStateValue == StateValue.ALARM) {
      return AlarmManager.createAlarmState(StateValue.ALARM, alarmPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
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
        return AlarmManager.createAlarmState(StateValue.OK, okPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
      } else if (lastNonInsufficientDataStateValue == StateValue.ALARM) {
        return AlarmManager.createAlarmState(StateValue.ALARM, alarmPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
      } else {
        return AlarmManager.createAlarmState(StateValue.INSUFFICIENT_DATA, insufficientDataPoints, relevantDataPoints, alarmEntity.getComparisonOperator(), alarmEntity.getThreshold(), alarmEntity.getPeriod(), queryDate, alarmEntity.getStatistic());
        // (TODO: distinguish the case of complete insufficient data
        // from that of some insufficient data and some alarm, where alarm has not been seen "long enough"
      }
    }
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
