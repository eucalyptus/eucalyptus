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

import javax.annotation.Nonnull;
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;

public class ModifyInstanceTypeAttributeType extends VmTypeMessage {

  private Boolean reset = false;
  private Boolean force = false;
  @Nonnull
  private String name;
  @ComputeMessageValidation.FieldRange( min = 1l )
  private Integer cpu;
  @ComputeMessageValidation.FieldRange( min = 1l )
  private Integer disk;
  @ComputeMessageValidation.FieldRange( min = 1l )
  private Integer memory;
  @ComputeMessageValidation.FieldRange( min = 1l, max = 8l )
  private Integer networkInterfaces;

  public Boolean getReset( ) {
    return reset;
  }

  public void setReset( Boolean reset ) {
    this.reset = reset;
  }

  public Boolean getForce( ) {
    return force;
  }

  public void setForce( Boolean force ) {
    this.force = force;
  }

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
}
