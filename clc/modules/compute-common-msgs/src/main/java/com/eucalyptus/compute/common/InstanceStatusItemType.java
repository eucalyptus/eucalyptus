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

public class InstanceStatusItemType extends EucalyptusData {

  private String instanceId;
  private String availabilityZone;
  private InstanceStatusEventsSetType eventsSet;
  private InstanceStateType instanceState;
  private InstanceStatusType systemStatus;
  private InstanceStatusType instanceStatus;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public InstanceStatusEventsSetType getEventsSet( ) {
    return eventsSet;
  }

  public void setEventsSet( InstanceStatusEventsSetType eventsSet ) {
    this.eventsSet = eventsSet;
  }

  public InstanceStateType getInstanceState( ) {
    return instanceState;
  }

  public void setInstanceState( InstanceStateType instanceState ) {
    this.instanceState = instanceState;
  }

  public InstanceStatusType getSystemStatus( ) {
    return systemStatus;
  }

  public void setSystemStatus( InstanceStatusType systemStatus ) {
    this.systemStatus = systemStatus;
  }

  public InstanceStatusType getInstanceStatus( ) {
    return instanceStatus;
  }

  public void setInstanceStatus( InstanceStatusType instanceStatus ) {
    this.instanceStatus = instanceStatus;
  }
}
