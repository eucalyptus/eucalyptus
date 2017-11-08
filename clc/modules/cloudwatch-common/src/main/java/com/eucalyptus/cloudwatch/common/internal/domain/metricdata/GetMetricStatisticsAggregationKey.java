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

import java.util.Comparator;
import java.util.Date;

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;

public class GetMetricStatisticsAggregationKey {

  public static enum COMPARATOR_WITH_NULLS implements Comparator<GetMetricStatisticsAggregationKey> {
    INSTANCE {

      @Override
      public int compare(GetMetricStatisticsAggregationKey a, GetMetricStatisticsAggregationKey b) {
        if (a == b) return 0;
        if (a == null && b == null) return 0;
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        // otherwise do it by fields...
        if (compare(a.accountId, b.accountId) != 0) {
          return compare(a.accountId, b.accountId);
        }
        if (compare(a.timestamp, b.timestamp) != 0) {
          return compare(a.timestamp, b.timestamp);
        }
        if (compare(a.namespace, b.namespace) != 0) {
          return compare(a.namespace, b.namespace);
        }
        if (compare(a.metricType, b.metricType) != 0) {
          return compare(a.metricType, b.metricType);
        }
        if (compare(a.metricName, b.metricName) != 0) {
          return compare(a.metricName, b.metricName);
        }
        if (compare(a.dimensionHash, b.dimensionHash) != 0) {
          return compare(a.dimensionHash, b.dimensionHash);
        }
        return compare(a.units, b.units);
      
      }
      
      private int compare(Units a, Units b) {
        // "null" is considered "less"
        if (a == null && b == null) return 0;
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        return a.compareTo(b);
      }

      private int compare(Date a, Date b) {
        // "null" is considered "less"
        if (a == null && b == null) return 0;
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        return a.compareTo(b);
      }

      private int compare(MetricType a, MetricType b) {
        // "null" is considered "less"
        if (a == null && b == null) return 0;
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        return a.compareTo(b);
      }

      private int compare(String a, String b) {
        // "null" is considered "less"
        if (a == null && b == null) return 0;
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        return a.compareTo(b);
      }
    }
  }

  
  private String accountId;
  private String namespace;
  private String metricName;
  private Units units;
  private MetricType metricType;
  private Date timestamp;
  private String dimensionHash;

  public GetMetricStatisticsAggregationKey(MetricEntity me, Date startTime,
      Integer period, String dimensionHash) {
    this.accountId = me.getAccountId();
    this.namespace = me.getNamespace();
    this.metricName = me.getMetricName();
    this.units = me.getUnits();
    this.metricType = me.getMetricType();
    this.timestamp = MetricUtils.getPeriodStart(me.getTimestamp(), startTime, period);
    this.dimensionHash = dimensionHash;
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
