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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package edu.ucsb.eucalyptus.cloud;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import com.eucalyptus.auth.principal.AccountFullName;

public class NetworkToken implements Comparable {
  private final String networkUuid;
  private final String networkName;
  
  public String getNetworkUuid( ) {
    return this.networkUuid;
  }
  
  public String getNetworkName( ) {
    return this.networkName;
  }
  
  private final String          cluster;
  private final Integer         vlan;
  private NavigableSet<Integer> indexes = new ConcurrentSkipListSet<Integer>( );
  private final String          name;
  private final AccountFullName accountFullName;
  
  public NetworkToken( final String cluster, final AccountFullName accountFullName, final String networkName, final String networkUuid, final int vlan ) {
    this.networkName = networkName;
    this.networkUuid = networkUuid;
    this.cluster = cluster;
    this.vlan = vlan;
    this.accountFullName = accountFullName;
    this.name = this.accountFullName.getAccountNumber( ) + "-" + this.networkName;
  }
  
  public String getCluster( ) {
    return this.cluster;
  }
  
  public Integer getVlan( ) {
    return this.vlan;
  }
  
  public String getAccountNumber( ) {
    return this.accountFullName.getAccountNumber( );
  }
  
  public AccountFullName getAccountFullName( ) {
    return this.accountFullName;
  }

  public String getName( ) {
    return this.name;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkToken:%s:cluster=%s:vlan=%s:indexes=%s", this.name, this.cluster, this.vlan, this.indexes );
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof NetworkToken ) ) return false;
    NetworkToken that = ( NetworkToken ) o;
    
    if ( !this.cluster.equals( that.cluster ) ) return false;
    if ( !this.networkName.equals( that.networkName ) ) return false;
    if ( !this.accountFullName.getAccountNumber( ).equals( that.accountFullName.getAccountNumber( ) ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    int result;
    
    result = networkName.hashCode( );
    result = 31 * result + cluster.hashCode( );
    result = 31 * result + accountFullName.getAccountNumber( ).hashCode( );
    return result;
  }
  
  @Override
  public int compareTo( Object o ) {
    NetworkToken that = ( NetworkToken ) o;
    return ( !this.cluster.equals( that.cluster ) && ( this.vlan.equals( that.vlan ) ) )
      ? this.vlan - that.vlan
      : this.cluster.compareTo( that.cluster );
  }
  
  public void removeIndex( Integer index ) {
    this.indexes.remove( index );
  }
  
  public void allocateIndex( Integer nextIndex ) {
    this.indexes.add( nextIndex );
  }
  
  public boolean isEmpty( ) {
    return this.indexes.isEmpty( );
  }
  
  public NavigableSet<Integer> getIndexes( ) {
    return this.indexes;
  }
  
}
