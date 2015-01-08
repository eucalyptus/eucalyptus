/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.network;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.hibernate.annotations.Parent;

@Embeddable
public class NetworkPeer implements Serializable {
  @Transient
  private static final long serialVersionUID = 1L;
  @Parent
  private NetworkRule       networkRule;
  @Column( name = "network_rule_peer_network_user_query_key" )
  private String            otherAccountId;
  @Column( name = "network_rule_peer_network_user_group" )
  private String            groupName;
  @Column( name = "network_rule_peer_network_group_id" )
  private String            groupId;

  NetworkPeer( ) {}

  NetworkPeer( final String userQueryKey,
               final String groupName,
               final String groupId ) {
    this( null, userQueryKey, groupName, groupId );
  }

  NetworkPeer( @Nullable final NetworkRule networkRule,
               final String userQueryKey,
               final String groupName,
               final String groupId ) {
    this.networkRule = networkRule;
    this.otherAccountId = userQueryKey;
    this.groupName = groupName;
    this.groupId = groupId;
  }
  
  public String getUserQueryKey( ) {
    return this.otherAccountId;
  }
  
  public void setUserQueryKey( final String userQueryKey ) {
    this.otherAccountId = userQueryKey;
  }
  
  public String getGroupName( ) {
    return this.groupName;
  }
  
  public void setGroupName( final String groupName ) {
    this.groupName = groupName;
  }

  /**
   * Group ID will not be present for peers created prior to 3.3.
   *
   * @return The group ID or null if not available
   */
  @Nullable
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId( final String groupId ) {
    this.groupId = groupId;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( ( o == null ) || !this.getClass( ).equals( o.getClass( ) ) ) return false;
    
    final NetworkPeer that = ( NetworkPeer ) o;

    if(this.groupId!=null && that.groupId!=null)
    	return this.groupId.equals(that.groupId);
    if ( !this.groupName.equals( that.groupName ) ) return false;
    if ( !this.otherAccountId.equals( that.otherAccountId ) ) return false;
    
    return true;
  }
  
  @Override
  public int hashCode( ) {
    int result;
    if(this.groupId!=null)
    	result = this.groupId.hashCode();
    else{
	    result = this.otherAccountId.hashCode( );
	    result = 31 * result + this.groupName.hashCode( );
    }
    return result;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkPeer:userQueryKey=%s:groupName=%s:groupId=%s", this.otherAccountId, this.groupName, this.groupId );
  }
  
  private NetworkRule getNetworkRule( ) {
    return this.networkRule;
  }
  
  private void setNetworkRule( final NetworkRule networkRule ) {
    this.networkRule = networkRule;
  }
}
