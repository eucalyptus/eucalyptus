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
