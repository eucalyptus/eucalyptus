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

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;

import com.eucalyptus.event.Event;

public class InstanceUsageEvent implements Event {
  private static final long serialVersionUID = 1L;

  private final String uuid;
  private final String instanceId;
  private final String metric;
  private final Long sequenceNum;
  private final String dimension;
  private final Double value;
  private final Long valueTimestamp;

  public InstanceUsageEvent( final String uuid,
                             final String instanceId,
                             final String metric,
                             final Long sequenceNum,
                             final String dimension,
                             final Double value,
                             final Long valueTimestamp ) {
    checkParam( uuid, not( isEmptyOrNullString() ) );
    checkParam( instanceId, not( isEmptyOrNullString() ) );
    checkParam( metric, not( isEmptyOrNullString() ) );
    checkParam( sequenceNum, notNullValue() );
    checkParam( dimension, not( isEmptyOrNullString() ) );
    checkParam( value, notNullValue() );
    checkParam( valueTimestamp, notNullValue() );
    this.uuid = uuid;
    this.instanceId = instanceId;
    this.metric = metric;
    this.sequenceNum = sequenceNum;
    this.dimension = dimension;
    this.value = value;
    this.valueTimestamp = valueTimestamp;
  }

  public String getUuid() {
    return uuid;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getMetric() {
    return metric;
  }

  public Long getSequenceNum() {
    return sequenceNum;
  }

  public String getDimension() {
    return dimension;
  }

  public Double getValue() {
    return value;
  }

  public Long getValueTimestamp() {
    return valueTimestamp;
  }

  @Override
  public String toString() {
    return "InstanceUsageEvent [uuid=" + uuid
        + ", instanceId=" + instanceId + ", metric=" + metric
        + ", sequenceNum=" + sequenceNum + ", dimension=" + dimension
        + ", value=" + value + ", valueTimestamp=" + valueTimestamp
        + "]";
  }

}
