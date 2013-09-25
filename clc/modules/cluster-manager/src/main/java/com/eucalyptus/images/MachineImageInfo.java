/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.images;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.upgrade.Upgrades.Version;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "machine" )
public class MachineImageInfo extends PutGetImageInfo implements BootableImageInfo {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Column( name = "metadata_image_kernel_id" )
  private String kernelId;
  @Column( name = "metadata_image_ramdisk_id" )
  private String ramdiskId;
  @Column( name = "metadata_image_virtualization_type" )
  @Enumerated(  EnumType.STRING )
  private ImageMetadata.VirtualizationType virtType;
  
  public MachineImageInfo( ) {
    super( ImageMetadata.Type.machine );
  }
  
  public MachineImageInfo( final String imageId ) {
    super( ImageMetadata.Type.machine, imageId );
  }
  
  public MachineImageInfo( final UserFullName userFullName, final String imageId,
                           final String imageName, final String imageDescription, final Long imageSizeBytes, final Architecture arch, final Platform platform,
                           final String imageLocation, final Long imageBundleSizeBytes, final String imageChecksum, final String imageChecksumType,
                           final String kernelId, final String ramdiskId, ImageMetadata.VirtualizationType virtType ) {
    super( userFullName, imageId, ImageMetadata.Type.machine, imageName, imageDescription, imageSizeBytes, arch, platform, imageLocation, imageBundleSizeBytes,
           imageChecksum, imageChecksumType );
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.virtType = virtType;
  }
  
  @Override
  public String getKernelId( ) {
    return this.kernelId;
  }
  
  public void setKernelId( final String kernelId ) {
    this.kernelId = kernelId;
  }
  
  @Override
  public String getRamdiskId( ) {
    return this.ramdiskId;
  }
  
  public void setRamdiskId( final String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }
  
  @Override
  public boolean hasKernel( ) {
    return this.getKernelId( ) != null;
  }
  
  @Override
  public boolean hasRamdisk( ) {
    return this.getRamdiskId( ) != null;
  }
  
  @Override
  public String getManifestLocation( ) {
    return super.getManifestLocation( );
  }

  @Override
  public String getRootDeviceName( ) {
    return "/dev/sda1";
  }

  @Override
  public String getRootDeviceType( ) {
    return "instance-store";
  }

  @Override
  public ImageMetadata.VirtualizationType getVirtualizationType(){
	  return this.virtType;
  }
  

  @EntityUpgrade( entities = { MachineImageInfo.class }, since = Version.v3_4_0, value = Eucalyptus.class )
  public enum MachineImageInfo340Upgrade implements Predicate<Class> {
	  INSTANCE;
	  private static Logger LOG = Logger.getLogger( MachineImageInfo.MachineImageInfo340Upgrade.class );

	  @Override
	  public boolean apply(@Nullable Class arg0) {
		  // TODO Auto-generated method stub
		  EntityTransaction db = Entities.get( MachineImageInfo.class );
		  try {
			  List<MachineImageInfo> images = Entities.query( new MachineImageInfo( ) );
			  for ( MachineImageInfo image : images ) {
				  LOG.info("Upgrading MachineImageInfo: " + image.toString());
				  // all machine images prior 3.4.0 are paravirtualized type
				  if(image.virtType==null){
					  if(ImageMetadata.Platform.windows.equals(image.getPlatform()))
						  image.virtType = ImageMetadata.VirtualizationType.hvm;
					  else
						  image.virtType = ImageMetadata.VirtualizationType.paravirtualized;
					  Entities.persist(image);
				  }
			  }
			  db.commit( );
			  return true;
		  } catch ( Exception ex ) {
			  LOG.error("Error upgrading MachineImageInfo: ", ex);
			  db.rollback();
			  throw Exceptions.toUndeclared( ex );
		  } finally{
			  if(db.isActive())
				  db.rollback();
		  }
	  } 
  }
}
