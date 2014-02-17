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

import java.util.Map;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.common.CloudMetadata.VmTypeMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.images.DeviceMapping;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Definition of the VM resource type.<br/>
 * Currently reflects resource allocations in the following dimensions and kinds:
 * <ul>
 * <li>CPU: only in terms of quantity of VCPUs allocated on the hypervisor
 * <li>Memory: the number of memories
 * <li>Disk:
 * <ol>
 * <li>Root: size of the root disk
 * <li>Ephemeral: size of the possible ephemeral partitions attached (For epheremeral disk
 * information see the below referenced documents.)
 * </ol>
 * </ul>
 * Continue to be missing:
 * <ul>
 * <li>Placement affinity
 * <li>Placement restrictions
 * <li>EBS optimized storage
 * <li>SSDs
 * <li>GPUs
 * <li>32bit/64bit restrictions
 * <li>Network capacity characteristics (may never be feasible)
 * </ul>
 * 
 * @see <a
 *      href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/InstanceStorage.html#StorageOnInstanceTypes">Storage
 *      on Instance Types</a>
 * @see <a
 *      href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/InstanceStorage.html#InstanceStoreDeviceNames">Instance
 *      Store Device Names</a>
 * @see DeviceMapping
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_vm_type" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VmType extends AbstractPersistent implements VmTypeMetadata, HasFullName<VmTypeMetadata> {
  @Transient
  private static final long  serialVersionUID = 1L;
  
  @Column( name = "config_vm_type_name" )
  private String             name;
  
  @Column( name = "config_vm_type_cpu" )
  private Integer            cpu;
  
  @Column( name = "config_vm_type_disk" )
  private Integer            disk;
  
  @Column( name = "config_vm_type_memory" )
  private Integer            memory;
  
  @Column( name = "config_vm_type_ebs_only" )
  private Boolean            ebsOnly;
  
  @Column( name = "config_vm_type_ebs_iops" )
  private Integer            ebsIopsLimit;
  
  @Column( name = "config_vm_type_64bit_only" )
  private Boolean            x86_64only;
  
  @ElementCollection
  @CollectionTable( name = "config_vm_types_ephemeral_disks" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<EphemeralDisk> epehemeralDisks  = Sets.newHashSet( );
  
  private VmType( ) {}
  
  private VmType( final String name ) {
    this.name = name;
    this.setNaturalId( Crypto.getDigestBase64( name, Digest.SHA1 ) );//this ensures that natural ids are used when unique queries are performed.
  }
  
  private VmType( final String name, final Integer cpu, final Integer disk, final Integer memory ) {
    this( name );
    this.cpu = cpu;
    this.disk = disk;
    this.memory = memory;
  }
  
  public static VmType create( ) {
    return new VmType( );
  }
  
  @Override
  public String getDisplayName( ) {
    return this.name;
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  public void setName( final String name ) {
    this.name = name;
  }
  
  @Override
  public Integer getCpu( ) {
    return this.cpu;
  }
  
  public void setCpu( final Integer cpu ) {
    this.cpu = cpu;
  }
  
  @Override
  public Integer getDisk( ) {
    return this.disk;
  }
  
  public void setDisk( final Integer disk ) {
    this.disk = disk;
  }
  
  @Override
  public Integer getMemory( ) {
    return this.memory;
  }
  
  public void setMemory( final Integer memory ) {
    this.memory = memory;
  }
  
  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( ( o == null ) || ( this.getClass( ) != o.getClass( ) ) ) return false;
    
    final VmType vmType = ( VmType ) o;
    
    if ( !this.cpu.equals( vmType.cpu ) ) return false;
    if ( !this.disk.equals( vmType.disk ) ) return false;
    if ( !this.memory.equals( vmType.memory ) ) return false;
    if ( !this.name.equals( vmType.name ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    int result = this.name.hashCode( );
    result = ( 31 * result ) + this.cpu.hashCode( );
    result = ( 31 * result ) + this.disk.hashCode( );
    result = ( 31 * result ) + this.memory.hashCode( );
    return result;
  }
  
  @Override
  public int compareTo( final VmTypeMetadata that ) {
    if ( this.equals( that ) ) return 0;
    if ( this.getDisk( ) < that.getDisk( ) ) {
      return -1;
    } else if ( this.getDisk( ) > that.getDisk( ) ) {
      return 1;
    }
    if ( this.getMemory( ) < that.getMemory( ) ) {
      return -1;
    } else if ( this.getMemory( ) > that.getMemory( ) ) {
      return 1;
    }
    if ( this.getCpu( ) < that.getCpu( ) ) {
      return -1;
    } else if ( this.getCpu( ) > that.getCpu( ) ) {
      return 1;
    }
    return this.getDisplayName( ).compareTo( that.getDisplayName( ) );
  }
  
  @Override
  public String toString( ) {
    return "VmType " + this.name + " cores=" + this.cpu + " disk=" + this.disk + " mem=" + this.memory;
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( Principals.systemFullName( ).getAccountNumber( ) )
                          .relativeId( "vm-type", this.getName( ) );
  }
  
  @Override
  public OwnerFullName getOwner( ) {
    return Principals.nobodyFullName( );
  }
  
  public Supplier<VmType> allocator( ) {
    return new Supplier<VmType>( ) {
      
      @Override
      public VmType get( ) {
        return VmType.this;
      }
    };
  }
  
  public enum SizeProperties implements Function<VmType, Integer> {
    Cpu {
      @Override
      public Integer apply( final VmType vmType ) {
        return vmType.getCpu( );
      }
    },
    Disk {
      @Override
      public Integer apply( final VmType vmType ) {
        return vmType.getDisk( );
      }
    },
    Memory {
      @Override
      public Integer apply( final VmType vmType ) {
        return vmType.getMemory( );
      }
    }
  }
  
  public static class EphemeralBuilder {
    private Integer              index       = 0;
    
    private Map<Integer, String> deviceNames = Maps.newHashMap( );
    
    private Set<EphemeralDisk>   disks       = Sets.newHashSet( );
    
    private VmType               parent;
    
    EphemeralBuilder( VmType parent ) {
      super( );
      this.parent = parent;
    }
    
    private String getDiskName( String deviceName ) {
      if ( "/dev/sda1".equals( deviceName ) ) {
        throw new IllegalArgumentException( "Attempt to assign restricted device name: " + deviceName );
      } else if ( deviceNames.containsValue( deviceName ) ) {
        throw new IllegalArgumentException( "Attempt to assign same device name to multiple devices: " + deviceName + " with " + deviceNames.entrySet( ) );
      } else {
        int idx = index++;
        deviceNames.put( idx, deviceName );
        return "ephemeral" + idx;
      }
    }
    
    public EphemeralBuilder addDisk( EphemeralDisk disk ) {
      String diskName = getDiskName( disk.getDeviceName( ) );
      EphemeralDisk ephemeral = EphemeralDisk.create( this.parent, diskName, disk.getDeviceName( ), disk.getSize( ), disk.getFormat( ) );
      disks.add( ephemeral );
      return this;
    }
    
    public VmType commit( ) {
      this.parent.getEpehemeralDisks( ).addAll( disks );
      return this.parent;
    }
  }
  
  Boolean getEbsOnly( ) {
    return this.ebsOnly;
  }
  
  void setEbsOnly( Boolean ebsOnly ) {
    this.ebsOnly = ebsOnly;
  }
  
  Integer getEbsIopsLimit( ) {
    return this.ebsIopsLimit;
  }
  
  void setEbsIopsLimit( Integer ebsIopsLimit ) {
    this.ebsIopsLimit = ebsIopsLimit;
  }
  
  Boolean getX86_64only( ) {
    return this.x86_64only;
  }
  
  void setX86_64only( Boolean x86_64only ) {
    this.x86_64only = x86_64only;
  }
  
  Set<EphemeralDisk> getEpehemeralDisks( ) {
    return this.epehemeralDisks;
  }
  
  public void addEphemeralDisks( EphemeralDisk... disks ) {
    EphemeralBuilder builder = this.withEphemeralDisks( );
    for ( EphemeralDisk d : disks ) {
      builder.addDisk( d );
    }
  }
  
  public VmType.EphemeralBuilder withEphemeralDisks( ) {
    this.getEpehemeralDisks( ).clear( );
    return new VmType.EphemeralBuilder( this );
  }
  
  public static VmType create( String name, Integer cpu, Integer disk, Integer memory ) {
    return new VmType( name, cpu, disk, memory );
  }
  
  public static VmType named( String name ) {
    return new VmType( name );
  }
  
  
  public Predicate<VmType> orderedPredicate( ) {
    return new Predicate<VmType>( ) {
      
      @Override
      public boolean apply( VmType vm ) {
        boolean gtcpu = vm.cpu > VmType.this.cpu, eqcpu = vm.cpu == VmType.this.cpu, ltcpu = vm.cpu < VmType.this.cpu;
        boolean gtdisk = vm.disk > VmType.this.disk, eqdisk = vm.disk == VmType.this.disk, ltdisk = vm.disk < VmType.this.disk;
        boolean gtmemory = vm.memory > VmType.this.memory, eqmem = vm.memory == VmType.this.memory, ltmem = vm.memory < VmType.this.memory;
        
        boolean singleOrder = ( gtcpu && gtdisk && ltmem ) ||
                              ( gtcpu && gtmemory && ltdisk ) ||
                              ( gtdisk && gtmemory && ltcpu );
        
        boolean doubleOrder = ( gtmemory && ltcpu && ltdisk ) ||
                              ( gtdisk && ltcpu && ltmem ) ||
                              ( gtcpu && ltdisk && ltmem );
        
        return !this.equals( vm ) && ( singleOrder || doubleOrder );
      }
    };
  }
}
