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

import java.util.Comparator;
import java.util.Date;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;

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
    this.timestamp = MetricManager.getPeriodStart(me.getTimestamp(), startTime, period);
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
