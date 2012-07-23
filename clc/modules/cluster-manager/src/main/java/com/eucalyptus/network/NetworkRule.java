/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_rule" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkRule extends AbstractPersistent {
  /**
   * 
   */
  private static final int RULE_MIN_PORT = 0;
  /**
   * 
   */
  private static final int RULE_MAX_PORT = 65535;

  public enum Protocol {
    icmp, tcp, udp;
  }
  
  @Transient
  private static final long serialVersionUID = 1L;
  @Enumerated( EnumType.STRING )
  @Column( name = "metadata_network_rule_protocol" )
  private Protocol          protocol;
  @Column( name = "metadata_network_rule_low_port" )
  private Integer           lowPort;
  @Column( name = "metadata_network_rule_high_port" )
  private Integer           highPort;
  
  @ElementCollection
  @CollectionTable( name = "metadata_network_rule_ip_ranges" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<String>       ipRanges         = Sets.newHashSet( );
  
  @ElementCollection
  @CollectionTable( name = "metadata_network_group_rule_peers" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkPeer>  networkPeers     = Sets.newHashSet( );
  
  private NetworkRule( ) {}
  
  private NetworkRule( final Protocol protocol, final Integer lowPort, final Integer highPort,
                       final Collection<String> ipRanges,
                       final Multimap<String, String> peers ) {
    this.protocol = protocol;
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
      for ( final Entry<String, String> entry : peers.entries( ) ) {
        this.networkPeers.add( new NetworkPeer( this, entry.getKey( ), entry.getValue( ) ) );
      }
    }
  }
  
  public static NetworkRule create( Protocol protocol, final Integer lowPort, final Integer highPort,
                                    final Multimap<String, String> peers,
                                    final Collection<String> ipRanges ) {
    return new NetworkRule( protocol, lowPort, highPort, ipRanges, peers );
  }
  
  public static NetworkRule create( final String protocol, final Integer lowPort, final Integer highPort,
                                    final Multimap<String, String> peers,
                                    final Collection<String> ipRanges ) {
    return create( Protocol.valueOf( protocol ), lowPort, highPort, peers, ipRanges );
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
    int result = super.hashCode( );
    result = prime * result + ( ( this.highPort == null ) ? 0 : this.highPort.hashCode( ) );
    result = prime * result + ( ( this.ipRanges == null ) ? 0 : this.ipRanges.hashCode( ) );
    result = prime * result + ( ( this.lowPort == null ) ? 0 : this.lowPort.hashCode( ) );
    result = prime * result + ( ( this.networkPeers == null ) ? 0 : this.networkPeers.hashCode( ) );
    result = prime * result + ( ( this.protocol == null ) ? 0 : this.protocol.hashCode( ) );
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
    return true;
  }

  @Override
  public String toString( ) {
    return String.format( "NetworkRule:%s:%d:%d:ipRanges=%s:networkPeers=%s:",
                          this.protocol, this.lowPort, this.highPort, this.ipRanges, this.networkPeers );
  }
}
