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
package com.eucalyptus.cloudwatch.service;

import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmHistory;
import com.eucalyptus.cloudwatch.common.internal.domain.alarms.AlarmManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricStatistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.eucalyptus.cloudwatch.common.msgs.AlarmNames;
import com.eucalyptus.cloudwatch.common.msgs.Datapoint;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilter;
import com.eucalyptus.cloudwatch.common.msgs.DimensionFilters;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.ResourceList;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.cloudwatch.common.msgs.Statistics;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CloudWatchServiceFieldValidator {

  private static final String SystemMetricPrefix = "AWS/";

  static void validateNotTooManyDataPoints(Date startTime, Date endTime,
                                           Integer period, long maxDataPoints) throws CloudWatchException {
    NumberFormat nf = NumberFormat.getInstance();
    long possibleRequestedDataPoints = (endTime.getTime() - startTime.getTime()) / (1000L * period);
    if (possibleRequestedDataPoints > maxDataPoints) {
      throw new InvalidParameterCombinationException("You have requested up to " + nf.format(possibleRequestedDataPoints)+ " datapoints, which exceeds the limit of " + nf.format(maxDataPoints) + ". You may reduce the datapoints requested by increasing Period, or decreasing the time range.");
    }
  }

  static int NUM_SYSTEM_METRIC_DATA_POINTS = 50;
  static int NUM_CUSTOM_METRIC_DATA_POINTS = 20;
  static List<MetricDatum> validateMetricData(MetricData metricData, MetricType metricType) throws CloudWatchException {
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
    if (metricDataCollection.size() > NUM_CUSTOM_METRIC_DATA_POINTS && metricType == MetricType.Custom) {
      throw new InvalidParameterValueException(
        "The collection MetricData must not have a size greater than " + NUM_CUSTOM_METRIC_DATA_POINTS);
    }
    if (metricDataCollection.size() > NUM_SYSTEM_METRIC_DATA_POINTS && metricType == MetricType.Custom) {
      throw new InvalidParameterValueException(
          "The collection MetricData must not have a size greater than " + NUM_SYSTEM_METRIC_DATA_POINTS + " (for system metrics)");
    }
    int ctr = 1;
    for (MetricDatum metricDatum : metricDataCollection) {
      validateMetricDatum(metricDatum, "MetricData.member." + ctr);
      ctr++;
    }
    return metricDataCollection;
  }

  static Date validateStartTime(Date startTime, boolean required)
      throws CloudWatchException {
    return validateTimestamp(startTime, "StartTime", required);
  }

  static Date validateEndTime(Date endTime, boolean required)
      throws CloudWatchException {
    return validateTimestamp(endTime, "EndTime", required);
  }

  static MetricDatum validateMetricDatum(MetricDatum metricDatum, String name) throws CloudWatchException {
    if (metricDatum == null) {
      throw new MissingParameterException("The parameter " + name + " is required.");
    }
    validateDimensions(metricDatum.getDimensions(), name + ".Dimensions");
    validateDimensionsNoDuplicateKeys(metricDatum.getDimensions());
    validateMetricName(metricDatum.getMetricName(), name + ".MetricName",
        true);
    validateWithinTwoWeeks(metricDatum.getTimestamp(), name + "Timestamp");
    validateUnits(metricDatum.getUnit(), name + ".Unit", true);
    validateValueAndStatisticSet(metricDatum.getValue(), name + ".Value",
          metricDatum.getStatisticValues(), name + ".StatisticValues");
    return metricDatum;
  }

  static void validateDimensionsNoDuplicateKeys(Dimensions dimensions) throws InvalidParameterValueException {
    Set<String> seenDimensionNames = Sets.newHashSet();
    if (dimensions != null && dimensions.getMember() != null) {
      for (Dimension dimension: dimensions.getMember()) {
        if (seenDimensionNames.contains(dimension.getName())) {
          throw new InvalidParameterValueException("No Metric may specify the same dimension twice.");
        } else {
          seenDimensionNames.add(dimension.getName());
        }
      }
    }
  }

  static void validateWithinTwoWeeks(Date timestamp, String name) throws CloudWatchException {
    if (timestamp == null) return;
    Date now = new Date();
    Date twoWeeksAgo = new Date(now.getTime() - 2 * 7 * 24 * 3600 * 1000L);
    long BUFFER = 2 * 3600 * 1000L; // two hours
    if (timestamp.getTime() > now.getTime() + BUFFER || timestamp.getTime() < twoWeeksAgo.getTime() - BUFFER) {
      throw new InvalidParameterValueException("The parameter " + name + ".Timestamp must specify a time within the past two weeks.");
    }
  }

  static void validateValueAndStatisticSet(Double value, String valueName, StatisticSet statisticValues, String statisticValuesName) throws CloudWatchException {
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

  static void validateAllStatisticSetFields(StatisticSet statisticValues, String statisticValuesName) throws CloudWatchException {
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

  static boolean statisticSetHasNoFields(StatisticSet statisticValues) {
    return (statisticValues == null) || (
        (statisticValues.getMaximum()) == null && (statisticValues.getMinimum() == null) &&
        (statisticValues.getSampleCount()) == null && (statisticValues.getSum() == null));
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

  static String validateMetricName(String metricName, boolean required)
      throws CloudWatchException {
    return validateMetricName(metricName, "MetricName", required);
  }

  static Dimensions validateDimensions(Dimensions dimensions)
      throws CloudWatchException {
    return validateDimensions(dimensions, "Dimensions");
  }

  static DimensionFilters validateDimensionFilters(
    DimensionFilters dimensionFilters)
      throws CloudWatchException {
    return validateDimensionFilters(dimensionFilters, "Dimensions");
  }

  static Statistics validateStatistics(Statistics statistics)
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

  static DimensionFilters validateDimensionFilters(
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

  static MetricEntity.MetricType getMetricTypeFromNamespace(String namespace) {
    return namespace.startsWith(SystemMetricPrefix) ? MetricEntity.MetricType.System
        : MetricEntity.MetricType.Custom;
  }

  static ArrayList<Datapoint> convertMetricStatisticsToDatapoints(
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
