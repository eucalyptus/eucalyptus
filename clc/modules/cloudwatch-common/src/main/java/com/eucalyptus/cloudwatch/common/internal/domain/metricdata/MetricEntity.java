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

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.eucalyptus.cloudwatch.common.internal.domain.AbstractPersistentWithDimensions;
import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
public abstract class MetricEntity {

  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Column(name = "account_id", nullable = false)
  private String accountId;
  @Column(name = "namespace", nullable = false)
  private String namespace;
  @Column(name = "metric_name", nullable = false)
  private String metricName;
  @Column(name = "dimension_hash", nullable = false)
  private String dimensionHash;
  @Column(name = "units", nullable = false)
  @Enumerated(EnumType.STRING)
  private Units units;
  @Column(name = "metric_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MetricType metricType;
  @Column(name = "timestamp", nullable = false)
  private Date timestamp;
  @Column(name = "sample_size", nullable = false)
  private Double sampleSize;
  @Column(name = "sample_max", nullable = false)
  private Double sampleMax;
  @Column(name = "sample_min", nullable = false)
  private Double sampleMin;
  @Column(name = "sample_sum", nullable = false)
  private Double sampleSum;

  public enum MetricType {
    Custom, System
  }

  public String getAccountId() {
    return accountId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getDimensionHash() {
    return dimensionHash;
  }

  public void setDimensionHash(String dimensionHash) {
    this.dimensionHash = dimensionHash;
  }

  public Units getUnits() {
    return units;
  }

  public void setUnits(Units units) {
    this.units = units;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = MetricUtils.stripSeconds(timestamp);
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
    return "MetricEntity [accountId=" + accountId 
        + ", namespace=" + namespace + ", metricName=" + metricName
        + ", dimensionHash=" + dimensionHash + ", units=" + units
        + ", metricType=" + metricType + ", timestamp=" + timestamp
        + ", sampleSize=" + sampleSize + ", sampleMax=" + sampleMax
        + ", sampleMin=" + sampleMin + ", sampleSum=" + sampleSum + "]";
  }
}
