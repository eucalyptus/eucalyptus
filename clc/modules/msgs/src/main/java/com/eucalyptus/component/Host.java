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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ViewId;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.Internets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class Host implements java.io.Serializable, Comparable<Host> {
  private static Logger              LOG       = Logger.getLogger( Host.class );
  private final Address              groupsId;
  private ImmutableList<InetAddress> hostAddresses;
  private ViewId                     viewId;
  private Boolean                    hasDatabase;
  private AtomicLong                 timestamp = new AtomicLong( System.currentTimeMillis( ) );
  private Long                       lastTime  = 0l;
  
  public Host( ViewId viewId, Address jgroupsId, Boolean hasDb, List<InetAddress> hostAddresses ) {
    this.groupsId = jgroupsId;
    this.update( viewId, hasDb, hostAddresses );
  }
  
  public Host( ViewId viewId ) {
    this.groupsId = Hosts.localMembershipAddress( );
    this.update( viewId, Components.lookup( Eucalyptus.class ).isAvailableLocally( ), Internets.getAllInetAddresses( ) );
  }
  
  synchronized void update( ViewId viewId, Boolean hasDb, List<InetAddress> addresses ) {
    this.lastTime = this.timestamp.getAndSet( System.currentTimeMillis( ) );
    if ( this.viewId != null && this.viewId.equals( viewId ) ) {
      LOG.debug( "Spurious update (" + viewId + ") for host: " + this );
    } else {
      LOG.debug( "Applying update (" + viewId + ") for host: " + this );
      ImmutableList<InetAddress> newAddrs = ImmutableList.copyOf( Ordering.from( Internets.INET_ADDRESS_COMPARATOR ).sortedCopy( addresses ) );
      if ( this.viewId == null ) {
        this.viewId = viewId;
        LOG.trace( "Adding host with addresses: " + addresses );
      }
      this.viewId = viewId;
      this.hasDatabase = hasDb;
      this.hostAddresses = newAddrs;
      LOG.trace( "Updated host: " + this );
    }
  }
  
  public Address getGroupsId( ) {
    return this.groupsId;
  }
  
  public ImmutableList<InetAddress> getHostAddresses( ) {
    return this.hostAddresses;
  }
  
  public ViewId getViewId( ) {
    return this.viewId;
  }
  
  public Boolean hasDatabase( ) {
    return this.hasDatabase;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.groupsId == null )
      ? 0
      : this.groupsId.hashCode( ) );
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
    Host other = ( Host ) obj;
    if ( this.groupsId == null ) {
      if ( other.groupsId != null ) {
        return false;
      }
    } else if ( this.groupsId.toString( ).equals( other.groupsId.toString( ) ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( Host that ) {
    return this.getGroupsId( ).compareTo( that.getGroupsId( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "Host:id=%s:viewId=%s:hostAddresses=%s:hasDatabase=%s", this.groupsId, this.viewId, this.hostAddresses, this.hasDatabase );
  }
  
}
