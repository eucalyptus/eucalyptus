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

package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_rule_peer_network" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkPeer extends AbstractPersistent {
  @Column( name = "network_rule_peer_network_user_query_key" )
  String otherAccountId;
  @Column( name = "network_rule_peer_network_user_group" )
  String groupName;
  
  public NetworkPeer( ) {}
  
  public NetworkPeer( final String userQueryKey, final String groupName ) {
    this.otherAccountId = userQueryKey;
    this.groupName = groupName;
  }
  
  public String getUserQueryKey( ) {
    return this.otherAccountId;
  }
  
  public void setUserQueryKey( String userQueryKey ) {
    this.otherAccountId = userQueryKey;
  }
  
  public String getGroupName( ) {
    return this.groupName;
  }
  
  public void setGroupName( String groupName ) {
    this.groupName = groupName;
  }
  
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || !getClass( ).equals( o.getClass( ) ) ) return false;
    
    NetworkPeer that = ( NetworkPeer ) o;
    
    if ( !groupName.equals( that.groupName ) ) return false;
    if ( !otherAccountId.equals( that.otherAccountId ) ) return false;
    
    return true;
  }
  
  public int hashCode( ) {
    int result;
    result = otherAccountId.hashCode( );
    result = 31 * result + groupName.hashCode( );
    return result;
  }
  
  public List<NetworkRule> getAsNetworkRules( ) {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>( );
    ruleList.add( new NetworkRule( "tcp", 0, 65535, new NetworkPeer( this.getUserQueryKey( ), this.getGroupName( ) ) ) );
    ruleList.add( new NetworkRule( "udp", 0, 65535, new NetworkPeer( this.getUserQueryKey( ), this.getGroupName( ) ) ) );
    ruleList.add( new NetworkRule( "icmp", -1, -1, new NetworkPeer( this.getUserQueryKey( ), this.getGroupName( ) ) ) );
    return ruleList;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkPeer:userQueryKey=%s:groupName=%s", this.otherAccountId, this.groupName );
  }
  
}
