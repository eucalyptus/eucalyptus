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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.hibernate.annotations.Parent;
import com.eucalyptus.images.EphemeralDeviceMapping;
import com.eucalyptus.vmtypes.VmTypes.Format;

/**
 * Static instance type definition of the base/default epehemeral disk configurations (NOT to be
 * confused with {@link EphemeralDeviceMapping}.
 * 
 * @author chris grzegorczyk <grze@eucalyptus.com>
 * @see {@link EphemeralDeviceMapping}
 */
@Embeddable
public class EphemeralDisk implements Comparable<EphemeralDisk> {
  
  @Parent
  private VmType  parent;
  @Column( name = "config_vm_type_ephemeral_disk_name" )
  private String  diskName;  //e.g., ephemeral0
  @Column( name = "config_vm_type_ephemeral_device_name" )
  private String  deviceName;
  @Column( name = "config_vm_type_ephemeral_size" )
  private Integer size;      //in GBs, of course?
  @Enumerated( EnumType.STRING )
  @Column( name = "config_vm_type_ephemeral_format" )
  private Format  format;
  
  private EphemeralDisk( VmType parent, String diskName, String deviceName, Integer size ) {
    super( );
    this.parent = parent;
    this.diskName = diskName;
    this.deviceName = deviceName;
    this.size = size;
  }
  
  private EphemeralDisk( VmType parent, String diskName, String deviceName, Integer size, Format format ) {
    this( parent, diskName, deviceName, size );
    this.format = format;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.deviceName == null ) ? 0 : this.deviceName.hashCode( ) );
    result = prime * result + ( ( this.diskName == null ) ? 0 : this.diskName.hashCode( ) );
    result = prime * result + ( ( this.size == null ) ? 0 : this.size.hashCode( ) );
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
    EphemeralDisk other = ( EphemeralDisk ) obj;
    if ( this.deviceName == null ) {
      if ( other.deviceName != null ) {
        return false;
      }
    } else if ( !this.deviceName.equals( other.deviceName ) ) {
      return false;
    }
    if ( this.diskName == null ) {
      if ( other.diskName != null ) {
        return false;
      }
    } else if ( !this.diskName.equals( other.diskName ) ) {
      return false;
    }
    if ( this.size == null ) {
      if ( other.size != null ) {
        return false;
      }
    } else if ( !this.size.equals( other.size ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( EphemeralDisk o ) {
    return this.equals( o ) ? 0 : this.hashCode( ) - o.hashCode( );
  }

  static EphemeralDisk create( VmType parent, String diskName, String deviceName, Integer size, Format format ) {
    return new EphemeralDisk( parent, diskName, deviceName, size, format );
  }

  static EphemeralDisk create( String diskName, String deviceName, Integer size, Format format ) {
    return new EphemeralDisk( null, diskName, deviceName, size, format );
  }

  VmType getParent( ) {
    return this.parent;
  }

  String getDiskName( ) {
    return this.diskName;
  }

  String getDeviceName( ) {
    return this.deviceName;
  }

  Integer getSize( ) {
    return this.size;
  }

  Format getFormat( ) {
    return this.format;
  }

  private void setParent( VmType parent ) {
    this.parent = parent;
  }

  private void setDiskName( String diskName ) {
    this.diskName = diskName;
  }

  private void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  private void setSize( Integer size ) {
    this.size = size;
  }

  private void setFormat( Format format ) {
    this.format = format;
  }
  
}
