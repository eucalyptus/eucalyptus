package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Date;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;

public class GetMetricStatisticsAggregationKey {

  private String accountId;
  private String namespace;
  private String metricName;
  private Units units;
  private MetricType metricType;
  private Date timestamp;
  private String dimensionHash;

  public GetMetricStatisticsAggregationKey(MetricEntity me, Date startTime,
      Integer period) {
    this.accountId = me.getAccountId();
    this.namespace = me.getNamespace();
    this.metricName = me.getMetricName();
    this.units = me.getUnits();
    this.metricType = me.getMetricType();
    this.timestamp = MetricManager.getPeriodStart(me.getTimestamp(), startTime, period);
    this.dimensionHash = MetricManager.hash(me.getDimensions());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((accountId == null) ? 0 : accountId.hashCode());
    result = prime * result
        + ((dimensionHash == null) ? 0 : dimensionHash.hashCode());
    result = prime * result
        + ((metricName == null) ? 0 : metricName.hashCode());
    result = prime * result
        + ((metricType == null) ? 0 : metricType.hashCode());
    result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    result = prime * result + ((units == null) ? 0 : units.hashCode());
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
    GetMetricStatisticsAggregationKey other = (GetMetricStatisticsAggregationKey) obj;
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
    return true;
  }

  
}
