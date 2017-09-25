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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VirtualMachineType extends EucalyptusData {

  private String name;
  private Integer memory;
  private Integer cores;
  private Integer disk;
  private ArrayList<VirtualBootRecordType> virtualBootRecord = new ArrayList<VirtualBootRecordType>( );

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Integer getMemory( ) {
    return memory;
  }

  public void setMemory( Integer memory ) {
    this.memory = memory;
  }

  public Integer getCores( ) {
    return cores;
  }

  public void setCores( Integer cores ) {
    this.cores = cores;
  }

  public Integer getDisk( ) {
    return disk;
  }

  public void setDisk( Integer disk ) {
    this.disk = disk;
  }

  public ArrayList<VirtualBootRecordType> getVirtualBootRecord( ) {
    return virtualBootRecord;
  }

  public void setVirtualBootRecord( ArrayList<VirtualBootRecordType> virtualBootRecord ) {
    this.virtualBootRecord = virtualBootRecord;
  }
}
