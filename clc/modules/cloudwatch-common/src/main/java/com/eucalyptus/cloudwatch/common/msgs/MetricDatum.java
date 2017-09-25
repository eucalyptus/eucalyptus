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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class MetricDatum extends EucalyptusData {

  private String metricName;
  private Dimensions dimensions;
  private Date timestamp;
  private Double value;
  private StatisticSet statisticValues;
  private String unit;

  @Override
  public String toString( ) {
    return "MetricDatum [metricName=" + metricName + ", dimensions=" + dimensions + ", timestamp=" + timestamp + ", value=" + value + ", statisticValues=" + statisticValues + ", unit=" + unit + "]";
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

  public Date getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( Date timestamp ) {
    this.timestamp = timestamp;
  }

  public Double getValue( ) {
    return value;
  }

  public void setValue( Double value ) {
    this.value = value;
  }

  public StatisticSet getStatisticValues( ) {
    return statisticValues;
  }

  public void setStatisticValues( StatisticSet statisticValues ) {
    this.statisticValues = statisticValues;
  }

  public String getUnit( ) {
    return unit;
  }

  public void setUnit( String unit ) {
    this.unit = unit;
  }
}
