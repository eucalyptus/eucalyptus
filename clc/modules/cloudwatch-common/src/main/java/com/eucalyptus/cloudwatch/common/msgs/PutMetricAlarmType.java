/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudwatch.common.msgs;

import com.eucalyptus.binding.HttpParameterMapping;

public class PutMetricAlarmType extends CloudWatchMessage {

  private String alarmName;
  private String alarmDescription;
  private Boolean actionsEnabled;
  @HttpParameterMapping( parameter = "OKActions" )
  private ResourceList okActions;
  private ResourceList alarmActions;
  private ResourceList insufficientDataActions;
  private String metricName;
  private String namespace;
  private String statistic;
  private Dimensions dimensions;
  private Integer period;
  private String unit;
  private Integer evaluationPeriods;
  private Double threshold;
  private String comparisonOperator;

  public String getAlarmName( ) {
    return alarmName;
  }

  public void setAlarmName( String alarmName ) {
    this.alarmName = alarmName;
  }

  public String getAlarmDescription( ) {
    return alarmDescription;
  }

  public void setAlarmDescription( String alarmDescription ) {
    this.alarmDescription = alarmDescription;
  }

  public Boolean getActionsEnabled( ) {
    return actionsEnabled;
  }

  public void setActionsEnabled( Boolean actionsEnabled ) {
    this.actionsEnabled = actionsEnabled;
  }

  public ResourceList getOkActions( ) {
    return okActions;
  }

  public void setOkActions( ResourceList okActions ) {
    this.okActions = okActions;
  }

  public ResourceList getAlarmActions( ) {
    return alarmActions;
  }

  public void setAlarmActions( ResourceList alarmActions ) {
    this.alarmActions = alarmActions;
  }

  public ResourceList getInsufficientDataActions( ) {
    return insufficientDataActions;
  }

  public void setInsufficientDataActions( ResourceList insufficientDataActions ) {
    this.insufficientDataActions = insufficientDataActions;
  }

  public String getMetricName( ) {
    return metricName;
  }

  public void setMetricName( String metricName ) {
    this.metricName = metricName;
  }

  public String getNamespace( ) {
    return namespace;
  }

  public void setNamespace( String namespace ) {
    this.namespace = namespace;
  }

  public String getStatistic( ) {
    return statistic;
  }

  public void setStatistic( String statistic ) {
    this.statistic = statistic;
  }

  public Dimensions getDimensions( ) {
    return dimensions;
  }

  public void setDimensions( Dimensions dimensions ) {
    this.dimensions = dimensions;
  }

  public Integer getPeriod( ) {
    return period;
  }

  public void setPeriod( Integer period ) {
    this.period = period;
  }

  public String getUnit( ) {
    return unit;
  }

  public void setUnit( String unit ) {
    this.unit = unit;
  }

  public Integer getEvaluationPeriods( ) {
    return evaluationPeriods;
  }

  public void setEvaluationPeriods( Integer evaluationPeriods ) {
    this.evaluationPeriods = evaluationPeriods;
  }

  public Double getThreshold( ) {
    return threshold;
  }

  public void setThreshold( Double threshold ) {
    this.threshold = threshold;
  }

  public String getComparisonOperator( ) {
    return comparisonOperator;
  }

  public void setComparisonOperator( String comparisonOperator ) {
    this.comparisonOperator = comparisonOperator;
  }
}
