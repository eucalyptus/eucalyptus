/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch.common.internal.domain.listmetrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import com.eucalyptus.cloudwatch.common.internal.domain.AbstractPersistentWithDimensions;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;

@Entity
@PersistenceContext(name="eucalyptus_cloudwatch")
@Table(name="list_metrics")
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
