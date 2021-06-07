/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class CloudWatchAlarmConfiguration extends EucalyptusData {

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_COMPARISONOPERATOR)
  private String comparisonOperator;

  @FieldRange(max = 10)
  private DimensionList dimensions;

  @Nonnull
  @FieldRange(min = 1)
  private Integer evaluationPeriods;

  @Nonnull
  @FieldRange(min = 1, max = 255)
  private String metricName;

  @Nonnull
  @FieldRange(min = 1, max = 255)
  private String namespace;

  @Nonnull
  @FieldRange(min = 60)
  private Integer period;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_STATISTIC)
  private String statistic;

  @Nonnull
  private Double threshold;

  public String getComparisonOperator() {
    return comparisonOperator;
  }

  public void setComparisonOperator(final String comparisonOperator) {
    this.comparisonOperator = comparisonOperator;
  }

  public DimensionList getDimensions() {
    return dimensions;
  }

  public void setDimensions(final DimensionList dimensions) {
    this.dimensions = dimensions;
  }

  public Integer getEvaluationPeriods() {
    return evaluationPeriods;
  }

  public void setEvaluationPeriods(final Integer evaluationPeriods) {
    this.evaluationPeriods = evaluationPeriods;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(final String metricName) {
    this.metricName = metricName;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  public Integer getPeriod() {
    return period;
  }

  public void setPeriod(final Integer period) {
    this.period = period;
  }

  public String getStatistic() {
    return statistic;
  }

  public void setStatistic(final String statistic) {
    this.statistic = statistic;
  }

  public Double getThreshold() {
    return threshold;
  }

  public void setThreshold(final Double threshold) {
    this.threshold = threshold;
  }

}
