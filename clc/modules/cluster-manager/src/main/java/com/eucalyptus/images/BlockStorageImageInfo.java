/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.images;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.PersistenceContext;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.ImageMetadata;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
  
  BlockStorageImageInfo( ) {
    super( ImageMetadata.Type.machine );
  }
  
  BlockStorageImageInfo( String imageId ) {
    super( ImageMetadata.Type.machine , imageId );
  }
  
  BlockStorageImageInfo( UserFullName userFullName, String imageId, String imageName, String imageDescription, Long imageSizeBytes,
                         ImageMetadata.Architecture arch, ImageMetadata.Platform platform,
                         String kernelId, String ramdiskId,
                         String snapshotId, Boolean deleteOnTerminate ) {
    super( userFullName, imageId, ImageMetadata.Type.machine, imageName, imageDescription, imageSizeBytes, arch, platform );
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.snapshotId = snapshotId;
    this.deleteOnTerminate = deleteOnTerminate;
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
  public boolean hasKernel( ) {
    return this.getKernelId( ) != null;
  }
  
  @Override
  public boolean hasRamdisk( ) {
    return this.getRamdiskId( ) != null;
  }
  
}
