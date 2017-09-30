/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
