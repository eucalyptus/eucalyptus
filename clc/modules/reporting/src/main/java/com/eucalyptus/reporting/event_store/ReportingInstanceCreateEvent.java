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
package com.eucalyptus.reporting.event_store;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import com.eucalyptus.component.annotation.RemotablePersistence;

@Entity
@PersistenceContext(name = "eucalyptus_reporting_backend")
@RemotablePersistence
@Table(name = "reporting_instance_create_events")
public class ReportingInstanceCreateEvent extends ReportingEventSupport {
  private static final long serialVersionUID = 1L;

  @Column( name = "uuid", nullable = false )
  private String uuid;
  @Column( name = "instance_id", nullable = false )
  private String instanceId;
  @Column( name = "instance_type", nullable = false )
  private String instanceType;
  @Column( name = "user_id", nullable = false )
  private String userId;
  @Column( name = "availability_zone", nullable = false )
  private String availabilityZone;

  /**
   * <p/>
   * Do not instantiate this class directly; use the
   * ReportingInstanceEventStore class.
   */
  protected ReportingInstanceCreateEvent() {
  }

  /**
   * <p/>
   * Do not instantiate this class directly; use the
   * ReportingInstanceEventStore class.
   */
  ReportingInstanceCreateEvent( final String uuid,
                                final String instanceId,
                                final Long timestampMs,
                                final String instanceType,
                                final String userId,
                                final String availabilityZone ) {
    this.uuid = uuid;
    this.instanceId = instanceId;
    this.timestampMs = timestampMs;
    this.instanceType = instanceType;
    this.userId = userId;
    this.availabilityZone = availabilityZone;
  }

  @Override
  public EventDependency asDependency() {
    return asDependency( "uuid", uuid );
  }

  @Override
  public Set<EventDependency> getDependencies() {
    return withDependencies().user( userId ).set();
  }

  @Override
  public int hashCode() {
    return (uuid == null) ? 0 : uuid.hashCode();
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj )
      return true;
    if ( getClass() != obj.getClass() )
      return false;
    ReportingInstanceCreateEvent other = (ReportingInstanceCreateEvent) obj;
    if ( uuid == null ) {
      if ( other.uuid != null )
        return false;
    } else if ( !uuid.equals( other.uuid ) )
      return false;
    return true;
  }

  public String getUuid() {
    return uuid;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public String getUserId() {
    return userId;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  @Override
  public String toString() {
    return "ReportingInstanceCreateEvent [uuid=" + uuid + ", instanceId="
        + instanceId + ", instanceType=" + instanceType + ", userId="
        + userId + ", availabilityZone=" + availabilityZone + "]";
  }
}
