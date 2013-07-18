/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudwatch.domain.absolute;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="absolute_metric_history")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AbsoluteMetricHistory extends AbstractPersistent {

  public AbsoluteMetricHistory() {
  }
  @Column(name = "namespace", nullable = false)
  private String namespace;
  @Column(name = "metric_name", nullable = false)
  private String metricName;
  @Column(name = "dimension_name", nullable = false)
  private String dimensionName;
  @Column(name = "dimension_value", nullable = false)
  private String dimensionValue;
  @Column(name = "timestamp", nullable = false)
  private Date timestamp;
  @Column(name = "last_metric_value", nullable = false)
  private Double lastMetricValue;
  public String getNamespace() {
    return namespace;
  }
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
  public String getMetricName() {
    return metricName;
  }
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }
  public String getDimensionName() {
    return dimensionName;
  }
  public void setDimensionName(String dimensionName) {
    this.dimensionName = dimensionName;
  }
  public String getDimensionValue() {
    return dimensionValue;
  }
  public void setDimensionValue(String dimensionValue) {
    this.dimensionValue = dimensionValue;
  }
  public Date getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
  public Double getLastMetricValue() {
    return lastMetricValue;
  }
  public void setLastMetricValue(Double lastMetricValue) {
    this.lastMetricValue = lastMetricValue;
  }
  

}