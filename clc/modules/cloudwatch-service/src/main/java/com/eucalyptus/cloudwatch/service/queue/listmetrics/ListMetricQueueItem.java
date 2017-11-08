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
package com.eucalyptus.cloudwatch.service.queue.listmetrics;

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;

import java.util.Map;

public class ListMetricQueueItem {

  private String accountId;
  private String namespace;
  private String metricName;
  private MetricType metricType;
  private Map<String, String> dimensionMap;

  public ListMetricQueueItem() {
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

  public Map<String, String> getDimensionMap() {
    return dimensionMap;
  }

  public void setDimensionMap(Map<String, String> dimensionMap) {
    this.dimensionMap = dimensionMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListMetricQueueItem that = (ListMetricQueueItem) o;

    if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
    if (dimensionMap != null ? !dimensionMap.equals(that.dimensionMap) : that.dimensionMap != null) return false;
    if (metricName != null ? !metricName.equals(that.metricName) : that.metricName != null) return false;
    if (metricType != that.metricType) return false;
    if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = accountId != null ? accountId.hashCode() : 0;
    result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
    result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
    result = 31 * result + (metricType != null ? metricType.hashCode() : 0);
    result = 31 * result + (dimensionMap != null ? dimensionMap.hashCode() : 0);
    return result;
  }
}
