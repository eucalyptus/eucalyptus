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

import java.util.Date;

public class GetMetricStatisticsType extends CloudWatchMessage {

  private String namespace;
  private String metricName;
  private Dimensions dimensions;
  private Date startTime;
  private Date endTime;
  private Integer period;
  private Statistics statistics;
  private String unit;

  public String getNamespace( ) {
    return namespace;
  }

  public void setNamespace( String namespace ) {
    this.namespace = namespace;
  }

  public String getMetricName( ) {
    return metricName;
  }

  public void setMetricName( String metricName ) {
    this.metricName = metricName;
  }

  public Dimensions getDimensions( ) {
    return dimensions;
  }

  public void setDimensions( Dimensions dimensions ) {
    this.dimensions = dimensions;
  }

  public Date getStartTime( ) {
    return startTime;
  }

  public void setStartTime( Date startTime ) {
    this.startTime = startTime;
  }

  public Date getEndTime( ) {
    return endTime;
  }

  public void setEndTime( Date endTime ) {
    this.endTime = endTime;
  }

  public Integer getPeriod( ) {
    return period;
  }

  public void setPeriod( Integer period ) {
    this.period = period;
  }

  public Statistics getStatistics( ) {
    return statistics;
  }

  public void setStatistics( Statistics statistics ) {
    this.statistics = statistics;
  }

  public String getUnit( ) {
    return unit;
  }

  public void setUnit( String unit ) {
    this.unit = unit;
  }
}
