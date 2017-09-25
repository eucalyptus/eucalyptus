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

public class DescribeAlarmsForMetricType extends CloudWatchMessage {

  private String metricName;
  private String namespace;
  private String statistic;
  private Dimensions dimensions;
  private Integer period;
  private String unit;

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
}
