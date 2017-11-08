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
package com.eucalyptus.cloudwatch.common.internal.domain.metricdata;

import java.util.Date;
import java.util.Map;

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
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
