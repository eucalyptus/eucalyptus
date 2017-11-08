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
