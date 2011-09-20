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

package com.eucalyptus.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Multimap;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_rule" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkRule extends AbstractPersistent {
  public enum Protocol {
    icmp, tcp, udp;
  }
  
  private static final long serialVersionUID = 1L;
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_network_rule_protocol" )
  private Protocol          protocol;
  @Column( name = "metadata_network_rule_low_port" )
  private Integer           lowPort;
  @Column( name = "metadata_network_rule_high_port" )
  private Integer           highPort;
  @ElementCollection
  @CollectionTable( name = "metadata_network_group_rule_ip_ranges" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>       ipRanges         = new HashSet<String>( );
  @OneToMany( cascade = { CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.EAGER )
  @JoinTable( name = "metadata_network_rule_has_peer_network", joinColumns = { @JoinColumn( name = "metadata_network_rule_id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_network_rule_peer_network_id" ) } )
  @ElementCollection
  @CollectionTable( name = "metadata_network_group_rule_peers" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkPeer>  networkPeers     = new HashSet<NetworkPeer>( );
  
  private NetworkRule( ) {}
  
  private NetworkRule( final String protocol, final Integer lowPort, final Integer highPort,
                       final Collection<String> ipRanges,
                       final Multimap<String, String> peers ) {
    assertThat( "Protocol must be one of: tcp, udp, icmp", protocol, notNullValue( ) );
    this.protocol = Protocol.valueOf( protocol );
    this.lowPort = lowPort;
    this.highPort = highPort;
    if ( ipRanges != null ) {
      this.ipRanges.addAll( ipRanges );
    }
    if ( peers != null ) {
      for ( Entry<String, String> entry : peers.entries( ) ) {
        this.networkPeers.add( new NetworkPeer( this, entry.getKey( ), entry.getValue( ) ) );
      }
    }
  }
  
  public static NetworkRule create( final String protocol, final Integer lowPort, final Integer highPort,
                                    final Multimap<String, String> peers,
                                    final Collection<String> ipRanges ) {
    return new NetworkRule( protocol, lowPort, highPort, ipRanges, peers );
  }
  
  public boolean isValid( ) {
    return "tcp".equals( this.protocol ) || "udp".equals( this.protocol ) || "icmp".equals( this.protocol );
  }
  
  public Protocol getProtocol( ) {
    return this.protocol;
  }
  
  public void setProtocol( final Protocol protocol ) {
    this.protocol = protocol;
  }
  
  public Integer getLowPort( ) {
    return this.lowPort;
  }
  
  public void setLowPort( final Integer lowPort ) {
    this.lowPort = lowPort;
  }
  
  public Integer getHighPort( ) {
    return this.highPort;
  }
  
  public void setHighPort( final Integer highPort ) {
    this.highPort = highPort;
  }
  
  public Set<String> getIpRanges( ) {
    return this.ipRanges;
  }
  
  public void setIpRanges( final Set<String> ipRanges ) {
    this.ipRanges = ipRanges;
  }
  
  public Set<NetworkPeer> getNetworkPeers( ) {
    return this.networkPeers;
  }
  
  public void setNetworkPeers( final Set<NetworkPeer> networkPeers ) {
    this.networkPeers = networkPeers;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.highPort == null )
      ? 0
      : this.highPort.hashCode( ) );
    result = prime * result + ( ( this.lowPort == null )
      ? 0
      : this.lowPort.hashCode( ) );
    result = prime * result + ( ( this.protocol == null )
      ? 0
      : this.protocol.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( !this.getClass( ).equals( obj.getClass( ) ) ) return false;
    final NetworkRule other = ( NetworkRule ) obj;
    if ( this.highPort == null ) {
      if ( other.highPort != null ) return false;
    } else if ( !this.highPort.equals( other.highPort ) ) return false;
    if ( this.lowPort == null ) {
      if ( other.lowPort != null ) return false;
    } else if ( !this.lowPort.equals( other.lowPort ) ) return false;
    if ( this.protocol == null ) {
      if ( other.protocol != null ) return false;
    } else if ( !this.protocol.equals( other.protocol ) ) return false;
    if ( this.ipRanges == null ) {
      if ( other.ipRanges != null ) return false;
    } else if ( !this.ipRanges.equals( other.ipRanges ) ) return false;
    if ( this.networkPeers == null ) {
      if ( other.networkPeers != null ) return false;
    } else if ( !this.networkPeers.equals( other.networkPeers ) ) return false;
    return true;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkRule:%s:%d:%d:ipRanges=%s:networkPeers=%s:",
                          this.protocol, this.lowPort, this.highPort, this.ipRanges, this.networkPeers );
  }
}
