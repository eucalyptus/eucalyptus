/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class TerminateInstancesItemType extends EucalyptusData {

  private String instanceId;
  private String previousStateCode;
  private String previousStateName;
  private String shutdownStateCode;
  private String shutdownStateName;

  public TerminateInstancesItemType(
      final String instanceId,
      final Integer previousStateCode,
      final String previousStateName,
      final Integer shutdownStateCode,
      final String shutdownStateName
  ) {
    this.instanceId = instanceId;
    this.previousStateCode = String.valueOf( previousStateCode );
    this.previousStateName = previousStateName;
    this.shutdownStateCode = String.valueOf( shutdownStateCode );
    this.shutdownStateName = shutdownStateName;
  }

  public TerminateInstancesItemType( ) {
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.instanceId == null ) ? 0 : this.instanceId.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }

    if ( obj == null ) {
      return false;
    }

    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }

    TerminateInstancesItemType other = (TerminateInstancesItemType) obj;
    if ( this.instanceId == null ) {
      if ( other.instanceId != null ) {
        return false;
      }

    } else if ( !this.instanceId.equals( other.instanceId ) ) {
      return false;
    }

    return true;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getPreviousStateCode( ) {
    return previousStateCode;
  }

  public void setPreviousStateCode( String previousStateCode ) {
    this.previousStateCode = previousStateCode;
  }

  public String getPreviousStateName( ) {
    return previousStateName;
  }

  public void setPreviousStateName( String previousStateName ) {
    this.previousStateName = previousStateName;
  }

  public String getShutdownStateCode( ) {
    return shutdownStateCode;
  }

  public void setShutdownStateCode( String shutdownStateCode ) {
    this.shutdownStateCode = shutdownStateCode;
  }

  public String getShutdownStateName( ) {
    return shutdownStateName;
  }

  public void setShutdownStateName( String shutdownStateName ) {
    this.shutdownStateName = shutdownStateName;
  }
}
