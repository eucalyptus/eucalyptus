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
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricUtils;

public class AlarmUtils {
  private static final Logger LOG = Logger.getLogger(AlarmUtils.class);

  public static Double calculateMetricValue(Statistic statistic, MetricStatistics metricStatistics) {
    Double metricValue = null;
    switch (statistic) {
      case Average:
        metricValue = MetricUtils.average(metricStatistics.getSampleSum(), metricStatistics.getSampleSize());
        break;
      case Minimum:
        metricValue = metricStatistics.getSampleMin();
        break;
      case Maximum:
        metricValue = metricStatistics.getSampleMax();
        break;
      case Sum:
        metricValue = metricStatistics.getSampleSum();
        break;
      case SampleCount:
        metricValue = metricStatistics.getSampleSize();
        break;
      default:
        LOG.warn("Invalid statistic");
    }
    return metricValue;
  }
  
  public static String matchSingularPlural(int number, String singular, String plural) {
    return (number == 1) ? singular : plural;
  }

  public static StateValue calculateStateValue(Double threshold, ComparisonOperator comparisonOperator, Double metricValue) {
    StateValue returnValue = null;
    if (metricValue != null && threshold != null) {
      boolean exceedsThreshold = false;
      switch (comparisonOperator) {
        case LessThanThreshold:
          exceedsThreshold = (metricValue < threshold);
          break;
        case LessThanOrEqualToThreshold:
          exceedsThreshold = (metricValue <= threshold);
          break;
        case GreaterThanThreshold:
          exceedsThreshold = (metricValue > threshold);
          break;
        case GreaterThanOrEqualToThreshold:
          exceedsThreshold = (metricValue >= threshold);
          break;
        default:
          LOG.warn("Invalid comparison operator");
      }
      returnValue =  (exceedsThreshold) ? StateValue.ALARM : StateValue.OK;
    } else {
      returnValue = StateValue.INSUFFICIENT_DATA;
    }
    return returnValue;
  }

  public static String comparisonOperatorString(ComparisonOperator comparisonOperator) {
    String comparisonOperatorStr = "";
    switch (comparisonOperator) {
      case GreaterThanThreshold:
        comparisonOperatorStr = "greater than";
        break;
      case GreaterThanOrEqualToThreshold:
        comparisonOperatorStr = "greater than or equal to";
        break;
      case LessThanThreshold:
        comparisonOperatorStr = "less than";
        break;
      case LessThanOrEqualToThreshold:
        comparisonOperatorStr = "less than or equal to";
        break;
      default:
        LOG.warn("Invalid comparison operator");
    }
    return comparisonOperatorStr;
  }

  public static Collection<String> getActionsByState(AlarmEntity alarmEntity,
      AlarmState state) {
    Collection<String> actions = null;
    switch (state.getStateValue()) {
      case ALARM:
        actions = alarmEntity.getAlarmActions();
        break;
      case OK:
        actions = alarmEntity.getOkActions();
        break;
      case INSUFFICIENT_DATA:
        actions = alarmEntity.getInsufficientDataActions();
        break;
      default:
        actions = new ArrayList<String>();
        // bad place to be.  TODO: log an exception or something
    }
    return actions;
  }

  public static String makeDoubleList(List<Double> dataPoints) {
    StringBuilder builder = new StringBuilder("(");
    String delimiter = "";
    for (Double dataPoint: dataPoints) {
      builder.append(delimiter + dataPoint);
      delimiter = ", ";
    }
    builder.append(")");
    return builder.toString();
  }

}
