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
package com.eucalyptus.cluster;

import com.eucalyptus.entities.VmType;

public class VmTypeAvailability implements Comparable {
  private VmType type;
  private int max;
  private int available;

  public VmTypeAvailability( final VmType type, final int max, final int available ) {
    this.type = type;
    this.max = max;
    this.available = available;
  }

  public VmType getType() {
    return type;
  }

  public void decrement( int quantity ) {
    this.available -= quantity;
    this.available = ( this.available < 0 ) ? 0 : this.available;
  }

  public int getMax() {
    return max;
  }

  public void setMax( final int max ) {
    this.max = max;
  }

  public int getAvailable() {
    return available;
  }

  public void setAvailable( final int available ) {
    this.available = available;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof VmTypeAvailability ) ) return false;

    VmTypeAvailability that = ( VmTypeAvailability ) o;

    if ( !type.equals( that.type ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  public int compareTo( final Object o ) {
    VmTypeAvailability v = ( VmTypeAvailability ) o;
    if ( v.getAvailable() == this.getAvailable() ) return this.type.compareTo( v.getType() );
    return v.getAvailable() - this.getAvailable();
  }

  @Override
  public String toString() {
    return "VmTypeAvailability{" +
           "type=" + type +
           ", max=" + max +
           ", available=" + available +
           "}\n";
  }

  public static VmTypeAvailability ZERO = new ZeroTypeAvailability();

  static class ZeroTypeAvailability extends VmTypeAvailability {
    ZeroTypeAvailability() {
      super( new VmType( "ZERO", -1, -1, -1 ), 0, 0 );
    }

    @Override
    public int compareTo( final Object o ) {
      VmTypeAvailability v = ( VmTypeAvailability ) o;
      if ( v == ZERO ) return 0;
      if ( v.getAvailable() > 0 ) return 1;
      else return -1;
    }

    @Override
    public void setAvailable( final int available ) {}

    @Override
    public void decrement( final int quantity ) {}

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      return false;
    }

  }

}

