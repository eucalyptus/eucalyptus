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
package com.eucalyptus.reporting.export;

import java.util.Date;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;

/**
 * Represents reported usage
 */
public class ReportedUsage {
  private String eventUuid;
  private Date created;
  private Date occurred;
  private String type;
  private String id;
  private String metric;
  private String dimension;
  private Long sequence;
  private Double value;

  public ReportedUsage() {
  }

  ReportedUsage( final ReportingEventSupport reportingEventSupport ) {
    setEventUuid( reportingEventSupport.getId() );
    setCreated( reportingEventSupport.getCreationTimestamp() );
    setOccurred( new Date( reportingEventSupport.getTimestampMs() ) );
  }

  public String getEventUuid() {
    return eventUuid;
  }

  public void setEventUuid( final String eventUuid ) {
    this.eventUuid = eventUuid;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated( final Date created ) {
    this.created = created;
  }

  public Date getOccurred() {
    return occurred;
  }

  public void setOccurred( final Date occurred ) {
    this.occurred = occurred;
  }

  public String getType() {
    return type;
  }

  public void setType( final String type ) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId( final String id ) {
    this.id = id;
  }

  public String getMetric() {
    return metric;
  }

  public void setMetric( final String metric ) {
    this.metric = metric;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension( final String dimension ) {
    this.dimension = dimension;
  }

  public Long getSequence() {
    return sequence;
  }

  public void setSequence( final Long sequence ) {
    this.sequence = sequence;
  }

  public Double getValue() {
    return value;
  }

  public void setValue( final Double value ) {
    this.value = value;
  }
}
