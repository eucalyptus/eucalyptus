/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.common.internal.images;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ImageMetadata;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@DiscriminatorValue( value = "blockstorage" )
public class BlockStorageImageInfo extends ImageInfo implements BootableImageInfo {
  @Column( name = "metadata_image_snapshot_id" )
  private String  snapshotId;
  @Column( name = "metadata_image_del_vol_on_terminate" )
  private Boolean deleteOnTerminate;
  @Column( name = "metadata_image_kernel_id" )
  private String  kernelId;
  @Column( name = "metadata_image_ramdisk_id" )
  private String  ramdiskId;
  @Column( name = "metadata_image_root_device" )
  private String rootDeviceName;
  @Column( name = "metadata_image_virtualization_type" )
  @Enumerated(  EnumType.STRING )
  private ImageMetadata.VirtualizationType virtType;
  
  BlockStorageImageInfo( ) {
    super( ImageMetadata.Type.machine );
  }
  
  BlockStorageImageInfo( String imageId ) {
    super( ImageMetadata.Type.machine , imageId );
  }
  
  public BlockStorageImageInfo( UserFullName userFullName, String imageId, String imageName, String imageDescription,
                                Long imageSizeBytes, ImageMetadata.Architecture arch, ImageMetadata.Platform platform,
                                String kernelId, String ramdiskId,
                                String snapshotId, Boolean deleteOnTerminate, String rootDeviceName,
                                boolean imagePublic ) {
    super( userFullName, imageId, ImageMetadata.Type.machine, imageName, imageDescription, imageSizeBytes, arch, platform, imagePublic );
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.snapshotId = snapshotId;
    this.deleteOnTerminate = deleteOnTerminate;
    this.rootDeviceName = rootDeviceName;
    this.virtType = ImageMetadata.VirtualizationType.hvm ;
  }
  
  public String getSnapshotId( ) {
    return this.snapshotId;
  }
  
  public void setSnapshotId( String snapshotId ) {
    this.snapshotId = snapshotId;
  }
  
  public Boolean getDeleteOnTerminate( ) {
    return this.deleteOnTerminate;
  }
  
  public void setDeleteOnTerminate( Boolean deleteOnTerminate ) {
    this.deleteOnTerminate = deleteOnTerminate;
  }
  
  @Override
  public String getKernelId( ) {
    return this.kernelId;
  }
  
  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }
  
  @Override
  public String getRamdiskId( ) {
    return this.ramdiskId;
  }
  
  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }
  
  @Override
  public String getRootDeviceName( ) {
    return this.rootDeviceName;
  }

  @Override
  public String getRootDeviceType( ) {
    return "ebs";
  }
  
  @Override
  public ImageMetadata.VirtualizationType getVirtualizationType(){
	  return this.virtType;
  }
  
  public void setRootDeviceName(String rootDeviceName) {
	this.rootDeviceName = rootDeviceName;
  }

}
