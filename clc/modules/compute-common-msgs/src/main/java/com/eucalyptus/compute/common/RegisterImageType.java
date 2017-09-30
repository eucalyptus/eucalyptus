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

public class RegisterImageType extends VmImageMessage {

  private String imageLocation;
  private String amiId;
  private String name;
  private String description;
  private String architecture;
  private String kernelId;
  private String ramdiskId;
  private String rootDeviceName;
  private String virtualizationType;
  private String platform;
  private String sriovNetSupport;
  @HttpParameterMapping( parameter = "BlockDeviceMapping" )
  @HttpEmbedded( multiple = true )
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>( );

  public String getImageLocation( ) {
    return imageLocation;
  }

  public void setImageLocation( String imageLocation ) {
    this.imageLocation = imageLocation;
  }

  public String getAmiId( ) {
    return amiId;
  }

  public void setAmiId( String amiId ) {
    this.amiId = amiId;
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

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public String getKernelId( ) {
    return kernelId;
  }

  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId( ) {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public String getRootDeviceName( ) {
    return rootDeviceName;
  }

  public void setRootDeviceName( String rootDeviceName ) {
    this.rootDeviceName = rootDeviceName;
  }

  public String getVirtualizationType( ) {
    return virtualizationType;
  }

  public void setVirtualizationType( String virtualizationType ) {
    this.virtualizationType = virtualizationType;
  }

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public String getSriovNetSupport( ) {
    return sriovNetSupport;
  }

  public void setSriovNetSupport( String sriovNetSupport ) {
    this.sriovNetSupport = sriovNetSupport;
  }

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( ArrayList<BlockDeviceMappingItemType> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }
}
