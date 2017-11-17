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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSCloudWatchAlarmProperties implements ResourceProperties {

  @Property
  private Boolean actionsEnabled;

  @Property
  private ArrayList<String> alarmActions = Lists.newArrayList( );

  @Property
  private String alarmDescription;

  @Property
  private String alarmName;

  @Required
  @Property
  private String comparisonOperator;

  @Property
  private ArrayList<CloudWatchMetricDimension> dimensions = Lists.newArrayList( );

  @Required
  @Property
  private Integer evaluationPeriods;

  @Property
  private ArrayList<String> insufficientDataActions = Lists.newArrayList( );

  @Required
  @Property
  private String metricName;

  @Required
  @Property
  private String namespace;

  @Property( name = "OKActions" )
  private ArrayList<String> okActions = Lists.newArrayList( );

  @Required
  @Property
  private Integer period;

  @Required
  @Property
  private String statistic;

  @Required
  @Property
  private Double threshold;

  @Property
  private String unit;

  public Boolean getActionsEnabled( ) {
    return actionsEnabled;
  }

  public void setActionsEnabled( Boolean actionsEnabled ) {
    this.actionsEnabled = actionsEnabled;
  }

  public ArrayList<String> getAlarmActions( ) {
    return alarmActions;
  }

  public void setAlarmActions( ArrayList<String> alarmActions ) {
    this.alarmActions = alarmActions;
  }

  public String getAlarmDescription( ) {
    return alarmDescription;
  }

  public void setAlarmDescription( String alarmDescription ) {
    this.alarmDescription = alarmDescription;
  }

  public String getAlarmName( ) {
    return alarmName;
  }

  public void setAlarmName( String alarmName ) {
    this.alarmName = alarmName;
  }

  public String getComparisonOperator( ) {
    return comparisonOperator;
  }

  public void setComparisonOperator( String comparisonOperator ) {
    this.comparisonOperator = comparisonOperator;
  }

  public ArrayList<CloudWatchMetricDimension> getDimensions( ) {
    return dimensions;
  }

  public void setDimensions( ArrayList<CloudWatchMetricDimension> dimensions ) {
    this.dimensions = dimensions;
  }

  public Integer getEvaluationPeriods( ) {
    return evaluationPeriods;
  }

  public void setEvaluationPeriods( Integer evaluationPeriods ) {
    this.evaluationPeriods = evaluationPeriods;
  }

  public ArrayList<String> getInsufficientDataActions( ) {
    return insufficientDataActions;
  }

  public void setInsufficientDataActions( ArrayList<String> insufficientDataActions ) {
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

  public ArrayList<String> getOkActions( ) {
    return okActions;
  }

  public void setOkActions( ArrayList<String> okActions ) {
    this.okActions = okActions;
  }

  public Integer getPeriod( ) {
    return period;
  }

  public void setPeriod( Integer period ) {
    this.period = period;
  }

  public String getStatistic( ) {
    return statistic;
  }

  public void setStatistic( String statistic ) {
    this.statistic = statistic;
  }

  public Double getThreshold( ) {
    return threshold;
  }

  public void setThreshold( Double threshold ) {
    this.threshold = threshold;
  }

  public String getUnit( ) {
    return unit;
  }

  public void setUnit( String unit ) {
    this.unit = unit;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "actionsEnabled", actionsEnabled )
        .add( "alarmActions", alarmActions )
        .add( "alarmDescription", alarmDescription )
        .add( "alarmName", alarmName )
        .add( "comparisonOperator", comparisonOperator )
        .add( "dimensions", dimensions )
        .add( "evaluationPeriods", evaluationPeriods )
        .add( "insufficientDataActions", insufficientDataActions )
        .add( "metricName", metricName )
        .add( "namespace", namespace )
        .add( "okActions", okActions )
        .add( "period", period )
        .add( "statistic", statistic )
        .add( "threshold", threshold )
        .add( "unit", unit )
        .toString( );
  }
}
