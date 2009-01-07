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

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.CascadeType;
import java.util.*;

@Entity
@Table( name = "network_rule" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class NetworkRule {

  @Id
  @GeneratedValue
  @Column( name = "network_rule_id" )
  private Long id = -1l;
  @Column( name = "network_rule_protocol" )
  String protocol;
  @Column( name = "network_rule_low_port" )
  Integer lowPort;
  @Column( name = "network_rule_high_port" )
  Integer highPort;
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "network_rule_has_ip_range",
      joinColumns = { @JoinColumn( name = "network_rule_id" ) },
      inverseJoinColumns = @JoinColumn( name = "network_rule_ip_range_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<IpRange> ipRanges = new ArrayList<IpRange>();
  @OneToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "network_rule_has_peer_network",
      joinColumns = { @JoinColumn( name = "network_rule_id" ) },
      inverseJoinColumns = @JoinColumn( name = "network_rule_peer_network_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<NetworkPeer> networkPeers = new ArrayList<NetworkPeer>();

  public NetworkRule() {}

  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort, final List<IpRange> ipRanges )
  {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
    this.ipRanges = ipRanges;
  }

  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort, final NetworkPeer peer )
  {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
    this.networkPeers.add( peer );
  }
  public NetworkRule( final String protocol, final Integer lowPort, final Integer highPort )
  {
    this.protocol = protocol;
    this.lowPort = lowPort;
    this.highPort = highPort;
  }

  public Long getId()
  {
    return id;
  }

  public String getProtocol()
  {
    return protocol;
  }

  public void setProtocol( final String protocol )
  {
    this.protocol = protocol;
  }

  public Integer getLowPort()
  {
    return lowPort;
  }

  public void setLowPort( final Integer lowPort )
  {
    this.lowPort = lowPort;
  }

  public Integer getHighPort()
  {
    return highPort;
  }

  public void setHighPort( final Integer highPort )
  {
    this.highPort = highPort;
  }

  public List<IpRange> getIpRanges()
  {
    return ipRanges;
  }

  public void setIpRanges( final List<IpRange> ipRanges )
  {
    this.ipRanges = ipRanges;
  }

  public List<NetworkPeer> getNetworkPeers()
  {
    return networkPeers;
  }

  public void setNetworkPeers( final List<NetworkPeer> networkPeers )
  {
    this.networkPeers = networkPeers;
  }

  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    NetworkRule that = ( NetworkRule ) o;
    if ( !highPort.equals( that.highPort ) ) return false;
    if ( !lowPort.equals( that.lowPort ) ) return false;
    if ( !protocol.equals( that.protocol ) ) return false;

    if ( !this.ipRanges.isEmpty() && !that.ipRanges.isEmpty() && !ipRanges.equals( that.ipRanges ) ) return false;
    else if ( !that.networkPeers.isEmpty() && !this.networkPeers.isEmpty() && !networkPeers.equals( that.networkPeers ) ) return false;
    return true;
  }

  public int hashCode()
  {
    int result;
    result = protocol.hashCode();
    result = 31 * result + lowPort.hashCode();
    result = 31 * result + highPort.hashCode();
    result = 31 * result + ipRanges.hashCode();
    result = 31 * result + networkPeers.hashCode();
    return result;
  }

  public boolean isValid() {
    return this.protocol.equals( "tcp" ) || this.protocol.equals( "udp" ) || this.protocol.equals( "icmp" );
  }

}
