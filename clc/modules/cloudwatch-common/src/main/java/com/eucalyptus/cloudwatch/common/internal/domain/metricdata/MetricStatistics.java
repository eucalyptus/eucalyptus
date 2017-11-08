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

import java.util.Collection;
import java.util.Date;

import com.eucalyptus.cloudwatch.common.internal.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;

public class MetricStatistics {

  private String accountId;
  private String namespace;
  private String metricName;
  private Units units;
  private MetricType metricType;
  private Date timestamp;
  private Double sampleSize;
  private Double sampleMax;
  private Double sampleMin;
  private Double sampleSum;
  private Collection<DimensionEntity> dimensions;

  
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


  public Collection<DimensionEntity> getDimensions() {
    return dimensions;
  }


  public void setDimensions(Collection<DimensionEntity> dimensions) {
    this.dimensions = dimensions;
  }

  public MetricStatistics(MetricEntity me, Date startTime, Integer period, Collection<DimensionEntity> dimensions) {
    this.accountId = me.getAccountId();
    this.namespace = me.getNamespace();
    this.metricName = me.getMetricName();
    this.units = me.getUnits();
    this.metricType = me.getMetricType();
    this.timestamp = MetricUtils.getPeriodStart(me.getTimestamp(), startTime, period);
    this.sampleSize = me.getSampleSize();
    this.sampleMax = me.getSampleMax();
    this.sampleMin = me.getSampleMin();
    this.sampleSum = me.getSampleSum();
    this.dimensions = dimensions;
  }

}
