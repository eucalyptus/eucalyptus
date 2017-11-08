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
