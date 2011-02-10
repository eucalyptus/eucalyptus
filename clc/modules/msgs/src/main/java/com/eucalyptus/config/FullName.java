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

package com.eucalyptus.config;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Assertions;

public class FullName {
  private final String partition;
  private final String name;
  private final String qName;
  private final String path = ""; 
  
  public FullName( ComponentId componentType, String partition, String name, String... pathParts ) {
    Assertions.assertArgumentNotNull( partition );
    Assertions.assertArgumentNotNull( name );
    this.partition = partition;
    this.name = name;
    StringBuilder b = new StringBuilder( );
    b.append( "arn:aws:euca:" ).append( partition );
    b.append( ":" );
    if( componentType != null ) {
      b.append( componentType );
    }
    b.append( ":" ).append( name );
    for( String pathPart : pathParts ) {
      b.append( "/" ).append( pathPart );
    }
    this.qName = b.toString( );
  }
  
  public final String getPartition( ) {
    return this.partition;
  }
  
  public final String getName( ) {
    return this.name;
  }
  
  public final String getQName( ) {
    return this.qName;
  }
  
  @Override
  public String toString( ) {
    return this.qName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name == null )
      ? 0
      : this.name.hashCode( ) );
    result = prime * result + ( ( this.partition == null )
      ? 0
      : this.partition.hashCode( ) );
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
    if ( !this.getClass( ).equals( obj.getClass( ) ) ) {
      return false;
    }

    FullName other = ( FullName ) obj;
    if ( this.name == null ) {
      if ( other.name != null ) {
        return false;
      }
    } else if ( !this.name.equals( other.name ) ) {
      return false;
    }
    if ( this.partition == null ) {
      if ( other.partition != null ) {
        return false;
      }
    } else if ( !this.partition.equals( other.partition ) ) {
      return false;
    }
    return true;
  }
  
//  @Override
//  public int hashCode( ) {
//    final int prime = 31;
//    int result = 1;
//    result = prime * result +
//             ( ( this.partition == null )
//               ? 0
//               : this.partition.hashCode( ) ) +
//             ( ( this.name == null )
//               ? 0
//               : this.name.hashCode( ) );
//    return result;
//  }
  
//  @Override
//  public boolean equals( Object obj ) {
//    if ( this == obj ) return true;
//    if ( obj == null ) return false;
//    if ( !getClass( ).equals( obj ) ) return false;
//    
//    FullName that = ( FullName ) obj;
//    return ( ( this.partition == null && this.name == null )
//             || ( this.name == null && this.partition.equals( that.getPartition( ) ) )
//             || ( this.partition == null && this.name.equals( that.getName( ) ) ) || ( this.partition.equals( that.getPartition( ) ) && this.name.equals( that.getName( ) ) ) );
//  }
//  
}
