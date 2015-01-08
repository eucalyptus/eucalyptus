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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.util.Pair;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_rule" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkRule extends AbstractPersistent {

  public static final Pattern PROTOCOL_PATTERN = Pattern.compile( "icmp|tcp|udp|[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5]|-1" );

  /**
   * 
   */
  public static final int RULE_MIN_PORT = 0;

  /**
   * 
   */
  public static final int RULE_MAX_PORT = 65535;

  public enum Protocol {
    icmp(1){
      @Override public Integer extractLowPort ( final NetworkRule rule ) { return null; }
      @Override public Integer extractHighPort( final NetworkRule rule ) { return null; }
      @Override public Integer extractIcmpType( final NetworkRule rule ) { return rule.getLowPort( ); }
      @Override public Integer extractIcmpCode( final NetworkRule rule ) { return rule.getHighPort( ); }
    },
    tcp(6),
    udp(17)
    ;

    private final int number;

    private Protocol( int number ) {
      this.number = number;
    }

    public int getNumber( ) {
      return number;
    }

    public static Protocol fromString( final String value ) {
      try {
        return Protocol.valueOf( value );
      } catch ( IllegalArgumentException e ) {
        final Integer protocolNumber = Ints.tryParse( value );
        if ( protocolNumber != null ) for ( final Protocol protocol : values( ) ) {
          if ( protocolNumber == protocol.number ) return protocol;
        }
        throw e;
      }
    }

    public Integer extractLowPort ( NetworkRule rule ) { return rule.getLowPort( ); }
    public Integer extractHighPort( NetworkRule rule ) { return rule.getHighPort( ); }
    public Integer extractIcmpType( NetworkRule rule ) { return null; }
    public Integer extractIcmpCode( NetworkRule rule ) { return null; }
  }
  
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_network_rule_egress", updatable = false )
  private Boolean           egress;
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_network_rule_protocol", updatable = false  )
  private Protocol          protocol;
  @Column( name = "metadata_network_rule_protocol_number", updatable = false  )
  private Integer           protocolNumber;
  @Column( name = "metadata_network_rule_low_port", updatable = false  )
  private Integer           lowPort;
  @Column( name = "metadata_network_rule_high_port", updatable = false  )
  private Integer           highPort;
  
  @ElementCollection
  @CollectionTable( name = "metadata_network_rule_ip_ranges" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>       ipRanges         = Sets.newHashSet( );
  
  @ElementCollection
  @CollectionTable( name = "metadata_network_group_rule_peers" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkPeer>  networkPeers     = Sets.newHashSet( );

  protected NetworkRule( ) {}
  
  protected NetworkRule( final Protocol protocol,
                         final Integer protocolNumber,
                         final Integer lowPort,
                         final Integer highPort,
                         final Collection<String> ipRanges,
                         final Collection<NetworkPeer> peers ) {
    this.egress = false;
    this.protocol = protocol;
    this.protocolNumber = protocolNumber;
    if ( Protocol.tcp.equals( protocol ) || Protocol.udp.equals( protocol ) ) {
      if ( lowPort < RULE_MIN_PORT || highPort < RULE_MIN_PORT ) {
        throw new IllegalArgumentException( "Provided ports must be greater than " + RULE_MIN_PORT + ": lowPort=" + lowPort + " highPort=" + highPort );
      } else if ( lowPort > RULE_MAX_PORT || highPort > RULE_MAX_PORT ) {
        throw new IllegalArgumentException( "Provided ports must be less than " + RULE_MAX_PORT + ": lowPort=" + lowPort + " highPort=" + highPort );
      } else if ( lowPort > highPort ) {
        throw new IllegalArgumentException( "Provided lowPort is greater than highPort: lowPort=" + lowPort + " highPort=" + highPort );
      }
    }
    this.lowPort = lowPort;
    this.highPort = highPort;
    if ( ipRanges != null ) {
      this.ipRanges.addAll( ipRanges );
    }
    if ( peers != null ) {
      this.networkPeers.addAll( peers );
    }
  }

  public static NetworkRule create( final Protocol protocol,
                                    final Integer lowPort,
                                    final Integer highPort,
                                    final Collection<NetworkPeer> peers,
                                    final Collection<String> ipRanges ) {
    return create( protocol, protocol.number, lowPort, highPort, peers, ipRanges );
  }

  public static NetworkRule create( final Protocol protocol,
                                    final Integer protocolNumber,
                                    final Integer lowPort,
                                    final Integer highPort,
                                    final Collection<NetworkPeer> peers,
                                    final Collection<String> ipRanges ) {
    return new NetworkRule( protocol, protocolNumber, lowPort, highPort, ipRanges, peers );
  }

  public static NetworkRule createEgress( final Protocol protocol,
                                          final Integer protocolNumber,
                                          final Integer lowPort,
                                          final Integer highPort,
                                          final Collection<NetworkPeer> peers,
                                          final Collection<String> ipRanges ) {
    final NetworkRule rule = new NetworkRule( protocol, protocolNumber, lowPort, highPort, ipRanges, peers );
    rule.setEgress( true );
    return rule;
  }

  public static NetworkRule create( final String protocolText,
                                    final boolean vpc,
                                    final Integer lowPort,
                                    final Integer highPort,
                                    final Collection<NetworkPeer> peers,
                                    final Collection<String> ipRanges ) {
    Pair<Optional<Protocol>,Integer> protocolPair = parseProtocol( protocolText, vpc );
    return create( protocolPair.getLeft( ).orNull( ), protocolPair.getRight( ), lowPort, highPort, peers, ipRanges );
  }

  protected static Pair<Optional<Protocol>,Integer> parseProtocol(
      final String protocolText,
      final boolean vpc
  ) {
    Protocol protocol = null;
    try {
      protocol = Protocol.fromString( protocolText );
    } catch ( final IllegalArgumentException e ) {
      if ( !vpc ) throw e;
    }
    Integer protocolNumber = protocol != null ?
        protocol.getNumber( ) :
        Integer.parseInt( protocolText );
    return Pair.lopair( protocol, protocolNumber );
  }

  public static NetworkRule named( ) {
    return new NetworkRule( );
  }

  /**
   * @since 4.1
   */
  public boolean isEgress( ) {
    return egress == null ? false : egress;
  }

  @Nullable
  public Boolean getEgress( ) {
    return egress;
  }

  public void setEgress( final Boolean egress ) {
    this.egress = egress;
  }

  /**
   * Get the protocol for this rule.
   *
   * @return The protocol if named (e.g. icmp, tcp, udp), else null
   */
  @Nullable
  public Protocol getProtocol( ) {
    return this.protocol;
  }
  
  public void setProtocol( final Protocol protocol ) {
    this.protocol = protocol;
  }

  /**
   * @since 4.1
   */
  @Nullable
  public Integer getProtocolNumber( ) {
    return protocolNumber;
  }

  public void setProtocolNumber( final Integer protocolNumber ) {
    this.protocolNumber = protocolNumber;
  }

  public String getDisplayProtocol( ) {
    return protocol != null ?
        protocol.name( ) :
        Objects.toString( protocolNumber, "-1" );
  }

  @Nullable
  public Integer getLowPort( ) {
    return this.lowPort;
  }
  
  public void setLowPort( final Integer lowPort ) {
    this.lowPort = lowPort;
  }

  @Nullable
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

  public boolean isVpcOnly( ) {
    return getLowPort() == null || getHighPort() == null || getProtocol() == null;
  }

  public static Predicate<NetworkRule> egress( ) {
    return NetworkRulePredicates.EGRESS;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.highPort == null ) ? 0 : this.highPort.hashCode( ) );
    result = prime * result + ( ( this.ipRanges == null ) ? 0 : this.ipRanges.hashCode( ) );
    result = prime * result + ( ( this.lowPort == null ) ? 0 : this.lowPort.hashCode( ) );
    result = prime * result + ( ( this.networkPeers == null ) ? 0 : this.networkPeers.hashCode( ) );
    result = prime * result + ( ( this.protocol == null ) ? 0 : this.protocol.hashCode( ) );
    result = prime * result + ( ( this.protocolNumber == null ) ? 0 : this.protocolNumber.hashCode( ) );
    result = prime * result + ( ( this.egress == null || !this.egress) ? 0 : this.egress.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    NetworkRule other = ( NetworkRule ) obj;
    if ( this.highPort == null ) {
      if ( other.highPort != null ) {
        return false;
      }
    } else if ( !this.highPort.equals( other.highPort ) ) {
      return false;
    }
    if ( this.ipRanges == null ) {
      if ( other.ipRanges != null ) {
        return false;
      }
    } else if ( !this.ipRanges.equals( other.ipRanges ) ) {
      return false;
    }
    if ( this.lowPort == null ) {
      if ( other.lowPort != null ) {
        return false;
      }
    } else if ( !this.lowPort.equals( other.lowPort ) ) {
      return false;
    }
    if ( this.networkPeers == null ) {
      if ( other.networkPeers != null ) {
        return false;
      }
    } else if ( !this.networkPeers.equals( other.networkPeers ) ) {
      return false;
    }
    if ( this.protocol != other.protocol ) {
      return false;
    }
    if ( this.protocolNumber == null ) {
      if ( other.protocolNumber != null ) {
        return false;
      }
    } else if ( !this.protocolNumber.equals( other.protocolNumber ) ) {
      return false;
    }
    if ( this.isEgress( ) != other.isEgress( ) ) {
      return false;
    }
    return true;
  }

  @Override
  public String toString( ) {
    return String.format( "NetworkRule:%s:%d:%d:ipRanges=%s:networkPeers=%s:",
                          this.protocol, this.lowPort, this.highPort, this.ipRanges, this.networkPeers );
  }

  @PrePersist
  @PreUpdate
  protected void onUpdate( ) {
    if ( protocol != null ) protocolNumber = protocol.getNumber( );
  }

  private enum NetworkRulePredicates implements Predicate<NetworkRule> {
    EGRESS {
      @Override
      public boolean apply( @Nullable final NetworkRule networkRule ) {
        return networkRule != null && networkRule.isEgress( );
      }
    }
  }

}
