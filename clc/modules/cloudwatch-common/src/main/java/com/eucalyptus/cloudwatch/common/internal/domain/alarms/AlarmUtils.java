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

package com.eucalyptus.cloudwatch.common.internal.domain.alarms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity.ComparisonOperator;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity.StateValue;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity.Statistic;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;

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
