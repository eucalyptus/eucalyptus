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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.NetworkSecurityGroup;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.FullName;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;

@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkRulesGroup extends UserMetadata<NetworkRulesGroup.State> implements NetworkSecurityGroup {
  enum State { available, removing }
  @Column( name = "metadata_network_group_user_network_group_name", unique = true )
  private String            uniqueName;                                          //bogus field to enforce uniqueness
  @Column( name = "metadata_network_group_description" )
  private String            description;
  @OneToMany( cascade = { CascadeType.ALL }, fetch = FetchType.EAGER )
  @JoinTable( name = "metadata_network_group_has_rules", joinColumns = { @JoinColumn( name = "id" ) }, inverseJoinColumns = { @JoinColumn( name = "metadata_network_rule_id" ) } )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<NetworkRule> networkRules         = new ArrayList<NetworkRule>( );
  @Transient
  private FullName          fullName;
  public static String      NETWORK_DEFAULT_NAME = "default";
  
  public static NetworkRulesGroup named( UserFullName userFullName, String groupName ) {
    return new NetworkRulesGroup( userFullName, groupName );
  }
  
  public NetworkRulesGroup( ) {}
  
  public NetworkRulesGroup( final UserFullName userFullName ) {
    super( userFullName );
  }
  
  public NetworkRulesGroup( final UserFullName userFullName, final String groupName ) {
    super( userFullName, groupName );
    this.fullName = FullName.create.vendor( "euca" )
                                   .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                                   .namespace( this.getOwnerAccountId( ) )
                                   .relativeId( "security-group", this.getDisplayName( ) );
    this.uniqueName = this.fullName.toString( );
    this.setState( State.available );
  }
  
  public NetworkRulesGroup( final UserFullName userFullName, final String groupName, final String groupDescription ) {
    this( userFullName, groupName );
    Assertions.assertNotNull( groupDescription );
    this.description = groupDescription;
  }
  
  public NetworkRulesGroup( final UserFullName userFullName, final String groupName, final String description, final List<NetworkRule> networkRules ) {
    this( userFullName, groupName, description );
    Assertions.assertNotNull( networkRules );
    this.networkRules = networkRules;
  }
  
  @Deprecated
  public String getUniqueName( ) {
    return this.uniqueName;
  }
  
  public void setUniqueName( String uniqueName ) {
    this.uniqueName = uniqueName;
  }
  
  public String getDescription( ) {
    return this.description;
  }
  
  public void setDescription( String description ) {
    this.description = description;
  }
  
  public List<NetworkRule> getNetworkRules( ) {
    return this.networkRules;
  }
  
  public void setNetworkRules( List<NetworkRule> networkRules ) {
    this.networkRules = networkRules;
  }
  
  public static NetworkRulesGroup getDefaultGroup( UserFullName userFullName ) {
    return new NetworkRulesGroup( userFullName, NETWORK_DEFAULT_NAME, "default group", new ArrayList<NetworkRule>( ) );
  }
  
  public Network getVmNetwork( ) {
    List<PacketFilterRule> pfRules = Lists.transform( this.getNetworkRules( ), this.ruleTransform );
    Network vmNetwork = new Network( UserFullName.getInstance( this.getOwnerUserId( ) ), this.getDisplayName( ), this.getId( )/**TODO:GRZE:this is surely wrong**/, pfRules );
    return vmNetwork;
  }
  
  @Override
  public String getPartition( ) {
    return ComponentIds.lookup( Eucalyptus.class ).name( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.fullName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( uniqueName == null )
      ? 0
      : uniqueName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    NetworkRulesGroup other = ( NetworkRulesGroup ) obj;
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
                                                                                                                          NetworkRulesGroup.this.getOwnerAccountId( ),
                                                                                                                          NetworkRulesGroup.this.getDisplayName( ),
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
  public int compareTo( NetworkSecurityGroup that ) {
    return this.getFullName( ).toString( ).compareTo( that.getFullName( ).toString( ) );
  }

  public String getClusterNetworkName( ) {
    return this.getOwnerUserId( ) + "-" + this.getDisplayName( );
  }
  
}
