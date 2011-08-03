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
import static org.hamcrest.Matchers.notNullValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.cloud.CloudMetadata.NetworkSecurityGroup;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkGroup extends UserMetadata<NetworkGroup.State> implements NetworkSecurityGroup<NetworkGroup> {
  enum State {
    DISABLED,
    AWAITING_PEER,
    PENDING,
    ACTIVE
  }
  
  @Column( name = "metadata_network_group_unique_name", unique = true )
  private String                   uniqueName;                                                 //bogus field to enforce uniqueness
  
  @Column( name = "metadata_network_group_description" )
  private String                   description;
  
  @OneToMany( cascade = { CascadeType.ALL } )
  @Fetch( FetchMode.JOIN )
  @JoinTable( name = "metadata_network_group_has_rules", joinColumns = { @JoinColumn( name = "id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_network_rule_id" ) } )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkRule>         networkRules         = new HashSet<NetworkRule>( );

  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "user" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<PrivateNetworkIndex> indexes              = new HashSet<PrivateNetworkIndex>( );
  @Transient
  public static String             NETWORK_DEFAULT_NAME = "default";
  
  NetworkGroup( ) {}
  
  NetworkGroup( final OwnerFullName ownerFullName ) {
    super( ownerFullName );
  }
  
  NetworkGroup( final OwnerFullName ownerFullName, final String groupName ) {
    super( ownerFullName, groupName );
  }
  
  NetworkGroup( final OwnerFullName ownerFullName, final String groupName, final String groupDescription ) {
    this( ownerFullName, groupName );
    assertThat( groupDescription, notNullValue( ) );
    this.description = groupDescription;
  }
  
  @PrePersist
  @PreUpdate
  private void prePersist( ) {
    if ( this.getState( ) == null ) {
      this.setState( State.PENDING );
    }
  }
  
  public String getDescription( ) {
    return this.description;
  }
  
  protected void setDescription( String description ) {
    this.description = description;
  }
  
  public Set<NetworkRule> getNetworkRules( ) {
    return this.networkRules;
  }
  
  private void setNetworkRules( Set<NetworkRule> networkRules ) {
    this.networkRules = networkRules;
  }
  
  //GRZE:NET
//  public Network getVmNetwork( ) {
//    Network asNet;
//    try {
//      asNet = Networks.getInstance( ).lookup( this );
//    } catch ( Exception ex ) {
//      Network vmNetwork = new Network( this );
//      asNet = Networks.getInstance( ).register( vmNetwork, Networks.State.ACTIVE );
//    }
//    return asNet;
//  }
  
  public Collection<PacketFilterRule> lookupPacketFilterRules( ) {
    Collection<PacketFilterRule> pfRules = Collections2.transform( this.getNetworkRules( ), this.ruleTransform );
    return pfRules;
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "security-group", this.getDisplayName( ) );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.uniqueName == null )
      ? 0
      : this.uniqueName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    NetworkGroup other = ( NetworkGroup ) obj;
    if ( uniqueName == null ) {
      if ( other.uniqueName != null ) return false;
    } else if ( !uniqueName.equals( other.uniqueName ) ) return false;
    return true;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkRulesGroup:%s:description=%s:networkRules=%s", this.uniqueName, this.description, this.networkRules );
  }
  
  @Transient
  private final Function<NetworkRule, PacketFilterRule> ruleTransform = new Function<NetworkRule, PacketFilterRule>( ) {
                                                                        
                                                                        @Override
                                                                        public PacketFilterRule apply( NetworkRule from ) {
                                                                          PacketFilterRule pfrule = new PacketFilterRule(
                                                                                                                          NetworkGroup.this.getOwnerAccountNumber( ),
                                                                                                                          NetworkGroup.this.getDisplayName( ),
                                                                                                                          from.getProtocol( ),
                                                                                                                          from.getLowPort( ),
                                                                                                                          from.getHighPort( ) );
                                                                          for ( IpRange cidr : from.getIpRanges( ) )
                                                                            pfrule.getSourceCidrs( ).add( cidr.getValue( ) );
                                                                          for ( NetworkPeer peer : from.getNetworkPeers( ) )
                                                                            pfrule.addPeer( peer.getUserQueryKey( ), peer.getGroupName( ) );
                                                                          return pfrule;
                                                                        }
                                                                      };
  
  @Override
  public int compareTo( NetworkGroup that ) {
    return this.getUniqueName( ).compareTo( that.getUniqueName( ) );
  }
  
  public String getClusterNetworkName( ) {
    return this.getOwnerUserId( ) + "-" + this.getDisplayName( );
  }
  
  public void setUniqueName( String uniqueName ) {
    this.uniqueName = uniqueName;
  }
  
  public String getUniqueName( ) {
    return this.uniqueName;
  }
  
  public String getPermanentUuid( ) {
    return this.permanentUuid;
  }
  
  /**
   * GRZE:TODO: eliminate these symbols
   */
  public void returnNetworkIndex( Integer net ) {}
  
  public Integer allocateNetworkIndex( String cluster ) {
    return null;
  }
  
}
