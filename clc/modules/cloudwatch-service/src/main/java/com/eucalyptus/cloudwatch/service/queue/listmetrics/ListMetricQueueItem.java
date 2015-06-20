/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
