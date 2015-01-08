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

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import static com.eucalyptus.upgrade.Upgrades.PreUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version;
import static com.eucalyptus.util.Parameters.checkParam;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.compute.common.CloudMetadata.NetworkGroupMetadata;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.vpc.Vpc;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Numbers;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import groovy.sql.Sql;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class NetworkGroup extends UserMetadata<NetworkGroup.State> implements NetworkGroupMetadata {
  private static final long   serialVersionUID = 1L;
  private static final Logger LOG              = Logger.getLogger( NetworkGroup.class );

  public static final String ID_PREFIX = "sg";
  
  public enum State {
    DISABLED,
    PENDING,
    AWAITING_PEER,
    ACTIVE
  }
  
  @Column( name = "metadata_network_group_id", unique = true )
  private String           groupId;
  
  @Column( name = "metadata_network_group_description" )
  private String           description;

  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_vpc", updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Vpc              vpc;

  @Column( name = "metadata_vpc_id", updatable = false )
  private String           vpcId;

  @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true )
  @JoinColumn( name = "metadata_network_group_rule_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<NetworkRule> networkRules = new HashSet<>( );

  @OneToOne( cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = true, orphanRemoval = true, mappedBy = "networkGroup" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private ExtantNetwork    extantNetwork;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "networkGroup" )
  private Collection<NetworkGroupTag> tags;

  protected NetworkGroup( ) {}
  
  protected NetworkGroup( final OwnerFullName ownerFullName ) {
    super( ownerFullName );
  }

  protected NetworkGroup( final OwnerFullName ownerFullName, final String groupName ) {
    super( ownerFullName, groupName );
  }

  protected NetworkGroup( final OwnerFullName ownerFullName,
                          final Vpc vpc,
                          final String groupId,
                          final String groupName,
                          final String groupDescription ) {
    this( ownerFullName, groupName );
    checkParam( groupDescription, notNullValue() );
    this.vpc = vpc;
    this.vpcId = CloudMetadatas.toDisplayName( ).apply( vpc );
    this.description = groupDescription;
    this.groupId = groupId;
  }

  protected NetworkGroup( final String naturalId ) {
    this.setNaturalId( naturalId );
  }

  public static NetworkGroup create( final OwnerFullName ownerFullName,
                                     final Vpc vpc,
                                     final String groupId,
                                     final String groupName,
                                     final String groupDescription ) {
    return new NetworkGroup( ownerFullName, vpc, groupId, groupName, groupDescription );
  }

  public static NetworkGroup withOwner( final OwnerFullName ownerFullName ) {
    return new NetworkGroup( ownerFullName );
  }

  public static NetworkGroup named( @Nonnull final OwnerFullName ownerFullName, final String groupName ) {
    return groupName == null ?
        withOwner( ownerFullName ) :
        withUniqueName( ownerFullName, null, groupName );
  }

  public static NetworkGroup withNaturalId( final String naturalId ) {
    return new NetworkGroup( naturalId );
  }

  public static NetworkGroup withGroupId( final OwnerFullName ownerFullName, final String groupId ) {
      return networkGroupWithGroupId(ownerFullName, groupId);
  }

  /**
   * Specify VPC identifier for names in a VPC, when VPC identifier is omitted only
   * non-VPC groups will match.
   */
  public static NetworkGroup withUniqueName( @Nonnull final OwnerFullName ownerFullName,
                                             @Nullable final String vpcId,
                                             final String groupName ) {
    final NetworkGroup networkGroup = new NetworkGroup( ownerFullName );
    networkGroup.setUniqueName( createUniqueName( ownerFullName.getAccountNumber( ), vpcId, groupName ) );
    return networkGroup;
  }

  /**
   * Example for finding a group by name in a VPC
   *
   * @see #withUniqueName(com.eucalyptus.util.OwnerFullName, String, String) withUniqueName - For use when owner is specified
   */
  public static NetworkGroup namedForVpc( final String vpcId,
                                          final String groupName ) {
    final NetworkGroup networkGroup = new NetworkGroup( null, groupName );
    networkGroup.setVpcId( vpcId );
    return networkGroup;
  }

  private static NetworkGroup networkGroupWithGroupId( final OwnerFullName ownerFullName, final String groupId ) {
      NetworkGroup findGroupWithId = new NetworkGroup(ownerFullName);
      findGroupWithId.setGroupId(groupId);
      return findGroupWithId;
  }

  private static String createUniqueName( final String accountNumber,
                                          final String vpcId,
                                          final String groupName ) {
    return vpcId == null ?
        accountNumber + ":" + groupName :
        accountNumber + "\u05c3" + vpcId + "\u05c3" + groupName; // non-ascii punctuation guarantees no collisions as group name must be ascii
  }

  @Override
  protected String createUniqueName( ) {
    return createUniqueName( getOwnerAccountNumber( ), getVpcId( ), getDisplayName( ) );
  }

  @PreRemove
  private void preRemove( ) {
    if ( this.extantNetwork != null && this.extantNetwork.teardown( ) ) {
      Entities.delete( this.extantNetwork );
      this.extantNetwork = null;
    }
  }
  
  @PrePersist
  @PreUpdate
  private void prePersist( ) {
    if ( this.getState( ) == null ) {
      this.setState( State.PENDING );
    }
    
  }

  @Nullable
  public Vpc getVpc( ) {
    return vpc;
  }

  protected void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  @Nullable
  public String getVpcId( ) {
    return vpcId;
  }

  protected void setVpcId( final String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getDescription( ) {
    return this.description;
  }
  
  public String getGroupId( ) {
    return this.groupId;
  }
  
  protected void setDescription( final String description ) {
    this.description = description;
  }
  
  public Set<NetworkRule> getNetworkRules( ) {
    return this.networkRules;
  }

  public Iterable<NetworkRule> getIngressNetworkRules( ) {
    return Iterables.filter( getNetworkRules( ), Predicates.not( NetworkRule.egress( ) ) );
  }

  public Iterable<NetworkRule> getEgressNetworkRules( ) {
    return Iterables.filter( getNetworkRules( ), NetworkRule.egress( ) );
  }

  private void setGroupId( final String groupId ){
     this.groupId = groupId;
  }
  
  private void setNetworkRules( final Set<NetworkRule> networkRules ) {
    this.networkRules = networkRules;
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
    result = prime * result + ( ( this.getUniqueName( ) == null )
      ? 0
      : this.getUniqueName( ).hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !this.getClass( ).equals( obj.getClass( ) ) ) return false;
    final NetworkGroup other = ( NetworkGroup ) obj;
    if ( this.getUniqueName( ) == null ) {
      if ( other.getUniqueName( ) != null ) return false;
    } else if ( !this.getUniqueName( ).equals( other.getUniqueName( ) ) ) return false;
    return true;
  }
  
  @Override
  public String toString( ) {
    return String.format( "NetworkRulesGroup:%s:description=%s:networkRules=%s", this.getUniqueName( ), this.description, Entities.isReadable( this.networkRules ) ? this.networkRules : "<Not loaded>" );
  }
  
  public String getClusterNetworkName( ) {
    return this.getOwnerAccountNumber( ) + "-" + this.getNaturalId( );
  }
  
  public ExtantNetwork reclaim( Integer i ) throws NotEnoughResourcesException, TransientEntityException {
    if ( !Entities.isPersistent( this ) ) {
      throw new TransientEntityException( this.toString( ) );
    } else {
      ExtantNetwork exNet = Entities.persist( ExtantNetwork.create( this, i ) );
      this.setExtantNetwork( exNet );
      return this.getExtantNetwork( );
    }
  }
  
  public ExtantNetwork extantNetwork( ) throws NotEnoughResourcesException, TransientEntityException {
    if ( !Entities.isPersistent( this ) ) {
      throw new TransientEntityException( this.toString( ) );
    } else {
      ExtantNetwork exNet = this.getExtantNetwork( );
      if ( exNet == null ) {
        for ( Integer i : Numbers.shuffled( NetworkGroups.networkTagInterval( ) ) ) {
          try {
            Entities.uniqueResult( ExtantNetwork.named( i ) );
            continue;
          } catch ( Exception ex ) {
            exNet = ExtantNetwork.create( this, i );
            Entities.persist( exNet );
            this.setExtantNetwork( exNet );
            return this.getExtantNetwork( );
          }
        }
        throw new NotEnoughResourcesException( "Failed to allocate network tag for network: " + this.getFullName( ) + ": no network tags are free." );
      } else {
        return this.getExtantNetwork( );
      }
    }
  }
  
  ExtantNetwork getExtantNetwork( ) {
    return this.extantNetwork;
  }
  
  void setExtantNetwork( final ExtantNetwork extantNetwork ) {
    this.extantNetwork = extantNetwork;
  }
  
  public boolean hasExtantNetwork( ) {
    return this.extantNetwork != null;
  }

  @EntityUpgrade(entities = { NetworkGroup.class },  since = Version.v3_3_0, value = Eucalyptus.class)
  public enum NetworkGroupUpgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( NetworkGroup.NetworkGroupUpgrade.class );
    @Override
    public boolean apply( Class arg0 ) {
      addSecurityGroupIdentifiers( );
      addSecurityGroupIdentifiersToRules( );
      return true;
    }

    private void addSecurityGroupIdentifiers( ) {
      final EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        final List<NetworkGroup> networkGroupList = Entities.query( new NetworkGroup( ) );
        final Set<String> generatedIdentifiers = // ensure identifiers and names do not collide
            Sets.newHashSet( Iterables.transform( networkGroupList, RestrictedTypes.toDisplayName() ) );
        for ( final NetworkGroup networkGroup : networkGroupList ) {
          LOG.debug( "Upgrading " + networkGroup.getDisplayName( ) );
          if ( networkGroup.getGroupId( ) == null ) {
            String networkGroupId = null;
            while ( networkGroupId == null || generatedIdentifiers.contains( networkGroupId ) ) {
              networkGroupId = ResourceIdentifiers.generateString( ID_PREFIX );
            }
            generatedIdentifiers.add( networkGroupId );
            networkGroup.setGroupId( networkGroupId );
          }
        }
        db.commit();
      } catch (Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      } finally {
        if ( db.isActive() ) db.rollback();
      }
    }

    private void addSecurityGroupIdentifiersToRules( ) {
      final EntityTransaction db = Entities.get( NetworkRule.class );
      try {
        final List<NetworkRule> networkRuleList = Entities.query( NetworkRule.named() );
        for ( final NetworkRule networkRule : networkRuleList ) {
          LOG.debug( "Upgrading " + networkRule );
          if ( networkRule.getNetworkPeers() != null && networkRule.getNetworkPeers().size() > 0 ) {
            final Set<NetworkPeer> updatedPeers = Sets.newHashSet();
            for ( final NetworkPeer networkPeer : networkRule.getNetworkPeers() ) {
              if ( networkPeer.getGroupId() == null ) {
                // find the corresponding network group from network groups
                String groupId = null;
                try {
                  if ( Accounts.getAccountProvider() == null ) {
                    DatabaseAuthProvider dbAuth = new DatabaseAuthProvider();
                    Accounts.setAccountProvider( dbAuth );
                  }
                  final NetworkGroup networkGroup =
                      NetworkGroups.lookup( AccountFullName.getInstance( networkPeer.getUserQueryKey() ), networkPeer.getGroupName() );
                  groupId = networkGroup.getGroupId();
                } catch ( final NoSuchMetadataException ex ) {
                  LOG.error( String.format( "unable to find the network group (%s-%s)", networkPeer.getUserQueryKey(), networkPeer.getGroupName() ) );
                } catch ( final Exception ex ) {
                  LOG.error( "failed to query network group", ex );
                }
                if ( groupId != null ) {
                  networkPeer.setGroupId( groupId );
                  LOG.debug( "network peer upgraded: " + networkPeer );
                }
              }
              updatedPeers.add( networkPeer );
            }

            networkRule.getNetworkPeers().clear();
            networkRule.setNetworkPeers( updatedPeers );
          }
        }

        db.commit();
      } catch (Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      } finally {
        if ( db.isActive() ) db.rollback();
      }
    }
  }
  
  @PreUpgrade( value = Eucalyptus.class, since = Version.v4_1_0 ) // originally v3_4_0
  public static class NetworkGroupPreUpgrade34 implements Callable<Boolean> {
    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
        sql.execute( "alter table metadata_network_group drop column if exists vm_network_index" );
        return true;
      } catch ( Exception ex ) {
        LOG.error( "Error deleting column vm_network_index for metadata_network_group", ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
