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
 ************************************************************************/
package com.eucalyptus.cloudwatch.backend;

import com.eucalyptus.cloudwatch.common.backend.msgs.AlarmNames;
import com.eucalyptus.cloudwatch.common.backend.msgs.Datapoint;
import com.eucalyptus.cloudwatch.common.backend.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.backend.msgs.DimensionFilter;
import com.eucalyptus.cloudwatch.common.backend.msgs.DimensionFilters;
import com.eucalyptus.cloudwatch.common.backend.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.backend.msgs.ResourceList;
import com.eucalyptus.cloudwatch.common.backend.msgs.StatisticSet;
import com.eucalyptus.cloudwatch.common.backend.msgs.Statistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.google.common.collect.Lists;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CloudWatchBackendServiceFieldValidator {

  private static final String SystemMetricPrefix = "AWS/";

  static void validatePeriodAndEvaluationPeriodsNotAcrossDays(Integer period,
                                                              Integer evaluationPeriods) throws CloudWatchException{
    if (period * evaluationPeriods > 86400) {
      throw new InvalidParameterCombinationException("Metrics cannot be checked across more than a day (EvaluationPeriods * Period must be <= 86400).");
    }
  }

  static Date validateStartDate(Date startDate, boolean required)
      throws CloudWatchException {
    return validateTimestamp(startDate, "StartDate", required);
  }

  static Date validateEndDate(Date endDate, boolean required)
      throws CloudWatchException {
    return validateTimestamp(endDate, "EndDate", required);
  }

  static Units validateUnits(String unit, boolean useNoneIfNull) throws CloudWatchException {
    return validateUnits(unit, "Unit", useNoneIfNull);

  }

  static Date validateTimestamp(Date timestamp, String name, boolean required)
      throws CloudWatchException {
    if (timestamp == null) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      }
    }
    return timestamp;
  }

  static Integer validateEvaluationPeriods(Integer evaluationPeriods,
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

  static String validateMetricName(String metricName, boolean required)
      throws CloudWatchException {
    return validateMetricName(metricName, "MetricName", required);
  }

  static Dimensions validateDimensions(Dimensions dimensions)
      throws CloudWatchException {
    return validateDimensions(dimensions, "Dimensions");
  }

  static Double validateDouble(Double value, String name, boolean required)
      throws CloudWatchException {
    if (value == null) {
      if (required) {
        throw new MissingParameterException("The parameter " + name + " is required.");
      }
    }
    return value;
  }

  static Double validateThreshold(Double threshold, boolean required)
      throws CloudWatchException {
    return validateDouble(threshold, "Threshold", required);
  }

  static AlarmEntity.ComparisonOperator validateComparisonOperator(
    String comparisonOperator, boolean required) throws CloudWatchException {
    return validateEnum(comparisonOperator, "ComparisonOperator",
        AlarmEntity.ComparisonOperator.class, required);
  }

  static String validateAlarmDescription(String alarmDescription) throws CloudWatchException {
    return validateStringLength(alarmDescription, "AlarmDescription", 0, 255, false);
  }

  static Boolean validateActionsEnabled(Boolean actionsEnabled, boolean useTrueIfNull) throws CloudWatchException {
    if (actionsEnabled == null) {
      if (useTrueIfNull) {
        return Boolean.TRUE;
      }
    }
    return actionsEnabled;
  }

  static Collection<String> validateActions(ResourceList actions,
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

  static void validateAction(String action, Map<String, String> dimensionMap,
                             String name) throws CloudWatchException {
    if (AlarmManager.ActionManager.getAction(action, dimensionMap) == null) {
      throw new InvalidParameterValueException("The parameter " + name + "'" + action
          + "' is an unsupported action for this metric and dimension list.");
    }
  }

  static String validateAlarmNamePrefix(String alarmNamePrefix,
                                        boolean required) throws CloudWatchException {
    return validateStringLength(alarmNamePrefix, "AlarmNamePrefix", 1, 255,
        required);
  }

  static String validateActionPrefix(String actionPrefix, boolean required)
      throws CloudWatchException {
    return validateStringLength(actionPrefix, "ActionPrefix", 1, 1024, required);
  }

  static Dimensions validateDimensions(Dimensions dimensions, String name) throws CloudWatchException {
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

  static Units validateUnits(String unit, String name, boolean useNoneIfNull) throws CloudWatchException {
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

  static AlarmEntity.Statistic validateStatistic(String statistic, boolean required)
      throws CloudWatchException {
    return validateEnum(statistic, "Statistic", AlarmEntity.Statistic.class, required);
  }

  static Integer validatePeriod(Integer period, boolean required)
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

  static String validateNamespace(String namespace, boolean required)
      throws CloudWatchException {
    namespace = validateStringLength(namespace, "Namespace", 1, 255, required);
    return namespace;
  }

  static String validateMetricName(String metricName, String name,
                                   boolean required) throws CloudWatchException {
    return validateStringLength(metricName, name, 1, 255, required);
  }

  static void validateDateOrder(Date startDate, Date endDate,
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

  static AlarmHistory.HistoryItemType validateHistoryItemType(String historyItemType,
                                                              boolean required) throws CloudWatchException {
    return validateEnum(historyItemType, "HistoryItemType",
        AlarmHistory.HistoryItemType.class, required);
  }

  static Collection<String> validateAlarmNames(AlarmNames alarmNames,
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

  static <T extends Enum<T>> T validateEnum(String value, String name,
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

  static AlarmEntity.StateValue validateStateValue(String stateValue, boolean required)
      throws CloudWatchException {
    return validateEnum(stateValue, "StateValue", AlarmEntity.StateValue.class, required);
  }

  static String validateStateReasonData(String stateReasonData,
                                        boolean required) throws CloudWatchException {
    stateReasonData = validateStringLength(stateReasonData, "StateReasonData",
        0, 4000, required);
    stateReasonData = validateJSON(stateReasonData, "StateReasonData", required);
    return stateReasonData;
  }

  static String validateJSON(String value, String name, boolean required)
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

  static String validateStringLength(String value, String name, int minLength,
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

  static String validateAlarmName(String alarmName, boolean required)
      throws CloudWatchException {
    return validateAlarmName(alarmName, "AlarmName", required);
  }

  static String validateAlarmName(String alarmNameValue, String alarmNameKey,
                                  boolean required) throws CloudWatchException {
    return validateStringLength(alarmNameValue, alarmNameKey, 1, 255, required);
  }

  static String validateStateReason(String stateReason, boolean required)
      throws CloudWatchException {
    return validateStringLength(stateReason, "StateReason", 0, 1024, required);
  }

  static MetricEntity.MetricType getMetricTypeFromNamespace(String namespace) {
    return namespace.startsWith(SystemMetricPrefix) ? MetricEntity.MetricType.System
        : MetricEntity.MetricType.Custom;
  }

  static void validateNotBothAlarmNamesAndAlarmNamePrefix(
    Collection<String> alarmNames, String alarmNamePrefix)
      throws CloudWatchException {
    if (alarmNames != null && alarmNamePrefix != null) {
      throw new InvalidParameterCombinationException(
          "AlarmNamePrefix and AlarmNames.member are mutually exclusive");
    }
  }

  static Integer validateMaxRecords(Integer maxRecords)
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
}
