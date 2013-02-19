/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.vm;

import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.*;
import java.util.Arrays;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Type;

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
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
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
  @CollectionTable( name = "metadata_instances_persistent_volumes" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<VmVolumeAttachment> persistentVolumes = Sets.newHashSet( );
  
  @Column( name = "metadata_vm_user_data" )
  private byte[]                  userData;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "metadata_vm_sshkey" )
  private String                  sshKeyString;
  @ManyToOne
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private VmType                  vmType;
  
  VmBootRecord( ) {
    super( );
  }
  
  VmBootRecord( BootableSet bootSet, byte[] userData, SshKeyPair sshKeyPair, VmType vmType ) {
    checkParam( "Bootset must not be null", bootSet, notNullValue( ) );
    if ( bootSet.getMachine() instanceof ImageInfo ) {
      this.machineImage = ( ImageInfo ) bootSet.getMachine( );
    }
    if ( bootSet.hasKernel( ) ) {
      this.kernel = bootSet.getKernel( );
    }
    if ( bootSet.hasRamdisk( ) ) {
      this.ramdisk = bootSet.getRamdisk( );
    }
    this.platform = bootSet.getMachine( ).getPlatform( ).name( );
    this.userData = userData;
    this.sshKeyString = sshKeyPair.getPublicKey( );
    this.vmType = vmType;
  }
  
  @PreRemove
  private void cleanUp( ) {
    this.persistentVolumes.clear( );
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
  
  public Set<VmVolumeAttachment> getPersistentVolumes( ) {
    return this.persistentVolumes;
  }
  
  public boolean hasPersistentVolumes( ) {
    return !this.persistentVolumes.isEmpty( );
  }
  
  byte[] getUserData( ) {
    return this.userData;
  }
  
  SshKeyPair getSshKeyPair( ) {
    if ( this.getSshKeyString( ) != null ) {
      return SshKeyPair.withPublicKey( null, this.getSshKeyString( ).replaceAll( ".*@eucalyptus\\.", "" ), this.getSshKeyString( ) );
    } else {
      return KeyPairs.noKey( );
    }
  }
  
  VmType getVmType( ) {
    return this.vmType;
  }
  
  void setPlatform( String platform ) {
    this.platform = platform;
  }
  
  public boolean isBlockStorage( ) {
    return this.getMachine( ) instanceof BlockStorageImageInfo;
  }
  
  public boolean isLinux( ) {
    return
        this.getMachine( ) == null ||
        this.getMachine( ).getPlatform( ) == null ||
        ImageMetadata.Platform.linux.equals( this.getMachine( ).getPlatform( ) );
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
  
  void setVmInstance( VmInstance vmInstance ) {
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
  
  private void setVmType( VmType vmType ) {
    this.vmType = vmType;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "VmBootRecord:" );
    if ( this.machineImage != null ) builder.append( "machineImage=" ).append( this.machineImage ).append( ":" );
    if ( this.kernel != null ) builder.append( "kernel=" ).append( this.kernel ).append( ":" );
    if ( this.ramdisk != null ) builder.append( "ramdisk=" ).append( this.ramdisk ).append( ":" );
    if ( this.platform != null ) builder.append( "platform=" ).append( this.platform ).append( ":" );
    if ( this.persistentVolumes != null ) builder.append( "persistentVolumes=" ).append( this.persistentVolumes ).append( ":" );
    if ( this.userData != null ) builder.append( "userData=" ).append( Arrays.toString( this.userData ) ).append( ":" );
    if ( this.sshKeyString != null ) builder.append( "sshKeyPair=" ).append( this.sshKeyString.replaceAll( ".*@eucalyptus\\.", "" ) ).append( ":" );
    if ( this.vmType != null ) builder.append( "vmType=" ).append( this.vmType );
    return builder.toString( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.vmInstance == null )
                                                           ? 0
                                                           : this.vmInstance.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    VmBootRecord other = ( VmBootRecord ) obj;
    if ( this.vmInstance == null ) {
      if ( other.vmInstance != null ) {
        return false;
      }
    } else if ( !this.vmInstance.equals( other.vmInstance ) ) {
      return false;
    }
    return true;
  }

  private String getSshKeyString( ) {
    return this.sshKeyString;
  }

  private void setSshKeyString( String sshKeyString ) {
    this.sshKeyString = sshKeyString;
  }
  
}
