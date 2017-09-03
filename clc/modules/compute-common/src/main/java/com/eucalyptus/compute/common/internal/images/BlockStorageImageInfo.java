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

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

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
  
  @EntityUpgrade( entities = { BlockStorageImageInfo.class }, since = Version.v3_3_0, value = Eucalyptus.class )
  public enum BlockStorageImageInfo330Upgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( BlockStorageImageInfo.BlockStorageImageInfo330Upgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      EntityTransaction db = Entities.get( BlockStorageImageInfo.class );
      try {
        List<BlockStorageImageInfo> images = Entities.query( new BlockStorageImageInfo( ) );
        for ( BlockStorageImageInfo image : images ) {
          LOG.info("Upgrading BlockStorageImageInfo: " + image.toString());
          if (StringUtils.isBlank(image.getRootDeviceName())) {
        	LOG.info("Setting the root device name to " + Images.DEFAULT_ROOT_DEVICE);
            image.setRootDeviceName(Images.DEFAULT_ROOT_DEVICE);
          }
          DeviceMapping mapping = null;
          if ( image.getDeviceMappings().size() == 1 && (mapping = image.getDeviceMappings().iterator().next()) != null 
        		  && mapping instanceof BlockStorageDeviceMapping ) {
            LOG.info("Setting the device mapping name to " + Images.DEFAULT_ROOT_DEVICE);
            mapping.setDeviceName(Images.DEFAULT_ROOT_DEVICE);
            LOG.info("Adding ephemeral disk at " + Images.DEFAULT_EPHEMERAL_DEVICE);
        	image.getDeviceMappings().add(new EphemeralDeviceMapping( image, Images.DEFAULT_EPHEMERAL_DEVICE, "ephemeral0" ));
          } else {
        	LOG.error("Expected to see only the root block device mapping but encountered " + image.getDeviceMappings().size() + " device mappings.");
          }
          Entities.persist(image);
        }
        db.commit( );
        return true;
      } catch ( Exception ex ) {
    	LOG.error("Error upgrading BlockStorageImageInfo: ", ex);
    	db.rollback();
        throw Exceptions.toUndeclared( ex );
      }
    }
  }

  @EntityUpgrade( entities = { BlockStorageImageInfo.class }, since = Version.v3_4_0, value = Eucalyptus.class )
  public enum BlockStorageImageInfo340Upgrade implements Predicate<Class> {
	  INSTANCE;
	  private static Logger LOG = Logger.getLogger( BlockStorageImageInfo.BlockStorageImageInfo340Upgrade.class );

	  @Override
	  public boolean apply(@Nullable Class arg0) {
		  // TODO Auto-generated method stub
		  EntityTransaction db = Entities.get( BlockStorageImageInfo.class );
		  try {
			  List<BlockStorageImageInfo> images = Entities.query( new BlockStorageImageInfo( ) );
			  for ( BlockStorageImageInfo image : images ) {
				  LOG.info("Upgrading BlockStorageImageInfo: " + image.toString());
				  if(image.virtType == null){
					  image.virtType = ImageMetadata.VirtualizationType.hvm;
					  Entities.persist(image);
				  }
			  }
			  db.commit( );
			  return true;
		  } catch ( Exception ex ) {
			  LOG.error("Error upgrading BlockStorageImageInfo: ", ex);
			  db.rollback();
			  throw Exceptions.toUndeclared( ex );
		  } finally{
			  if(db.isActive())
				  db.rollback();
		  }
	  } 
  }
}
