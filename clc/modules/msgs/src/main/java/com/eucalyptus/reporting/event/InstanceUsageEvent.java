/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
