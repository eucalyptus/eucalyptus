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
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;

public class CreateImageType extends VmImageMessage {

  private String instanceId;
  private String name;
  private String description;
  private Boolean noReboot;
  @HttpParameterMapping( parameter = "BlockDeviceMapping" )
  @HttpEmbedded( multiple = true )
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>( );

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getNoReboot( ) {
    return noReboot;
  }

  public void setNoReboot( Boolean noReboot ) {
    this.noReboot = noReboot;
  }

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( ArrayList<BlockDeviceMappingItemType> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }
}
