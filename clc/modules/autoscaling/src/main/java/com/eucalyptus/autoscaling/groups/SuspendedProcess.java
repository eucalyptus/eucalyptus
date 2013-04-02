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
package com.eucalyptus.autoscaling.groups;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 */
@Embeddable
public class SuspendedProcess {

  @Column( name = "metadata_scaling_process_type", nullable = false, updatable = false )
  @Enumerated( EnumType.STRING )
  private ScalingProcessType scalingProcessType;

  @Column( name = "metadata_reason", nullable = false, updatable = false )
  private String reason;

  @Temporal( TemporalType.TIMESTAMP)
  @Column( name = "creation_timestamp", nullable = false, updatable = false )
  private Date creationTimestamp;

  public ScalingProcessType getScalingProcessType() {
    return scalingProcessType;
  }

  public void setScalingProcessType( final ScalingProcessType scalingProcessType ) {
    this.scalingProcessType = scalingProcessType;
  }

  public String getReason() {
    return reason;
  }

  public void setReason( final String reason ) {
    this.reason = reason;
  }

  public Date getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp( final Date creationTimestamp ) {
    this.creationTimestamp = creationTimestamp;
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final SuspendedProcess that = (SuspendedProcess) o;

    if ( scalingProcessType != that.scalingProcessType ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return scalingProcessType != null ? scalingProcessType.hashCode() : 0;
  }

  public static SuspendedProcess createManual( final ScalingProcessType scalingProcessType ) {
    return buildSuspendedProcess( scalingProcessType, "Manual suspension" );
  }

  public static SuspendedProcess createAdministrative( final ScalingProcessType scalingProcessType ) {
    return buildSuspendedProcess( scalingProcessType, "Administrative suspension" );
  }

  private static SuspendedProcess buildSuspendedProcess( final ScalingProcessType scalingProcessType,
                                                         final String reason) {
    final SuspendedProcess suspendedProcess = new SuspendedProcess();
    suspendedProcess.setCreationTimestamp( new Date() );
    suspendedProcess.setScalingProcessType( scalingProcessType );
    suspendedProcess.setReason( reason );
    return suspendedProcess;
  }
}
