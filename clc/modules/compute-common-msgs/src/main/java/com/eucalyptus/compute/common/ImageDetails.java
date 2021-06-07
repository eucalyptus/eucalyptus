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

import java.util.ArrayList;
import java.util.Date;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


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
    return new CompatFunction<ImageDetails, String>( ) {
      @Override
      public String apply( final ImageDetails imageDetails ) {
        return imageDetails.getImageId( );
      }
    };
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
