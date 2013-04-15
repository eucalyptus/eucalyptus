package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Collection;
import java.util.Date;

import com.eucalyptus.cloudwatch.domain.DimensionEntity;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;

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

  public MetricStatistics(MetricEntity me, Date startTime, Integer period) {
    this.accountId = me.getAccountId();
    this.namespace = me.getNamespace();
    this.metricName = me.getMetricName();
    this.units = me.getUnits();
    this.metricType = me.getMetricType();
    this.timestamp = MetricManager.getPeriodStart(me.getTimestamp(), startTime, period);
    this.sampleSize = me.getSampleSize();
    this.sampleMax = me.getSampleMax();
    this.sampleMin = me.getSampleMin();
    this.sampleSum = me.getSampleSum();
    this.dimensions = me.getDimensions();
  }

}
