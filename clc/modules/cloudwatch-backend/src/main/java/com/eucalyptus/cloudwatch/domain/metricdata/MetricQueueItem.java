/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloudwatch.domain.metricdata;


import com.eucalyptus.cloudwatch.common.backend.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;

public class MetricQueueItem {
  private String accountId;
  private MetricDatum metricDatum;
  private String namespace;
  private MetricType metricType;
  public String getAccountId() {
    return accountId;
  }
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
  public MetricDatum getMetricDatum() {
    return metricDatum;
  }
  public void setMetricDatum(MetricDatum metricDatum) {
    this.metricDatum = metricDatum;
  }
  public String getNamespace() {
    return namespace;
  }
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
  public MetricType getMetricType() {
    return metricType;
  }
  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }
}
