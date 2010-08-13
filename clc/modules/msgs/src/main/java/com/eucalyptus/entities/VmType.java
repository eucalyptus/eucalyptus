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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.entities;

import java.io.Serializable;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "vm_types" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class VmType extends AbstractPersistent implements Serializable, Comparable {
  //TODO: enumerate.
  public static String M1_SMALL  = "m1.small";
  public static String M1_LARGE  = "m1.large";
  public static String M1_XLARGE = "m1.xlarge";
  public static String C1_MEDIUM = "c1.medium";
  public static String C1_XLARGE = "c1.xlarge";
  
  @Column( name = "vm_type_name" )
  private String       name;
  @Column( name = "vm_type_cpu" )
  private Integer      cpu;
  @Column( name = "vm_type_disk" )
  private Integer      disk;
  @Column( name = "vm_type_memory" )
  private Integer      memory;
  
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
  
  public String getName( ) {
    return name;
  }
  
  public void setName( final String name ) {
    this.name = name;
  }
  
  public Integer getCpu( ) {
    return cpu;
  }
  
  public void setCpu( final Integer cpu ) {
    this.cpu = cpu;
  }
  
  public Integer getDisk( ) {
    return disk;
  }
  
  public void setDisk( final Integer disk ) {
    this.disk = disk;
  }
  
  public Integer getMemory( ) {
    return memory;
  }
  
  public void setMemory( final Integer memory ) {
    this.memory = memory;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    VmType vmType = ( VmType ) o;
    
    if ( !cpu.equals( vmType.cpu ) ) return false;
    if ( !disk.equals( vmType.disk ) ) return false;
    if ( !memory.equals( vmType.memory ) ) return false;
    if ( !name.equals( vmType.name ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    int result = name.hashCode( );
    result = 31 * result + cpu.hashCode( );
    result = 31 * result + disk.hashCode( );
    result = 31 * result + memory.hashCode( );
    return result;
  }
  
  public int compareTo( final Object o ) {
    VmType that = ( VmType ) o;
    if ( this.equals( that ) ) return 0;
    if ( ( this.getCpu( ) <= that.getCpu( ) ) && ( this.getDisk( ) <= that.getDisk( ) ) && ( this.getMemory( ) <= that.getMemory( ) ) ) return -1;
    if ( ( this.getCpu( ) >= that.getCpu( ) ) && ( this.getDisk( ) >= that.getDisk( ) ) && ( this.getMemory( ) >= that.getMemory( ) ) ) return 1;
    return 0;
  }
  
  public VmTypeInfo getAsVmTypeInfo( ) {
    return new VmTypeInfo( this.getName( ), this.getMemory( ), this.getDisk( ), this.getCpu( ) );
  }
  
  @Override
  public String toString( ) {
    return "VmType [name='" + name + '\'' + ", cpu=" + cpu + ", disk=" + disk + ", mem=" + memory + "]";
  }
}
