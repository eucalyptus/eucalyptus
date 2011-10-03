/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.vm;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.CloudMetadata.VmTypeMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;


@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_vm_types" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
//@ConfigurableClass(root="eucalyptus",alias="vmtypes",deferred=true,singleton=false,description="Virtual Machine type definitions")
public class VmType extends AbstractPersistent implements VmTypeMetadata, HasFullName<VmTypeMetadata> {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  //  @ConfigurableIdentifier
  @Column( name = "metadata_vm_type_name" )
  private String            name;
//  @ConfigurableField( description = "Number of CPUs per instance.", displayName = "CPUs" )
  @Column( name = "metadata_vm_type_cpu" )
  private Integer           cpu;
//  @ConfigurableField( description = "Gigabytes of disk per instance.", displayName = "Disk (GB)" )
  @Column( name = "metadata_vm_type_disk" )
  private Integer           disk;
//  @ConfigurableField( description = "Gigabytes of RAM per instance.", displayName = "RAM (GB)" )
  @Column( name = "metadata_vm_type_memory" )
  private Integer           memory;
  
  public VmType( ) {}
  
  public VmType( final String name ) {
    this.name = name;
  }
  
  public VmType( final String name, final Integer cpu, final Integer disk, final Integer memory ) {
    this.name = name;
    this.cpu = cpu;
    this.disk = disk;
    this.memory = memory;
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
    result = 31 * result + this.cpu.hashCode( );
    result = 31 * result + this.disk.hashCode( );
    result = 31 * result + this.memory.hashCode( );
    return result;
  }
  
  @Override
  public int compareTo( final VmTypeMetadata that ) {
    if ( this.equals( that ) ) return 0;
    if ( ( this.getCpu( ) <= that.getCpu( ) ) && ( this.getDisk( ) <= that.getDisk( ) ) && ( this.getMemory( ) <= that.getMemory( ) ) ) return -1;
    if ( ( this.getCpu( ) >= that.getCpu( ) ) && ( this.getDisk( ) >= that.getDisk( ) ) && ( this.getMemory( ) >= that.getMemory( ) ) ) return 1;
    return 0;
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
    return new Supplier<VmType>() {

      @Override
      public VmType get( ) {
        return VmType.this;
      }
    };
  }
  
}
