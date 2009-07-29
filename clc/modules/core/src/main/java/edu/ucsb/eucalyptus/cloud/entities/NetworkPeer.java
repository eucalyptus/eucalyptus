/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "network_rule_peer_network" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkPeer {

  @Id
  @GeneratedValue
  @Column( name = "network_rule_peer_network_id" )
  private Long id = -1l;
  @Column( name = "network_rule_peer_network_user_query_key" )
  private String userQueryKey;
  @Column( name = "network_rule_peer_network_user_group" )
  private String groupName;

  public NetworkPeer() {}

  public NetworkPeer( final String userQueryKey, final String groupName )
  {
    this.userQueryKey = userQueryKey;
    this.groupName = groupName;
  }

  public Long getId()
  {
    return id;
  }

  public String getUserQueryKey()
  {
    return userQueryKey;
  }

  public void setUserQueryKey( final String userQueryKey )
  {
    this.userQueryKey = userQueryKey;
  }

  public String getGroupName()
  {
    return groupName;
  }

  public void setGroupName( final String groupName )
  {
    this.groupName = groupName;
  }

  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    NetworkPeer that = ( NetworkPeer ) o;

    if ( !groupName.equals( that.groupName ) ) return false;
    if ( !userQueryKey.equals( that.userQueryKey ) ) return false;

    return true;
  }

  public int hashCode()
  {
    int result;
    result = userQueryKey.hashCode();
    result = 31 * result + groupName.hashCode();
    return result;
  }

  public List<NetworkRule> getAsNetworkRules()
  {
    List<NetworkRule> ruleList = new ArrayList<NetworkRule>();
    ruleList.add( new NetworkRule( "tcp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "udp", 0, 65535, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    ruleList.add( new NetworkRule( "icmp", -1, -1, new NetworkPeer( this.getUserQueryKey(), this.getGroupName() ) ) );
    return ruleList;
  }
}
