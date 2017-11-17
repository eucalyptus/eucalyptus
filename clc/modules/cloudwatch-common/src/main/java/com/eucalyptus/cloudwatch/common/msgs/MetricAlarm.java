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
package com.eucalyptus.cloudwatch.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class MetricAlarm extends EucalyptusData {

  private String alarmName;
  private String alarmArn;
  private String alarmDescription;
  private Date alarmConfigurationUpdatedTimestamp;
  private Boolean actionsEnabled;
  private ResourceList okActions;
  private ResourceList alarmActions;
  private ResourceList insufficientDataActions;
  private String stateValue;
  private String stateReason;
  private String stateReasonData;
  private Date stateUpdatedTimestamp;
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

  public String getAlarmArn( ) {
    return alarmArn;
  }

  public void setAlarmArn( String alarmArn ) {
    this.alarmArn = alarmArn;
  }

  public String getAlarmDescription( ) {
    return alarmDescription;
  }

  public void setAlarmDescription( String alarmDescription ) {
    this.alarmDescription = alarmDescription;
  }

  public Date getAlarmConfigurationUpdatedTimestamp( ) {
    return alarmConfigurationUpdatedTimestamp;
  }

  public void setAlarmConfigurationUpdatedTimestamp( Date alarmConfigurationUpdatedTimestamp ) {
    this.alarmConfigurationUpdatedTimestamp = alarmConfigurationUpdatedTimestamp;
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

  public String getStateValue( ) {
    return stateValue;
  }

  public void setStateValue( String stateValue ) {
    this.stateValue = stateValue;
  }

  public String getStateReason( ) {
    return stateReason;
  }

  public void setStateReason( String stateReason ) {
    this.stateReason = stateReason;
  }

  public String getStateReasonData( ) {
    return stateReasonData;
  }

  public void setStateReasonData( String stateReasonData ) {
    this.stateReasonData = stateReasonData;
  }

  public Date getStateUpdatedTimestamp( ) {
    return stateUpdatedTimestamp;
  }

  public void setStateUpdatedTimestamp( Date stateUpdatedTimestamp ) {
    this.stateUpdatedTimestamp = stateUpdatedTimestamp;
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
