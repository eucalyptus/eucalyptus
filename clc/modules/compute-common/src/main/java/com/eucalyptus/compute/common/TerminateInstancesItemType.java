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
    result = ( (int) ( prime * result + ( ( this.instanceId == null ) ? 0 : this.instanceId.hashCode( ) ) ) );
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
