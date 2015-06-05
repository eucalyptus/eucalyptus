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

  /**
   * Set of all supported protocols per: http://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
   * To enable a protocol to be supported in EC2Classic (vpc only is the default), then pass 'false' to the constructor
   *
   * An example currently is sctp, which we support in EDGE (euca-10031) as an exception to AWS compatibility
   */
  public enum Protocol {
    hopopt(0),
    icmp(1, false){
      @Override public Integer extractLowPort ( final NetworkRule rule ) { return null; }
      @Override public Integer extractHighPort( final NetworkRule rule ) { return null; }
      @Override public Integer extractIcmpType( final NetworkRule rule ) { return rule.getLowPort( ); }
      @Override public Integer extractIcmpCode( final NetworkRule rule ) { return rule.getHighPort( ); }

      @Override
      public String getUserFacingName() {
        return this.name();
      }
    },
    igmp(2),
    ggp(3),
    ipv4(4),
    st(5),
    tcp(6, false) {
      @Override
      public String getUserFacingName() {
        return this.name();
      }
    },
    cbt(7),
    egp(8),
    igp(9),
    bnn_rcc_mon(10),
    nvp_ii(11),
    pup(12),
    argus(13),
    emcon(14),
    xnet(15),
    chaos(16),
    udp(17, false) {
      @Override
      public String getUserFacingName() {
        return this.name();
      }
    },
    mux(18),
    dcn_meas(19),
    hmp(20),
    prm(21),
    xns_idp(22),
    trunk_1(23),
    trunk_2(24),
    leaf_1(25),
    leaf_2(26),
    rdp(27),
    irtp(28),
    iso_tp4(29),
    netblt(30),
    mfe_nsp(31),
    merit_inp(32),
    dccp(33),
    three_pc(34),
    idpr(35),
    xtp(36),
    ddp(37),
    idpr_cmtp(38),
    tp_plusplus(39),
    il(40),
    ipv6(41),
    sdrp(42),
    ipv6_route(43),
    ipv6_frag(44),
    idrp(45),
    rsvp(46),
    gre(47),
    dsr(48),
    bna(49),
    esp(50),
    ah(51),
    i_nlsp(52),
    swipe(53),
    narp(54),
    mobile(55),
    tlsp(56),
    skip(57),
    ipv6_icmp(58),
    ipv6_nonxt(59),
    ipv6_opts(60),
    any_host_internal(61),
    cftp(62),
    any_local_net(63),
    sat_expak(64),
    kyrptolan(65),
    rvd(66),
    ippc(67),
    any_distributed_filesystem(68),
    sat_mon(69),
    visa(70),
    ipcv(71),
    cpnx(72),
    cphb(73),
    wsn(74),
    pvp(75),
    br_sat_mon(76),
    sun_nd(77),
    wb_mon(78),
    wb_expak(79),
    iso_ip(80),
    vmtp(81),
    secure_vmtp(82),
    vines(83),
    ttp(84),
    iptn(84),
    nsfnet_igp(85),
    dgp(86),
    tcf(87),
    eigrp(88),
    ospfigp(89),
    sprite_rpc(90),
    larp(91),
    ntp(92),
    ax_25(93),
    ipip(94),
    micp(95),
    scc_sp(96),
    etherip(97),
    encap(98),
    any_private_encryption(99),
    gmtp(100),
    ifmp(101),
    pnni(102),
    pim(103),
    aris(104),
    scps(105),
    qnx(106),
    a_n(107),
    ipcomp(108),
    snp(109),
    compaq_peer(110),
    ipx_in_ip(111),
    vrrp(112),
    pgm(113),
    any_0hop(114),
    l2tp(115),
    ddx(116),
    iatp(117),
    stp(118),
    srp(119),
    uti(120),
    smp(121),
    sm(122),
    ptp(123),
    isis(124),
    fire(125),
    crtp(126),
    crudp(127),
    sscopmce(128),
    iplt(129),
    sps(130),
    pipe(131),
    sctp(132, false),
    fc(133),
    rsvp_e2e_ignore(134),
    nobility(135),
    udplite(136),
    mpls_in_ip(137),
    manet(138),
    hip(139),
    shim6(140),
    wesp(141),
    rohc(142),
    experimental1(253),
    experimental2(254)
        ;
    /*
    143-252		Unassigned		[Internet_Assigned_Numbers_Authority]
    255	Reserved			[Internet_Assigned_Numbers_Authority]
        */


    private final int number;
    private final boolean vpcModeOnly; //Is this protocol allowed in EC2-Classic mode (EDGE/MANAGED*)

    private Protocol( int number, boolean vpcOnly) {
      this.number = number;
      this.vpcModeOnly = vpcOnly;
    }

    private Protocol( int number) {
      this.number = number;
      this.vpcModeOnly = true;
    }

    public int getNumber( ) {
      return number;
    }

    public boolean isVpcOnly() {
      return this.vpcModeOnly;
    }

    public String getUserFacingName() {
      return String.valueOf(this.number);
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
    Protocol p = parseProtocol(protocolText, vpc);
    return create( p , p.getNumber(), lowPort, highPort, peers, ipRanges );
  }

  protected static Protocol parseProtocol(
      final String protocolText,
      final boolean vpc
  ) {
    Protocol protocol = Protocol.fromString( protocolText );

    //Ensure that VPC-only protocols aren't allowed
    if(!vpc && protocol.isVpcOnly()) {
      throw new IllegalArgumentException("Protocol '" + protocolText + "' not supported in EC2-Classic compatible mode");
    }
    return protocol;
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
        protocol.getUserFacingName() :
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
    return getProtocol() == null || getProtocol().isVpcOnly();
    //return getLowPort() == null || getHighPort() == null || getProtocol() == null;
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
