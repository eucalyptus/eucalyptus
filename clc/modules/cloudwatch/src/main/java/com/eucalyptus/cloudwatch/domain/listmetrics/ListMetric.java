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
package com.eucalyptus.cloudwatch.domain.listmetrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.cloudwatch.domain.AbstractPersistentWithDimensions;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="list_metrics")
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ListMetric extends AbstractPersistentWithDimensions {

  @Override
  public String toString() {
    return "ListMetric [accountId=" + accountId + ", namespace=" + namespace
        + ", metricName=" + metricName + ", metricType=" + metricType + "]";
  }

  public static final int MAX_DIM_NUM = 10;
  private static final Logger LOG = Logger.getLogger(ListMetric.class);
  public ListMetric() {
    super();
  }
  
  public String getAccountId() {
    return accountId;
  }
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
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

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  @Column( name = "account_id" , nullable = false)
  private String accountId;
  @Column( name = "namespace" , nullable = false)
  private String namespace; 
  @Column( name = "metric_name" , nullable = false)
  private String metricName;
  @Column(name = "metric_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MetricType metricType;
}
