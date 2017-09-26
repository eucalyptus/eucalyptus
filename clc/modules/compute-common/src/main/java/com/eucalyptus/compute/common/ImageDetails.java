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
import java.util.Date;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/** *******************************************************************************/
public class ImageDetails extends EucalyptusData {

  private String imageId;
  private String imageLocation;
  private String imageState;
  private String imageOwnerId;
  private String architecture;
  private String imageType;
  private String kernelId;
  private String ramdiskId;
  private String platform;
  private Boolean isPublic;
  private String imageOwnerAlias;
  private String rootDeviceType = "instance-store";
  private String rootDeviceName = "/dev/sda1";
  private String name;
  private String description;
  private String virtualizationType;
  private String hypervisor;
  private Date creationDate;
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMappings = new ArrayList<BlockDeviceMappingItemType>( );
  private ArrayList<String> productCodes = new ArrayList<String>( );
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  public static CompatFunction<ImageDetails, String> imageId( ) {
    return ImageDetails::getImageId;
  }

  public boolean equals( final Object o ) {
    if ( o == null || !( o instanceof ImageDetails ) ) return false;
    ImageDetails that = (ImageDetails) o;
    if ( !imageId.equals( that.imageId ) ) return false;
    return true;
  }

  public int hashCode( ) {
    return imageId.hashCode( );
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getImageLocation( ) {
    return imageLocation;
  }

  public void setImageLocation( String imageLocation ) {
    this.imageLocation = imageLocation;
  }

  public String getImageState( ) {
    return imageState;
  }

  public void setImageState( String imageState ) {
    this.imageState = imageState;
  }

  public String getImageOwnerId( ) {
    return imageOwnerId;
  }

  public void setImageOwnerId( String imageOwnerId ) {
    this.imageOwnerId = imageOwnerId;
  }

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public String getImageType( ) {
    return imageType;
  }

  public void setImageType( String imageType ) {
    this.imageType = imageType;
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

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public Boolean getIsPublic( ) {
    return isPublic;
  }

  public void setIsPublic( Boolean isPublic ) {
    this.isPublic = isPublic;
  }

  public String getImageOwnerAlias( ) {
    return imageOwnerAlias;
  }

  public void setImageOwnerAlias( String imageOwnerAlias ) {
    this.imageOwnerAlias = imageOwnerAlias;
  }

  public String getRootDeviceType( ) {
    return rootDeviceType;
  }

  public void setRootDeviceType( String rootDeviceType ) {
    this.rootDeviceType = rootDeviceType;
  }

  public String getRootDeviceName( ) {
    return rootDeviceName;
  }

  public void setRootDeviceName( String rootDeviceName ) {
    this.rootDeviceName = rootDeviceName;
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

  public String getVirtualizationType( ) {
    return virtualizationType;
  }

  public void setVirtualizationType( String virtualizationType ) {
    this.virtualizationType = virtualizationType;
  }

  public String getHypervisor( ) {
    return hypervisor;
  }

  public void setHypervisor( String hypervisor ) {
    this.hypervisor = hypervisor;
  }

  public Date getCreationDate( ) {
    return creationDate;
  }

  public void setCreationDate( Date creationDate ) {
    this.creationDate = creationDate;
  }

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( ArrayList<BlockDeviceMappingItemType> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public ArrayList<String> getProductCodes( ) {
    return productCodes;
  }

  public void setProductCodes( ArrayList<String> productCodes ) {
    this.productCodes = productCodes;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }
}
