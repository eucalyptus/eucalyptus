package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Date;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;


public class PutMetricDataAggregationKey {

  private String accountId;
  private String dimensionHash;
  private String metricName;
  private MetricType metricType;
  private String namespace;
  private Date timestamp;
  private Units units;
  private String userId;

  public PutMetricDataAggregationKey(MetricQueueItem item) {
    this.accountId = item.getAccountId();
    this.dimensionHash = MetricManager.hash(item.getDimensionMap());
    this.metricName = item.getMetricName();
    this.metricType = item.getMetricType();
    this.namespace = item.getNamespace();
    this.timestamp = item.getTimestamp();
    this.units = item.getUnits();
    this.userId = item.getUserId();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((accountId == null) ? 0 : accountId.hashCode());
    result = prime * result
        + ((dimensionHash == null) ? 0 : dimensionHash.hashCode());
    result = prime * result
        + ((metricName == null) ? 0 : metricName.hashCode());
    result = prime * result
        + ((metricType == null) ? 0 : metricType.hashCode());
    result = prime * result
        + ((namespace == null) ? 0 : namespace.hashCode());
    result = prime * result
        + ((timestamp == null) ? 0 : timestamp.hashCode());
    result = prime * result + ((units == null) ? 0 : units.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PutMetricDataAggregationKey other = (PutMetricDataAggregationKey) obj;
    if (accountId == null) {
      if (other.accountId != null)
        return false;
    } else if (!accountId.equals(other.accountId))
      return false;
    if (dimensionHash == null) {
      if (other.dimensionHash != null)
        return false;
    } else if (!dimensionHash.equals(other.dimensionHash))
      return false;
    if (metricName == null) {
      if (other.metricName != null)
        return false;
    } else if (!metricName.equals(other.metricName))
      return false;
    if (metricType != other.metricType)
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    if (timestamp == null) {
      if (other.timestamp != null)
        return false;
    } else if (!timestamp.equals(other.timestamp))
      return false;
    if (units != other.units)
      return false;
    if (userId == null) {
      if (other.userId != null)
        return false;
    } else if (!userId.equals(other.userId))
      return false;
    return true;
  }
  
}

