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
package com.eucalyptus.cloudwatch.common.internal.domain.metricdata;

import java.util.Date;

public class MetricUtils {

  public static Double average(Double sum, Double size) {
    if (Math.abs(size) < 0.0001) return 0.0; // TODO: make sure size is really an int
    return sum/size;
  }

  public static Date stripSeconds(Date timestamp) {
    if (timestamp == null)
      return timestamp;
    long time = timestamp.getTime();
    time = time - time % 60000L;
    return new Date(time);
  }

  public static Date getPeriodStart(Date originalTimestamp, Date startTime, Integer period) {
    long difference = originalTimestamp.getTime() - startTime.getTime();
    long remainderInOnePeriod = difference % (1000L * period);
    return new Date(originalTimestamp.getTime() - remainderInOnePeriod);
  }
}
