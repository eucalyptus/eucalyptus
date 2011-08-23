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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cluster;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.BootableImageInfo;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.KernelImageInfo;
import com.eucalyptus.images.RamdiskImageInfo;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@Embeddable
public class VmBootRecord {
  @Parent
  private VmInstance              vmInstance;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private ImageInfo               machineImage;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private KernelImageInfo         kernel;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private RamdiskImageInfo        ramdisk;
  @Column( name = "metadata_vm_platform" )
  private String                  platform;
  @ElementCollection
  private Set<VmVolumeAttachment> persistentVolumes = Sets.newHashSet( );
  @Lob
  @Column( name = "metadata_vm_user_data" )
  private byte[]                  userData;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private SshKeyPair              sshKeyPair;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private VmType                  vmType;
  
  VmBootRecord( ) {
    super( );
  }
  
  VmBootRecord( BootableSet bootSet, byte[] userData, SshKeyPair sshKeyPair, VmType vmType ) {
    super( );
    this.machineImage = ( ImageInfo ) bootSet.getMachine( );
    this.kernel = bootSet.getKernel( );
    this.ramdisk = bootSet.getRamdisk( );
    this.platform = bootSet.getMachine( ).getPlatform( ).name( );
    this.userData = userData;
    this.sshKeyPair = KeyPairs.noKey( ).equals( sshKeyPair ) || ( sshKeyPair == null )
      ? null
      : sshKeyPair;
    this.vmType = vmType;
  }
  
  private VmInstance getVmInstance( ) {
    return this.vmInstance;
  }
  
  public BootableImageInfo getMachine( ) {
    return ( BootableImageInfo ) this.machineImage;
  }
  
  public KernelImageInfo getKernel( ) {
    return this.kernel;
  }
  
  public RamdiskImageInfo getRamdisk( ) {
    return this.ramdisk;
  }
  
  public String getPlatform( ) {
    return this.platform;
  }
  
  Set<VmVolumeAttachment> getPersistentVolumes( ) {
    return this.persistentVolumes;
  }
  
  byte[] getUserData( ) {
    return this.userData;
  }
  
  SshKeyPair getSshKeyPair( ) {
    return this.sshKeyPair;
  }
  
  VmType getVmType( ) {
    return this.vmType;
  }
  
  void setPlatform( String platform ) {
    this.platform = platform;
  }
  
  public boolean isLinux( ) {
    return ImageMetadata.Platform.linux.equals( this.getMachine( ).getPlatform( ) ) || this.getMachine( ).getPlatform( ) == null;
  }
  
  public VmTypeInfo populateVirtualBootRecord( VmType vmType ) throws MetadataException {
    VmTypeInfo vmTypeInfo = VmTypes.asVmTypeInfo( vmType, this.getMachine( ) );
    if ( this.isLinux( ) ) {
      if ( this.getKernel( ) != null ) {
        vmTypeInfo.setKernel( this.getKernel( ).getDisplayName( ), this.getKernel( ).getManifestLocation( ) );
      }
      if ( this.getRamdisk( ) != null ) {
        vmTypeInfo.setRamdisk( this.getRamdisk( ).getDisplayName( ), this.getRamdisk( ).getManifestLocation( ) );
      }
    }
    return vmTypeInfo;
  }
  
  private ImageInfo getMachineImage( ) {
    return this.machineImage;
  }
  
  private void setMachineImage( ImageInfo machineImage ) {
    this.machineImage = machineImage;
  }
  
  private void setVmInstance( VmInstance vmInstance ) {
    this.vmInstance = vmInstance;
  }
  
  private void setKernel( KernelImageInfo kernel ) {
    this.kernel = kernel;
  }
  
  private void setRamdisk( RamdiskImageInfo ramdisk ) {
    this.ramdisk = ramdisk;
  }
  
  private void setPersistentVolumes( Set<VmVolumeAttachment> persistentVolumes ) {
    this.persistentVolumes = persistentVolumes;
  }
  
  private void setUserData( byte[] userData ) {
    this.userData = userData;
  }
  
  private void setSshKeyPair( SshKeyPair sshKeyPair ) {
    this.sshKeyPair = sshKeyPair;
  }
  
  private void setVmType( VmType vmType ) {
    this.vmType = vmType;
  }
  
  private boolean isBlockStorage( ) {
    return this.getMachine( ) instanceof BlockStorageImageInfo;
  }
}
