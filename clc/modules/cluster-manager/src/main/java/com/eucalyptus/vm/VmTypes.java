/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.vm;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.StaticDiskImage;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.BootableImageInfo;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmTypes {
  private static String DEFAULT_TYPE_NAME = "m1.small";//TODO:GRZE:@Configurable
  protected static final Long SWAP_SIZE_BYTES = 512 * 1024l * 1024l; // swap is hardcoded at 512MB for now
  private static final long MIN_EPHEMERAL_SIZE_BYTES = 61440; // the smallest ext{2|3|4} partition possible
  
  public static VmTypeInfo asVmTypeInfo( VmType vmType, BootableImageInfo img ) throws MetadataException {
    Long imgSize = img.getImageSizeBytes( );
    Long diskSize = vmType.getDisk( )*1024l*1024l*1024l;
  
    if ( !( img instanceof BlockStorageImageInfo ) && imgSize > diskSize ) {
      throw new InvalidMetadataException( "image too large [size=" + imgSize / ( 1024l * 1024l ) + "MB] for instance type " + vmType.getName( ) + " [disk="
                                          + vmType.getDisk( ) * 1024l + "MB]" );
    }
    VmTypeInfo vmTypeInfo = null;
    if ( img instanceof StaticDiskImage ) {
      if( ImageMetadata.Platform.windows.equals( img.getPlatform( ) ) ) {
        vmTypeInfo  = VmTypes.InstanceStoreWindowsVmTypeInfoMapper.INSTANCE.apply( vmType );
        vmTypeInfo.setEphemeral( 0, "sdb", diskSize - imgSize, "none" );
      } else {
        vmTypeInfo = VmTypes.InstanceStoreVmTypeInfoMapper.INSTANCE.apply( vmType );
        long ephemeralSize = diskSize - imgSize - SWAP_SIZE_BYTES;
        if (ephemeralSize < MIN_EPHEMERAL_SIZE_BYTES) {
        	throw new InvalidMetadataException( "image too large to accommodate swap and ephemeral [size=" + imgSize / ( 1024l * 1024l ) + "MB] for instance type " + vmType.getName( ) + " [disk="
                    + vmType.getDisk( ) * 1024l + "MB]" );
        }
        vmTypeInfo.setEphemeral( 0, "sda2", ephemeralSize, "ext3" );
      }
      vmTypeInfo.setRoot( img.getDisplayName( ), ( ( StaticDiskImage ) img ).getManifestLocation( ), imgSize );
    } else if ( img instanceof BlockStorageImageInfo ) {
      vmTypeInfo = VmTypes.BlockStorageVmTypeInfoMapper.INSTANCE.apply( vmType );
      vmTypeInfo.setEbsRoot( img.getDisplayName( ), null, imgSize );
      vmTypeInfo.setEphemeral( 0, "sdb", diskSize, "none" );
    } else {
      throw new InvalidMetadataException( "Failed to identify the root machine image type: " + img );
    }
    return vmTypeInfo;
  }
  
  private enum InstanceStoreVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda1" ) {
        {
          this.setSwap( "sda3", VmTypes.SWAP_SIZE_BYTES );
        }
      };
    }
  };

  private enum InstanceStoreWindowsVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda" );
    }
  };

  private enum BlockStorageVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda" );
    }
  };


  private static VmTypes                         singleton;
  
  private ConcurrentNavigableMap<String, VmType> vmTypeMap;

  private VmTypes( ) {
    this.vmTypeMap = new ConcurrentSkipListMap<String, VmType>( );
    this.update( );
  }

  private static VmTypes getSingleton( ) {
    synchronized(VmTypes.class) {
      singleton = singleton == null ? new VmTypes( ) : singleton;
    }
    return singleton;
  }

  public static synchronized void update( Set<VmType> newVmTypes ) throws EucalyptusCloudException {
    NavigableSet<VmType> newList = VmTypes.list( );
    if ( newVmTypes.size( ) != newList.size( ) ) throw new EucalyptusCloudException( "Proposed VmTypes fail to satisfy well-ordering requirement." );
    for ( VmType newVm : newVmTypes ) {
      if ( !getSingleton( ).vmTypeMap.containsValue( newVm ) ) {
        EntityWrapper<VmType> db = EntityWrapper.get( VmType.class );
        try {
          VmType oldVm = db.getUnique( new VmType( newVm.getName( ) ) );
          oldVm.setCpu( newVm.getCpu( ) );
          oldVm.setDisk( newVm.getDisk( ) );
          oldVm.setMemory( newVm.getMemory( ) );
          db.commit( );
        } catch ( EucalyptusCloudException e ) {
          db.rollback( );
          throw e;
        } catch ( Exception t ) {
          db.rollback( );
          throw new EucalyptusCloudException( t );
        }
      }
    }
  }

  private synchronized void update( ) {
    EntityWrapper<VmType> db = EntityWrapper.get( VmType.class );
    try {
      List<VmType> vmTypeList = db.query( new VmType( ) );

      for ( VmType v : vmTypeList ) {
        this.vmTypeMap.putIfAbsent( v.getName( ), v );

        if ( !this.vmTypeMap.get( v.getName( ) ).equals( v ) ) this.vmTypeMap.replace( v.getName( ), v );
      }

      if ( vmTypeList.isEmpty( ) ) {
        db.add( new VmType( "m1.small", 1, 2, 128 ) );
        db.add( new VmType( "c1.medium", 2, 5, 128 ) );
        db.add( new VmType( "m1.large", 2, 10, 512 ) );
        db.add( new VmType( "m1.xlarge", 2, 10, 1024 ) );
        db.add( new VmType( "c1.xlarge", 4, 10, 2048 ) );
      }
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
  }

  public static synchronized VmType getVmType( String name ) throws NoSuchMetadataException {
    getSingleton( ).update( );
    if( name == null ) {
      return getSingleton( ).vmTypeMap.get( VmTypes.DEFAULT_TYPE_NAME );
    } else {
      if(getSingleton( ).vmTypeMap.containsKey( name ) ) {
        return getSingleton( ).vmTypeMap.get( name );
      } else {
        throw new NoSuchMetadataException( "instance type does not exist: " + name );
      }
    }
  }

  public static synchronized NavigableSet<VmType> list( ) {
    getSingleton( ).update( );
    return new TreeSet<VmType>( getSingleton( ).vmTypeMap.values( ) );
  }

  public static String defaultTypeName( ) {
    return DEFAULT_TYPE_NAME;
  }

}
