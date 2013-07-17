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
package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Date;
import java.util.Map;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.google.common.collect.Maps;

public class SimpleMetricEntity {
  private String accountId;
  private String metricName;
  private String namespace;
  private Map<String, String> dimensionMap;
  private MetricType metricType;
  private Units units;
  private Date timestamp;
  private Double sampleSize;
  private Double sampleMax;
  private Double sampleMin;
  private Double sampleSum;

  public SimpleMetricEntity(SimpleMetricEntity other) {
    this.accountId = other.accountId;
    this.metricName = other.metricName;
    this.namespace = other.namespace;
    this.dimensionMap = (other.dimensionMap == null ? null : Maps.newHashMap(other.dimensionMap));
    this.metricType = other.metricType;
    this.units = other.units;
    this.timestamp = other.timestamp;
    this.sampleSize = other.sampleSize;
    this.sampleMax = other.sampleMax;
    this.sampleMin = other.sampleMin;
    this.sampleSum = other.sampleSum;
  }
  public SimpleMetricEntity() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Map<String, String> getDimensionMap() {
    return dimensionMap;
  }

  public void setDimensionMap(Map<String, String> dimensionMap) {
    this.dimensionMap = dimensionMap;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public Units getUnits() {
    return units;
  }

  public void setUnits(Units units) {
    this.units = units;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Double getSampleSize() {
    return sampleSize;
  }

  public void setSampleSize(Double sampleSize) {
    this.sampleSize = sampleSize;
  }

  public Double getSampleMax() {
    return sampleMax;
  }

  public void setSampleMax(Double sampleMax) {
    this.sampleMax = sampleMax;
  }

  public Double getSampleMin() {
    return sampleMin;
  }

  public void setSampleMin(Double sampleMin) {
    this.sampleMin = sampleMin;
  }

  public Double getSampleSum() {
    return sampleSum;
  }

  public void setSampleSum(Double sampleSum) {
    this.sampleSum = sampleSum;
  }

  @Override
  public String toString() {
    return "SimpleMetricEntity [accountId=" + accountId
        + ", metricName=" + metricName + ", namespace=" + namespace
        + ", dimensionMap=" + dimensionMap + ", metricType=" + metricType
        + ", units=" + units + ", timestamp=" + timestamp + ", sampleSize="
        + sampleSize + ", sampleMax=" + sampleMax + ", sampleMin="
        + sampleMin + ", sampleSum=" + sampleSum + "]";
  }
}
