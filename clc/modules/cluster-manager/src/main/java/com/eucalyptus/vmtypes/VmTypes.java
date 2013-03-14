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

package com.eucalyptus.vmtypes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import com.eucalyptus.cloud.CloudMetadata.VmTypeMetadata;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.StaticDiskImage;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.images.BootableImageInfo;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ConfigurableClass( root = "cloud.vmtypes",
                    description = "Parameters controlling the definition of virtual machine types." )
public class VmTypes {
  @ConfigurableField( description = "Default type used when no instance type is specified for run instances.",
                      initial = "m1.small" )
  public static String         DEFAULT_TYPE_NAME        = "m1.small";         //TODO:GRZE:@Configurable
  @Deprecated
  //GRZE: this and all its references must be removed to complete the vm type support
  protected static final Long  SWAP_SIZE_BYTES          = 512 * 1024l * 1024l; // swap is hardcoded at 512MB for now
  @Deprecated
  //GRZE: this and all its references must be removed to complete the vm type support
  private static final long    MIN_EPHEMERAL_SIZE_BYTES = 61440;              // the smallest ext{2|3|4} partition possible
  private static final Integer GB                       = 1024;
  private static final Integer ROOTFS                   = 10;
  
  public static synchronized void update( Set<VmType> newVmTypes ) throws EucalyptusCloudException {
    NavigableSet<VmType> newList = VmTypes.list( );
    if ( newVmTypes.size( ) != newList.size( ) ) throw new EucalyptusCloudException( "Proposed VmTypes fail to satisfy well-ordering requirement." );
    for ( VmType newVm : newVmTypes ) {
      Registry.INSTANCE.update( newVm );
    }
  }
  
  public static synchronized VmType lookup( String name ) throws NoSuchMetadataException {
    return Registry.get( name );
  }
  
  public static synchronized NavigableSet<VmType> list( ) {
    return Registry.list( );
  }
  
  public static String defaultTypeName( ) {
    return DEFAULT_TYPE_NAME;
  }
  
  @Resolver( VmTypeMetadata.class )
  private enum VmResolver implements Function<String, VmType> {
    INSTANCE;
    @Override
    public VmType apply( @Nullable String input ) {
      try {
        return Entities.uniqueResult( VmType.named( input ) );
      } catch ( NoSuchElementException ex ) {
        PredefinedTypes t = PredefinedTypes.valueOf( input.toUpperCase( ).replace( ".", "" ) );
        VmType vmType = VmType.create( input, t.getCpu( ), t.getDisk( ), t.getMemory( ) );
        return Entities.persist( vmType );
      } catch ( Exception ex ) {
        return null;
      }
    }
  }
  
  private static class PersistentMap<K, V> extends ForwardingConcurrentMap<K, V> {
    private static class Persister<V> implements Function<V, V> {
      @Override
      public V apply( @Nullable V input ) {
        try {
          return Entities.merge( input );
        } catch ( Exception ex ) {
          return null;
        }
      }
    }
    
    private static class Deleter<V> implements Predicate<V> {
      @Override
      public boolean apply( @Nullable V input ) {
        Entities.delete( input );
        return true;
      }
    }
    
    private final ConcurrentMap<K, V> backingMap;
    private final Function<K, V>      getFunction;
    private final Function<V, V>      putFunction;
    private final Predicate<V>        removeFunction;
    
    private PersistentMap( Function<K, V> getFunction ) {
      super( );
      ConcurrentMap<K, V> map = new MapMaker( ).makeComputingMap( getFunction );
      this.backingMap = map;
      Class valueType = Classes.genericsToClasses( getFunction ).get( 1 );
      this.getFunction = Entities.asTransaction( getFunction );
      this.putFunction = Entities.asTransaction( valueType, new Persister<V>( ) );
      this.removeFunction = Entities.asTransaction( valueType, new Deleter<V>( ) );
    }
    
    @Override
    protected ConcurrentMap<K, V> delegate( ) {
      return this.backingMap;
    }
    
    public static <K, V> ConcurrentMap<K, V> create( Function<K, V> getFunction ) {
      return new PersistentMap<K, V>( getFunction );
    }
    
    @Override
    public V put( K key, V value ) {
      value = this.putFunction.apply( value );
      V oldValue = delegate( ).put( key, value );
      return oldValue;
    }
    
    @Override
    public V putIfAbsent( K key, V value ) {
      if ( !delegate( ).containsKey( key ) ) {
        return this.put( key, value );
      } else {
        return this.get( key );
      }
    }
    
    @Override
    public V replace( K key, V value ) {
      if ( this.containsKey( key ) ) {
        return this.put( key, value );
      } else return null;
    }
    
    @Override
    public boolean replace( K key, V oldValue, V newValue ) {
      if ( this.containsKey( key ) && this.get( key ).equals( oldValue ) ) {
        this.put( key, newValue );
        return true;
      } else return false;
    }
    
  }
  
  private enum Registry {
    INSTANCE;
    private final ConcurrentMap<String, VmType>                          vmTypeMap = PersistentMap.create( VmResolver.INSTANCE );
    private final AtomicMarkableReference<ConcurrentMap<String, VmType>> ref       = new AtomicMarkableReference<ConcurrentMap<String, VmType>>( null, false );
    
//  .expireAfterAccess( 10, TimeUnit.SECONDS )
    
    private VmType update( VmType vmType ) {
      Registry.INSTANCE.initialize( );
      VmType oldVmType = this.vmTypeMap.get( vmType.getDisplayName( ) );
      if ( oldVmType == null ) {//except to save new vm type
        return this.vmTypeMap.putIfAbsent( vmType.getDisplayName( ), vmType );
      } else if ( vmType.equals( oldVmType ) ) {//return canonical map reference of vm type
        return this.vmTypeMap.get( vmType.getDisplayName( ) );
      } else if ( this.vmTypeMap.replace( vmType.getDisplayName( ), oldVmType, vmType ) ) {//expect to replace old vm type with new
        return this.vmTypeMap.get( vmType.getDisplayName( ) );
      } else {//noop
        return this.vmTypeMap.get( vmType.getDisplayName( ) );
      }
    }
    
    private void initialize( ) {
      if ( this.ref.compareAndSet( null, vmTypeMap, false, true ) ) {
        for ( PredefinedTypes preDefVmType : PredefinedTypes.values( ) ) {
          VmType vmType = this.vmTypeMap.get( preDefVmType.getName( ) );
        }
        this.ref.set( vmTypeMap, true );
      } else if ( this.ref.compareAndSet( vmTypeMap, vmTypeMap, true, true ) ) {
        if ( this.vmTypeMap.size( ) != PredefinedTypes.values( ).length ) {
          for ( PredefinedTypes preDefVmType : PredefinedTypes.values( ) ) {
            if ( !this.vmTypeMap.containsKey( preDefVmType ) ) {
              this.vmTypeMap.putIfAbsent(
                preDefVmType.getName( ),
                VmType.create( preDefVmType.getName( ), preDefVmType.getCpu( ), preDefVmType.getDisk( ), preDefVmType.getMemory( ) ) );
            }
          }
        }
      }
    }
    
    static VmType get( String name ) throws NoSuchMetadataException {
      INSTANCE.initialize( );
      name = ( name == null ? VmTypes.DEFAULT_TYPE_NAME : name );
      VmType ret = null;
      if ( !INSTANCE.vmTypeMap.containsKey( name ) ) {
        throw new NoSuchMetadataException( "Instance type does not exist: " + name );
      } else {
        return INSTANCE.vmTypeMap.get( name );
      }
    }
    
    public static NavigableSet<VmType> list( ) {
      INSTANCE.initialize( );
      return Sets.newTreeSet( INSTANCE.vmTypeMap.values( ) );
    }
    
  }
  
  enum VirtualDevice {
    ephemeral0,
    ephemeral1,
    ephemeral2,
    ephemeral3;
    
    public EphemeralDisk create( String deviceName, Integer size ) {
      return EphemeralDisk.create( this.name( ), deviceName, size, Format.ext3 );
    }
    
    public EphemeralDisk create( String deviceName, Integer size, Format format ) {
      return EphemeralDisk.create( this.name( ), deviceName, size, format );
    }
    
    private static EphemeralDisk create( Integer ephemeralIndex, String deviceName, Integer size, Format format ) {
      return EphemeralDisk.create( "ephemeral" + ephemeralIndex, deviceName, size, format );
    }
    
  }
  
  public enum Format {
    swap,
    ext3,
    none
  }
  
  enum Attribute {
    ssd,
    gpu,
    ebsonly
  }
  
  /**
   * <table>
   * <tr>
   * <th>Type</th>
   * <th>Name</th>
   * <th>EC2 Compute Units (ECU)</th>
   * <th>Virtual Cores</th>
   * <th>Memory</th>
   * <th>Instance Store Volumes</th>
   * <th>Platform</th>
   * <th>I/O</th>
   * <th>Available for Spot Instance</th>
   * </tr>
   * </table>
   * 
   * @author chris grzegorczyk <grze@eucalyptus.com>
   */
  enum PredefinedTypes {
    T1MICRO( "t1.micro",
            1, ROOTFS / 2, GB / 4,
            Attribute.ebsonly ),
    M1SMALL( "m1.small",
            1, ROOTFS / 2, GB / 4,
            VirtualDevice.ephemeral0.create( "/dev/sda2", 10, Format.ext3 ),
            VirtualDevice.ephemeral1.create( "/dev/sda3", 1, Format.swap ) ),
    M1MEDIUM( "m1.medium",
             1, ROOTFS, GB / 2,
             VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ) ),
    C1MEDIUM( "c1.medium",
             2, ROOTFS, GB / 2,
             VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ) ),
    M1LARGE( "m1.large",
            2, ROOTFS, GB / 2,
            VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
            VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    M1XLARGE( "m1.xlarge",
             2, ROOTFS, GB,
             VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
             VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ),
             VirtualDevice.ephemeral2.create( "/dev/sdd", 20, Format.ext3 ),
             VirtualDevice.ephemeral3.create( "/dev/sde", 20, Format.ext3 ) ),
    M2XLARGE( "m2.xlarge",
             2, ROOTFS, 2 * GB,
             VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ) ),
    C1XLARGE( "c1.xlarge",
             2, ROOTFS, 2 * GB,
             VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
             VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ),
             VirtualDevice.ephemeral2.create( "/dev/sdd", 20, Format.ext3 ),
             VirtualDevice.ephemeral3.create( "/dev/sde", 20, Format.ext3 ) ),
    M3XLARGE( "m3.xlarge",
             4, ROOTFS + ROOTFS / 2, 2 * GB,
             Attribute.ebsonly ),
    M22XLARGE( "m2.2xlarge",
              2, 3 * ROOTFS, 4 * GB,
              VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ) ),
    M32XLARGE( "m3.2xlarge",
              4, 3 * ROOTFS, 4 * GB,
              Attribute.ebsonly ),
    CC14XLARGE( "cc1.4xlarge",
               8, 6 * ROOTFS, 3 * GB,
               VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
               VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    M24XLARGE( "m2.4xlarge",
              8, 6 * ROOTFS, 4 * GB,
              VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
              VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    HI14XLARGE( "hi1.4xlarge",
               8, 12 * ROOTFS, 6 * GB,
               Attribute.ssd,
               VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
               VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    CC28XLARGE( "cc2.8xlarge",
               16, 12 * ROOTFS, 6 * GB,
               VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
               VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ),
               VirtualDevice.ephemeral2.create( "/dev/sdd", 20, Format.ext3 ),
               VirtualDevice.ephemeral3.create( "/dev/sde", 20, Format.ext3 ) ),
    CG14XLARGE( "cg1.4xlarge",
               16, 20 * ROOTFS, 12 * GB,
               Attribute.gpu,
               VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
               VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    CR18XLARGE( "cr1.8xlarge",
               16, 24 * ROOTFS, 16 * GB,
               Attribute.ssd,
               VirtualDevice.ephemeral0.create( "/dev/sdb", 20, Format.ext3 ),
               VirtualDevice.ephemeral1.create( "/dev/sdc", 20, Format.ext3 ) ),
    HS18XLARGE( "hs1.8xlarge",
               48, 24 * 100 * ROOTFS, 117 * GB ) {
      {
        for ( int i = 0; i < 24; i++ ) {
          this.getEphemeralDisks( ).add( VirtualDevice.create( i, "/dev/sdb", 20, Format.ext3 ) );
        }
      }
    };
    private final String             name;
    private final Integer            cpu;
    private final Integer            disk;
    private final Integer            memory;
    private final Boolean            ebsOnly;
    private final Integer            ethernetInterfaceLimit;
    private final Set<EphemeralDisk> ephemeralDisks = Sets.newHashSet( );
    
    private PredefinedTypes( String name, Integer cpu, Integer disk, Integer memory, EphemeralDisk... disks ) {
      this.name = name;
      this.cpu = cpu;
      this.disk = disk;
      this.memory = memory;
      this.ethernetInterfaceLimit = 1;
      this.ebsOnly = Boolean.FALSE;
    }
    
    private PredefinedTypes( String name, Integer cpu, Integer disk, Integer memory, Attribute attribute, EphemeralDisk... disks ) {
      this.name = name;
      this.cpu = cpu;
      this.disk = disk;
      this.memory = memory;
      this.ethernetInterfaceLimit = 1;
      this.ebsOnly = Attribute.ebsonly.equals( attribute );
    }
    
    public String getName( ) {
      return this.name;
    }
    
    public Integer getCpu( ) {
      return this.cpu;
    }
    
    public Integer getDisk( ) {
      return this.disk;
    }
    
    public Integer getMemory( ) {
      return this.memory;
    }
    
    public Boolean getEbsOnly( ) {
      return this.ebsOnly;
    }
    
    public Integer getEthernetInterfaceLimit( ) {
      return this.ethernetInterfaceLimit;
    }
    
    public Set<EphemeralDisk> getEphemeralDisks( ) {
      return this.ephemeralDisks;
    }
    
  }
  
  public static VmTypeInfo asVmTypeInfo( VmType vmType, BootableImageInfo img ) throws MetadataException {
    Long imgSize = img.getImageSizeBytes( );
    Long diskSize = vmType.getDisk( ) * 1024l * 1024l * 1024l;
    
    if ( !( img instanceof BlockStorageImageInfo ) && imgSize > diskSize ) {
      throw new InvalidMetadataException( "image too large [size=" + imgSize / ( 1024l * 1024l ) + "MB] for instance type " + vmType.getName( ) + " [disk="
                                          + vmType.getDisk( ) * 1024l + "MB]" );
    }
    VmTypeInfo vmTypeInfo = null;
    if ( img instanceof StaticDiskImage ) {
      if ( ImageMetadata.Platform.windows.equals( img.getPlatform( ) ) ) {
        vmTypeInfo = VmTypes.InstanceStoreWindowsVmTypeInfoMapper.INSTANCE.apply( vmType );
        vmTypeInfo.setEphemeral( 0, "sdb", diskSize - imgSize, "none" );
      } else {
        vmTypeInfo = VmTypes.InstanceStoreVmTypeInfoMapper.INSTANCE.apply( vmType );
        long ephemeralSize = diskSize - imgSize - SWAP_SIZE_BYTES;
        if ( ephemeralSize < MIN_EPHEMERAL_SIZE_BYTES ) {
          throw new InvalidMetadataException( "image too large to accommodate swap and ephemeral [size="
                                              + imgSize
                                              / ( 1024l * 1024l )
                                              + "MB] for instance type "
                                              + vmType.getName( )
                                              + " [disk="
                                              + vmType.getDisk( )
                                              * 1024l
                                              + "MB]" );
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
  
}
