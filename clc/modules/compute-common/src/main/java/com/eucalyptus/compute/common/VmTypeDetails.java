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

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VmTypeDetails extends EucalyptusData {

  private String name;
  private Integer cpu;
  private Integer disk;
  private Integer memory;
  private Integer networkInterfaces;
  private ArrayList<VmTypeZoneStatus> availability = new ArrayList<VmTypeZoneStatus>( );
  private ArrayList<VmTypeEphemeralDisk> ephemeralDisk = new ArrayList<VmTypeEphemeralDisk>( );

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Integer getCpu( ) {
    return cpu;
  }

  public void setCpu( Integer cpu ) {
    this.cpu = cpu;
  }

  public Integer getDisk( ) {
    return disk;
  }

  public void setDisk( Integer disk ) {
    this.disk = disk;
  }

  public Integer getMemory( ) {
    return memory;
  }

  public void setMemory( Integer memory ) {
    this.memory = memory;
  }

  public Integer getNetworkInterfaces( ) {
    return networkInterfaces;
  }

  public void setNetworkInterfaces( Integer networkInterfaces ) {
    this.networkInterfaces = networkInterfaces;
  }

  public ArrayList<VmTypeZoneStatus> getAvailability( ) {
    return availability;
  }

  public void setAvailability( ArrayList<VmTypeZoneStatus> availability ) {
    this.availability = availability;
  }

  public ArrayList<VmTypeEphemeralDisk> getEphemeralDisk( ) {
    return ephemeralDisk;
  }

  public void setEphemeralDisk( ArrayList<VmTypeEphemeralDisk> ephemeralDisk ) {
    this.ephemeralDisk = ephemeralDisk;
  }
}
