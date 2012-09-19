/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.reporting.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

import com.eucalyptus.event.Event;

public class InstanceUsageEvent implements Event {
  private static final long serialVersionUID = 1L;

  private final String uuid;
  private final long timestamp;
  private final String resourceName;
  private final String metric;
  private final int sequenceNum;
  private final String dimension;
  private final Double value;
  private final long valueTimestamp;

  public InstanceUsageEvent( final String uuid,
                             final long timestamp,
                             final String resourceName,
                             final String metric,
                             final int sequenceNum,
                             final String dimension,
                             final Double value,
                             final long valueTimestamp ) {

    assertThat( uuid, not( isEmptyOrNullString() ) );
    assertThat( timestamp, notNullValue() );
    assertThat( resourceName, not(isEmptyOrNullString()) );
    assertThat( metric, not(isEmptyOrNullString()) );
    assertThat( sequenceNum, notNullValue() );
    assertThat( dimension, not(isEmptyOrNullString()) );
    assertThat( value, notNullValue() );
    assertThat( valueTimestamp, notNullValue() );
    this.uuid = uuid;
    this.timestamp = timestamp;
    this.resourceName = resourceName;
    this.metric = metric;
    this.sequenceNum = sequenceNum;
    this.dimension = dimension;
    this.value = value;
    this.valueTimestamp = valueTimestamp;
  }

  public String getUuid() {
    return uuid;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getMetric() {
    return metric;
  }

  public int getSequenceNum() {
    return sequenceNum;
  }

  public String getDimension() {
    return dimension;
  }

  public Double getValue() {
    return value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getValueTimestamp() {
    return valueTimestamp;
  }

  @Override
  public String toString() {
    return "InstanceUsageEvent [uuid=" + uuid + ", timestamp=" + timestamp
        + ", resourceName=" + resourceName + ", metric=" + metric
        + ", sequenceNum=" + sequenceNum + ", dimension=" + dimension
        + ", value=" + value + ", valueTimestamp=" + valueTimestamp
        + "]";
  }

}
