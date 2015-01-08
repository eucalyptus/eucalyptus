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

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.cloud.util.MetadataConstraintException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.Reference;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.common.network.NetworkReportType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.records.Logs;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType;

@ConfigurableClass( root = "cloud.network",
                    description = "Default values used to bootstrap networking state discovery." )
public class NetworkGroups {
  private static final String DEFAULT_NETWORK_NAME          = "default";
  private static Logger       LOG                           = Logger.getLogger( NetworkGroups.class );
  private static String       NETWORK_DEFAULT_NAME          = "default";
  
  @ConfigurableField( description = "Default max network index." )
  public static Long          GLOBAL_MAX_NETWORK_INDEX      = 4096l;
  @ConfigurableField( description = "Default min network index." )
  public static Long          GLOBAL_MIN_NETWORK_INDEX      = 2l;
  @ConfigurableField( description = "Default max vlan tag." )
  public static Integer       GLOBAL_MAX_NETWORK_TAG        = 4096;
  @ConfigurableField( description = "Default min vlan tag." )
  public static Integer       GLOBAL_MIN_NETWORK_TAG        = 1;
  @ConfigurableField( description = "Minutes before a pending tag allocation timesout and is released." )
  public static Integer       NETWORK_TAG_PENDING_TIMEOUT   = 35;
  @ConfigurableField( description = "Minutes before a pending index allocation timesout and is released." )
  public static Integer       NETWORK_INDEX_PENDING_TIMEOUT = 35;
  @ConfigurableField(
      description = "Network configuration document.",
      changeListener = NetworkConfigurations.NetworkConfigurationPropertyChangeListener.class )
  public static String        NETWORK_CONFIGURATION = "";
  @ConfigurableField( description = "Minimum interval between broadcasts of network information (seconds)." )
  public static Integer       MIN_BROADCAST_INTERVAL = 5;


  public static class NetworkRangeConfiguration {
    private Boolean useNetworkTags  = Boolean.TRUE;
    private Integer minNetworkTag   = GLOBAL_MIN_NETWORK_TAG;
    private Integer maxNetworkTag   = GLOBAL_MAX_NETWORK_TAG;
    private Long    minNetworkIndex = GLOBAL_MIN_NETWORK_INDEX;
    private Long    maxNetworkIndex = GLOBAL_MAX_NETWORK_INDEX;
    
    Boolean hasNetworking( ) {
      return this.useNetworkTags;
    }
    
    Boolean getUseNetworkTags( ) {
      return this.useNetworkTags;
    }
    
    void setUseNetworkTags( final Boolean useNetworkTags ) {
      this.useNetworkTags = useNetworkTags;
    }
    
    public Integer getMinNetworkTag( ) {
      return this.minNetworkTag;
    }
    
    public void setMinNetworkTag( final Integer minNetworkTag ) {
      
      this.minNetworkTag = minNetworkTag;
    }
    
    public Integer getMaxNetworkTag( ) {
      return this.maxNetworkTag;
    }
    
    public void setMaxNetworkTag( final Integer maxNetworkTag ) {
      this.maxNetworkTag = maxNetworkTag;
    }
    
    public Long getMaxNetworkIndex( ) {
      return this.maxNetworkIndex;
    }
    
    public void setMaxNetworkIndex( final Long maxNetworkIndex ) {
      this.maxNetworkIndex = maxNetworkIndex;
    }
    
    public Long getMinNetworkIndex( ) {
      return this.minNetworkIndex;
    }
    
    public void setMinNetworkIndex( final Long minNetworkIndex ) {
      this.minNetworkIndex = minNetworkIndex;
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      builder.append( "NetworkRangeConfiguration:" );
      if ( this.useNetworkTags != null ) builder.append( "useNetworkTags=" ).append( this.useNetworkTags ).append( ":" );
      if ( this.minNetworkTag != null ) builder.append( "minNetworkTag=" ).append( this.minNetworkTag ).append( ":" );
      if ( this.maxNetworkTag != null ) builder.append( "maxNetworkTag=" ).append( this.maxNetworkTag ).append( ":" );
      if ( this.minNetworkIndex != null ) builder.append( "minNetworkIndex=" ).append( this.minNetworkIndex ).append( ":" );
      if ( this.maxNetworkIndex != null ) builder.append( "maxNetworkIndex=" ).append( this.maxNetworkIndex );
      return builder.toString( );
    }
    
  }  
  private enum ActiveTags implements Function<NetworkReportType, Integer> {
    INSTANCE;
    private final SetMultimap<String, Integer> backingMap            = HashMultimap.create( );
    private final SetMultimap<String, Integer> activeTagsByPartition = Multimaps.synchronizedSetMultimap( backingMap );
    
    public Integer apply( final NetworkReportType input ) {
      return input.getTag( );
    }
    
    /**
     * Update the cache of currently active network tags based on the most recent reported value.
     * 
     * @param cluster
     * @param activeNetworks
     */
    private void update( ServiceConfiguration cluster, List<NetworkReportType> activeNetworks ) {
      removeStalePartitions( );
      Set<Integer> activeTags = Sets.newHashSet( Lists.transform( activeNetworks, ActiveTags.INSTANCE ) );
      this.activeTagsByPartition.replaceValues( cluster.getPartition( ), activeTags );
    }
    
    /**
     * Update the {@link #partitionActiveNetworkTags} map by removing any {@code key} values which
     * no longer have a corresponding service registration.
     * 
     * @throws PersistenceException
     */
    private void removeStalePartitions( ) throws PersistenceException {
      Set<String> partitions = Sets.newHashSet( );
      for ( ServiceConfiguration cc : ServiceConfigurations.list( ClusterController.class ) ) {
        partitions.add( cc.getPartition( ) );
      }
      for ( String stalePartition : Sets.difference( this.activeTagsByPartition.keySet( ), partitions ) ) {
        this.activeTagsByPartition.removeAll( stalePartition );
      }
    }
    
    /**
     * Returns true if the collection of most recently reported active network tags across the whole
     * system included the argument {@code tag}.
     * 
     * @param tag
     * @return true if {@code tag} is active
     */
    private boolean isActive( Integer tag ) {
      return this.activeTagsByPartition.containsValue( tag );
    }
  }

  private enum NetworkIndexTransform implements Function<NetworkReportType, List<String>> {
    INSTANCE;

    @Override
    public List<String> apply( @Nullable final NetworkReportType networkReportType ) {
      final List<String> taggedIndices = Lists.newArrayList( );
      if ( networkReportType != null && networkReportType.getAllocatedIndexes( ) != null ) {
        for ( String index : networkReportType.getAllocatedIndexes( ) ) {
          taggedIndices.add( networkReportType.getTag() + ":" + index );
        }
      }
      return taggedIndices;
    }
  }

  /**
   * Update network tag information by marking reported tags as being EXTANT and removing previously
   * EXTANT tags which are no longer reported
   * 
   * @param activeNetworks
   */
  public static void updateExtantNetworks( ServiceConfiguration cluster, List<NetworkReportType> activeNetworks ) {
    ActiveTags.INSTANCE.update( cluster, activeNetworks );
    /**
     * For each of the reported active network tags ensure that the locally stored extant network
     * state reflects that the network has now been EXTANT in the system (i.e. is no longer PENDING)
     */
    for ( NetworkReportType activeNetReport : activeNetworks ) {
      EntityTransaction tx = Entities.get( NetworkGroup.class );
      try {
        NetworkGroup net = NetworkGroups.lookupByNaturalId( activeNetReport.getUuid() );
        if ( net.hasExtantNetwork( ) ) {
          ExtantNetwork exNet = net.extantNetwork( );
          if ( Reference.State.PENDING.equals( exNet.getState( ) ) ) {
            LOG.debug( "Found PENDING extant network for " + net.getFullName( ) + " updating to EXTANT." );
            exNet.setState( Reference.State.EXTANT );
          } else {
            LOG.debug( "Found " + exNet.getState( ) + " extant network for " + net.getFullName( ) + ": skipped." );
          }
        } else {
          LOG.warn( "Failed to find extant network for " + net.getFullName( ) );//TODO:GRZE: likely we should be trying to reclaim tag here
        }
        tx.commit( );
      } catch ( Exception ex ) {
        LOG.debug( ex );
        Logs.extreme( ).error( ex, ex );
      } finally {
        if ( tx.isActive( ) ) tx.rollback( );
      }
    }
    /**
     * For each defined network group check to see if the extant network is in the set of active
     * tags and remove it if appropriate.
     * 
     * It is appropriate to remove the network when the state of the extant network is
     * {@link Reference.State.RELEASING}.
     * 
     * Otherwise, if {@link ActiveTags#INSTANCE#isActive()} is false and:
     * <ol>
     * <li>The state of the extant network is {@link Reference.State.EXTANT}
     * <li>The state of the extant network is {@link Reference.State.PENDING} and has exceeded
     * {@link NetworksGroups#NETWORK_TAG_PENDING_TIMEOUT}
     * </ol>
     * Then the state of the extant network is updated to {@link Reference.State.RELEASING}.
     */    
    try {
      final List<NetworkGroup> groups = NetworkGroups.lookupAll( null, null );
      for ( NetworkGroup net : groups ) {
        final EntityTransaction tx = Entities.get( NetworkGroup.class );
        try {
          net = Entities.merge( net );
          if ( net.hasExtantNetwork( ) ) {
            ExtantNetwork exNet = net.getExtantNetwork( );
            Integer exNetTag = exNet.getTag( );
            if ( !ActiveTags.INSTANCE.isActive( exNetTag ) ) {
              if ( Reference.State.EXTANT.equals( exNet.getState( ) ) ) {
                exNet.setState( Reference.State.RELEASING );
              } else if ( Reference.State.PENDING.equals( exNet.getState( ) )
                              && isTimedOut( exNet.lastUpdateMillis( ), NetworkGroups.NETWORK_TAG_PENDING_TIMEOUT ) ) {
                exNet.setState( Reference.State.RELEASING );
              } else if ( Reference.State.RELEASING.equals( exNet.getState( ) ) ) {
                exNet.teardown( );
                Entities.delete( exNet );
                net.setExtantNetwork( null );
              }
            }
          }          
          tx.commit( );
        } catch ( final Exception ex ) {
          LOG.debug( ex );
          Logs.extreme( ).error( ex, ex );
        } finally {
          if ( tx.isActive( ) ) tx.rollback( );
        }
      }
    } catch ( MetadataException ex ) {
      LOG.error( ex );
    }

    // Time out pending network indexes that are not reported
    final Set<String> taggedNetworkIndicies = Sets.newHashSet( CollectionUtils.<String>listJoin()
        .apply( Lists.transform( activeNetworks, NetworkIndexTransform.INSTANCE ) ) );
    try ( final TransactionResource db = Entities.transactionFor( PrivateNetworkIndex.class  ) ) {
      for ( final PrivateNetworkIndex index :
          Entities.query( PrivateNetworkIndex.inState( Reference.State.PENDING ), Entities.queryOptions().build() ) ) {
        if ( isTimedOut( index.lastUpdateMillis(), NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
          if ( taggedNetworkIndicies.contains( index.getDisplayName( ) ) ) {
            LOG.warn( String.format( "Pending network index (%s) timed out, setting state to EXTANT", index.getDisplayName( ) ) );
            index.setState( Reference.State.EXTANT );
          } else {
            LOG.warn( String.format( "Pending network index (%s) timed out, tearing down", index.getDisplayName( ) ) );
            index.release( );
            index.teardown( );
          }
        }
      }
      db.commit();
    } catch ( final Exception ex ) {
      LOG.debug( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }

  private static boolean isTimedOut( Long timeSinceUpdateMillis, Integer timeoutMinutes ) {
    return
        timeSinceUpdateMillis != null &&
        timeoutMinutes != null &&
        ( timeSinceUpdateMillis > TimeUnit.MINUTES.toMillis( timeoutMinutes )  );
  }

  static NetworkRangeConfiguration netConfig = new NetworkRangeConfiguration( );
  
  public static synchronized void updateNetworkRangeConfiguration( ) {
    final AtomicReference<NetworkRangeConfiguration> equalityCheck = new AtomicReference<NetworkRangeConfiguration>( null );
    try {
      Transactions.each( new ClusterConfiguration( ), new Callback<ClusterConfiguration>( ) {
        
        @Override
        public void fire( final ClusterConfiguration input ) {
          NetworkRangeConfiguration comparisonConfig = new NetworkRangeConfiguration( );
          comparisonConfig.setUseNetworkTags( input.getUseNetworkTags( ) );
          comparisonConfig.setMinNetworkTag( input.getMinNetworkTag( ) );
          comparisonConfig.setMaxNetworkTag( input.getMaxNetworkTag( ) );
          comparisonConfig.setMinNetworkIndex( input.getMinNetworkIndex( ) );
          comparisonConfig.setMaxNetworkIndex( input.getMaxNetworkIndex( ) );
          Logs.extreme( ).debug( "Updating cluster config: " + input.getName( ) + " " + comparisonConfig.toString( ) );
          if ( equalityCheck.compareAndSet( null, comparisonConfig ) ) {
            Logs.extreme( ).debug( "Initialized cluster config check: " + equalityCheck.get( ) );
          } else {
            NetworkRangeConfiguration currentConfig = equalityCheck.get( );
            List<String> errors = Lists.newArrayList( );
            if ( !currentConfig.getUseNetworkTags( ).equals( comparisonConfig.getUseNetworkTags( ) ) ) {
              errors.add( input.getName( ) + " network config mismatch: vlan tagging  " + currentConfig.getUseNetworkTags( ) + " != " + comparisonConfig.getUseNetworkTags( ) );
            } else if ( !currentConfig.getMinNetworkTag( ).equals( comparisonConfig.getMinNetworkTag( ) ) ) {
              errors.add( input.getName( ) + " network config mismatch: min vlan tag " + currentConfig.getMinNetworkTag( ) + " != " + comparisonConfig.getMinNetworkTag( ) );
            } else if ( !currentConfig.getMaxNetworkTag( ).equals( comparisonConfig.getMaxNetworkTag( ) ) ) {
              errors.add( input.getName( ) + " network config mismatch: max vlan tag " + currentConfig.getMaxNetworkTag( ) + " != " + comparisonConfig.getMaxNetworkTag( ) );
            } else if ( !currentConfig.getMinNetworkIndex( ).equals( comparisonConfig.getMinNetworkIndex( ) ) ) {
              errors.add( input.getName( ) + " network config mismatch: min net index " + currentConfig.getMinNetworkIndex( ) + " != " + comparisonConfig.getMinNetworkIndex( ) );
            } else if ( !currentConfig.getMaxNetworkIndex( ).equals( comparisonConfig.getMaxNetworkIndex( ) ) ) {
              errors.add( input.getName( ) + " network config mismatch: max net index " + currentConfig.getMaxNetworkIndex( ) + " != " + comparisonConfig.getMaxNetworkIndex( ) );
            }
          }
        }
      } );
    } catch ( RuntimeException ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    } catch ( TransactionException ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }

    netConfig = new NetworkRangeConfiguration( );
    final AtomicBoolean netTagging = new AtomicBoolean( true );
    try {
      Transactions.each( new ClusterConfiguration( ), new Callback<ClusterConfiguration>( ) {
        
        @Override
        public void fire( final ClusterConfiguration input ) {
          netTagging.compareAndSet( true, input.getUseNetworkTags( ) );
          
          netConfig.setMinNetworkTag( Ints.max( netConfig.getMinNetworkTag( ), input.getMinNetworkTag( ) ) );
          netConfig.setMaxNetworkTag( Ints.min( netConfig.getMaxNetworkTag( ), input.getMaxNetworkTag( ) ) );
          
          netConfig.setMinNetworkIndex( Longs.max( netConfig.getMinNetworkIndex( ), input.getMinNetworkIndex( ) ) );
          netConfig.setMaxNetworkIndex( Longs.min( netConfig.getMaxNetworkIndex( ), input.getMaxNetworkIndex( ) ) );
          
        }
      } );
      Logs.extreme( ).debug( "Updated network configuration: " + netConfig.toString( ) );
    } catch ( final TransactionException ex ) {
      Logs.extreme( ).error( ex, ex );
    }
    netConfig.setUseNetworkTags( netTagging.get( ) );
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final List<NetworkGroup> ret = Entities.query( new NetworkGroup( ) );
      for ( NetworkGroup group : ret ) {
        ExtantNetwork exNet = group.getExtantNetwork( );
        if ( exNet != null && ( exNet.getTag( ) > netConfig.getMaxNetworkTag( ) || exNet.getTag( ) < netConfig.getMinNetworkTag( ) ) ) {
          exNet.teardown( );
          Entities.delete( exNet );
          group.setExtantNetwork( null );
        }
      }
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      LOG.error( ex );
      db.rollback( );
    }
  }
  
  public static List<Long> networkIndexInterval( ) {
    final List<Long> interval = Lists.newArrayList( );
    for ( Long i = NetworkGroups.networkingConfiguration( ).getMinNetworkIndex( ); i < NetworkGroups.networkingConfiguration( ).getMaxNetworkIndex( ); i++ ) {
      interval.add( i );
    }
    return interval;
  }
  
  public static List<Integer> networkTagInterval( ) {
    final List<Integer> interval = Lists.newArrayList( );
    for ( Integer i = NetworkGroups.networkingConfiguration( ).getMinNetworkTag( ); i < NetworkGroups.networkingConfiguration( ).getMaxNetworkTag( ); i++ ) {
      interval.add( i );
    }
    return interval;
  }
  
  public static synchronized NetworkRangeConfiguration networkingConfiguration( ) {
    return netConfig;
  }
  
  public static NetworkGroup delete( final String groupId ) throws MetadataException {
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final NetworkGroup ret = Entities.uniqueResult( NetworkGroup.withGroupId( null, groupId ) );
      Entities.delete( ret );
      db.commit( );
      return ret;
    } catch ( final ConstraintViolationException ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new MetadataConstraintException( "Failed to delete security group: " + groupId + " because of: "
                                                + Exceptions.causeString( ex ), ex );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new NoSuchMetadataException( "Failed to find security group: " + groupId, ex );
    } finally {
      if ( db.isActive( ) ) db.rollback( );
    }
  }
  
  public static NetworkGroup lookupByNaturalId( final String uuid ) throws NoSuchMetadataException {
    EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      NetworkGroup entity = Entities.uniqueResult( NetworkGroup.withNaturalId( uuid ) );
      db.commit( );
      return entity;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find security group: " + uuid, ex );
    }
  }
  
  public static NetworkGroup lookupByGroupId( final String groupId ) throws NoSuchMetadataException {
    return lookupByGroupId( null, groupId );
  }

  public static NetworkGroup lookupByGroupId( @Nullable final OwnerFullName ownerFullName,
                                              final String groupId ) throws NoSuchMetadataException {
      EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        NetworkGroup entity = Entities.uniqueResult( NetworkGroup.withGroupId(ownerFullName, groupId) );
        db.commit( );
        return entity;
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
        throw new NoSuchMetadataException( "Failed to find security group: " + groupId, ex );
      }
    }
  
  public static NetworkGroup lookup( final OwnerFullName ownerFullName, final String groupName ) throws MetadataException {
    if ( defaultNetworkName( ).equals( groupName ) ) {
      createDefault( ownerFullName );
    }
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      NetworkGroup ret = Entities.uniqueResult( new NetworkGroup( ownerFullName, groupName ) );
      db.commit( );
      return ret;
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find security group: " + groupName + " for " + ownerFullName, ex );
    }
  }
  
  public static List<NetworkGroup> lookupAll( final OwnerFullName ownerFullName, final String groupNamePattern ) throws MetadataException {
    if ( defaultNetworkName( ).equals( groupNamePattern ) ) {
      createDefault( ownerFullName );
    }
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final List<NetworkGroup> results = Entities.query( new NetworkGroup( ownerFullName, groupNamePattern ) );
      final List<NetworkGroup> ret = Lists.newArrayList( results );
      db.commit( );
      return ret;
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find security group: " + groupNamePattern + " for " + ownerFullName, ex );
    }
  }

  public static Function<NetworkGroup,String> groupId() {
    return FilterFunctions.GROUP_ID;
  }

  static void createDefault( final OwnerFullName ownerFullName ) throws MetadataException {
    try {
      try {
        NetworkGroup net = Transactions.find( new NetworkGroup( AccountFullName.getInstance( ownerFullName.getAccountNumber( ) ), NETWORK_DEFAULT_NAME ) );
        if ( net == null ) {
          create( ownerFullName, NETWORK_DEFAULT_NAME, "default group" );
        }
      } catch ( NoSuchElementException ex ) {
        try {
          create( ownerFullName, NETWORK_DEFAULT_NAME, "default group" );
        } catch ( ConstraintViolationException ex1 ) {}
      } catch ( TransactionException ex ) {
        try {
          create( ownerFullName, NETWORK_DEFAULT_NAME, "default group" );
        } catch ( ConstraintViolationException ex1 ) {}
      }
    } catch ( DuplicateMetadataException ex ) {}
  }
  
  public static String defaultNetworkName( ) {
    return DEFAULT_NETWORK_NAME;
  }
  
  public static NetworkGroup create( final OwnerFullName ownerFullName, final String groupName, final String groupDescription ) throws MetadataException {
    UserFullName userFullName = null;
    if ( ownerFullName instanceof UserFullName ) {
      userFullName = ( UserFullName ) ownerFullName;
    } else {
      try {
        Account account = Accounts.lookupAccountById( ownerFullName.getAccountNumber( ) );
        User admin = Iterables.find( account.getUsers( ), new Predicate<User>( ) {
          
          @Override
          public boolean apply( User input ) {
            return input.isAccountAdmin( );
          }
        } );
        userFullName = UserFullName.getInstance( admin );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new NoSuchMetadataException( "Failed to create group because owning user could not be identified.", ex );
      }
    }
    
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      NetworkGroup net = Entities.uniqueResult( new NetworkGroup( AccountFullName.getInstance( userFullName.getAccountNumber( ) ), groupName ) );
      if ( net == null ) {
        final NetworkGroup entity = Entities.persist( new NetworkGroup( userFullName, groupName, groupDescription ) );
        db.commit( );
        return entity;
      } else {
        db.rollback( );
        throw new DuplicateMetadataException( "Failed to create group: " + groupName + " for " + userFullName.toString( ) );
      }
    } catch ( final NoSuchElementException ex ) {
      final NetworkGroup entity = Entities.persist( new NetworkGroup( userFullName, groupName, groupDescription ) );
      db.commit( );
      return entity;
    } catch ( final ConstraintViolationException ex ) {
      Logs.exhaust( ).error( ex );
      db.rollback( );
      throw new DuplicateMetadataException( "Failed to create group: " + groupName + " for " + userFullName.toString( ), ex );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new MetadataException( "Failed to create group: " + groupName + " for " + userFullName.toString( ), PersistenceExceptions.transform( ex ) );
    }
  }

  /**
   * Resolve Group Names / Identifiers for the given permissions.
   *
   * <p>Caller must have open transaction.</p>
   *
   * @param permissions - The permissions to update
   * @param defaultUserId - The account number to use when not specified
   * @param revoke - True if resolving for a revoke operation
   * @throws MetadataException If an error occurs
   */
  public static void resolvePermissions( final Iterable<IpPermissionType> permissions,
                                         final String defaultUserId,
                                         final boolean revoke) throws MetadataException {
    for ( final IpPermissionType ipPermission : permissions ) {
      if ( ipPermission.getGroups() != null ) for ( final UserIdGroupPairType groupInfo : ipPermission.getGroups() ) {
        if ( !Strings.isNullOrEmpty( groupInfo.getSourceGroupId( ) ) ) {
          try{
            final NetworkGroup networkGroup = NetworkGroups.lookupByGroupId( groupInfo.getSourceGroupId() );
            groupInfo.setSourceUserId( networkGroup.getOwnerAccountNumber() );
            groupInfo.setSourceGroupName( networkGroup.getDisplayName() );
          }catch(final NoSuchMetadataException ex){
            if(!revoke)
              throw ex;
          }
        } else if ( Strings.isNullOrEmpty( groupInfo.getSourceGroupName( ) ) ) {
          throw new MetadataException( "Group ID or Group Name required." );
        } else {
          try{
            final NetworkGroup networkGroup = NetworkGroups.lookup(
                AccountFullName.getInstance(
                    Objects.firstNonNull( Strings.emptyToNull( groupInfo.getSourceUserId() ), defaultUserId ) ),
                groupInfo.getSourceGroupName( ) );
            groupInfo.setSourceGroupId( networkGroup.getGroupId( ) );
          }catch(final NoSuchMetadataException ex){
            if(!revoke)
              throw ex;
          }
        }
      }
    }
  }

  static void flushRules( ) {
    if ( EdgeNetworking.isEnabled( ) ) {
      NetworkInfoBroadcaster.requestNetworkInfoBroadcast( );
    }
  }

  @TypeMapper
  public enum NetworkPeerAsUserIdGroupPairType implements Function<NetworkPeer, UserIdGroupPairType> {
    INSTANCE;
    
    @Override
    public UserIdGroupPairType apply( final NetworkPeer peer ) {
      return new UserIdGroupPairType(
          peer.getUserQueryKey( ),
          peer.getGroupName( ),
          peer.getGroupId( ) );
    }
  }
  
  @TypeMapper
  public enum NetworkRuleAsIpPerm implements Function<NetworkRule, IpPermissionType> {
    INSTANCE;
    
    @Override
    public IpPermissionType apply( final NetworkRule rule ) {
      final IpPermissionType ipPerm = new IpPermissionType( rule.getProtocol( ).name( ), rule.getLowPort( ), rule.getHighPort( ) );
      final Iterable<UserIdGroupPairType> peers = Iterables.transform( rule.getNetworkPeers( ),
                                                                       TypeMappers.lookup( NetworkPeer.class, UserIdGroupPairType.class ) );
      Iterables.addAll( ipPerm.getGroups( ), peers );
      ipPerm.setCidrIpRanges( rule.getIpRanges( ) );
      return ipPerm;
    }
  }
  
  @TypeMapper
  public enum NetworkGroupAsSecurityGroupItem implements Function<NetworkGroup, SecurityGroupItemType> {
    INSTANCE;
    @Override
    public SecurityGroupItemType apply( final NetworkGroup input ) {
      final EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        final NetworkGroup netGroup = Entities.merge( input );
        final SecurityGroupItemType groupInfo = new SecurityGroupItemType( netGroup.getOwnerAccountNumber( ),
                                                                           netGroup.getGroupId( ),
                                                                           netGroup.getDisplayName( ),
                                                                           netGroup.getDescription( ) );
        final Iterable<IpPermissionType> ipPerms = Iterables.transform( netGroup.getNetworkRules( ),
                                                                        TypeMappers.lookup( NetworkRule.class, IpPermissionType.class ) );
        Iterables.addAll( groupInfo.getIpPermissions( ), ipPerms );
        return groupInfo;
      } finally {
        db.rollback( );
      }
    }
    
  }
  
  public enum IpPermissionTypeExtractNetworkPeers implements Function<IpPermissionType, Collection<NetworkPeer>> {
    INSTANCE;
    
    @Override
    public Collection<NetworkPeer> apply( IpPermissionType ipPerm ) {
      final Collection<NetworkPeer> networkPeers = Lists.newArrayList();
      for ( UserIdGroupPairType peerInfo : ipPerm.getGroups( ) ) {
        networkPeers.add( new NetworkPeer( peerInfo.getSourceUserId(), peerInfo.getSourceGroupName(), peerInfo.getSourceGroupId() ) );
      }
      return networkPeers;
    }
  }
  
  public enum IpPermissionTypeAsNetworkRule implements Function<IpPermissionType, List<NetworkRule>> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Nonnull
    @Override
    public List<NetworkRule> apply( IpPermissionType ipPerm ) {
      List<NetworkRule> ruleList = new ArrayList<NetworkRule>( );
      if ( !ipPerm.getGroups( ).isEmpty( ) ) {
        if ( ipPerm.getFromPort( ) == 0 && ipPerm.getToPort( ) == 0 ) {
          ipPerm.setToPort( 65535 );
        }
        List<String> empty = Lists.newArrayList( );
        //:: fixes handling of under-specified named-network rules sent by some clients :://
        if ( ipPerm.getIpProtocol( ) == null ) {
          NetworkRule rule = NetworkRule.create( NetworkRule.Protocol.tcp, ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                                 IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), empty );
          ruleList.add( rule );
          NetworkRule rule1 = NetworkRule.create( NetworkRule.Protocol.udp, ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                                  IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), empty );
          ruleList.add( rule1 );
          NetworkRule rule2 = NetworkRule.create( NetworkRule.Protocol.tcp, -1, -1,
                                                  IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), empty );
          ruleList.add( rule2 );
        } else {
          NetworkRule rule = NetworkRule.create( ipPerm.getIpProtocol( ), ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                                 IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), empty );
          ruleList.add( rule );
        }
      } else if ( !ipPerm.getCidrIpRanges().isEmpty( ) ) {
        List<String> ipRanges = Lists.newArrayList( );
        for ( String range : ipPerm.getCidrIpRanges() ) {
          String[] rangeParts = range.split( "/" );
          try {
            if ( rangeParts.length != 2 ) throw new IllegalArgumentException( );
            if ( Integer.parseInt( rangeParts[1] ) > 32 || Integer.parseInt( rangeParts[1] ) < 0 ) throw new IllegalArgumentException( );
            if ( InetAddresses.forString( rangeParts[0] ) instanceof Inet4Address ) {
              ipRanges.add( range );
            }
          } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Invalid IP range: '"+range+"'" );
          }
        }
        NetworkRule rule = NetworkRule.create( ipPerm.getIpProtocol( ), ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                               IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), ipRanges );
        ruleList.add( rule );
      } else {
        throw new IllegalArgumentException( "Invalid Ip Permissions:  must specify either a source cidr or user" );
      }
      return ruleList;
    }
  }
  
  static List<NetworkRule> ipPermissionsAsNetworkRules( final List<IpPermissionType> ipPermissions ) {
    final List<NetworkRule> ruleList = Lists.newArrayList( );
    for ( final IpPermissionType ipPerm : ipPermissions ) {
      ruleList.addAll( IpPermissionTypeAsNetworkRule.INSTANCE.apply( ipPerm ) );
    }
    return ruleList;
  }

  public static class NetworkGroupFilterSupport extends FilterSupport<NetworkGroup> {
    public NetworkGroupFilterSupport() {
      super( builderFor( NetworkGroup.class )
          .withTagFiltering( NetworkGroupTag.class, "networkGroup" )
          .withStringProperty( "description", FilterFunctions.DESCRIPTION )
          .withStringProperty( "group-id", FilterFunctions.GROUP_ID )
          .withStringProperty( "group-name", CloudMetadatas.toDisplayName() )
          .withStringSetProperty( "ip-permission.cidr", FilterSetFunctions.PERMISSION_CIDR )
          .withStringSetProperty( "ip-permission.from-port", FilterSetFunctions.PERMISSION_FROM_PORT )
          .withStringSetProperty( "ip-permission.group-name", FilterSetFunctions.PERMISSION_GROUP )
          .withStringSetProperty( "ip-permission.protocol", FilterSetFunctions.PERMISSION_PROTOCOL )
          .withStringSetProperty( "ip-permission.to-port", FilterSetFunctions.PERMISSION_TO_PORT )
          .withStringSetProperty( "ip-permission.user-id", FilterSetFunctions.PERMISSION_ACCOUNT_ID )
          .withStringProperty( "owner-id", FilterFunctions.ACCOUNT_ID )
          .withPersistenceAlias( "networkRules", "networkRules" )
          .withPersistenceFilter( "description" )
          .withPersistenceFilter( "group-id", "groupId" )
          .withPersistenceFilter( "group-name", "displayName" )
          .withPersistenceFilter( "ip-permission.from-port", "networkRules.lowPort", PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "ip-permission.protocol", "networkRules.protocol", Enums.valueOfFunction( NetworkRule.Protocol.class ) )
          .withPersistenceFilter( "ip-permission.to-port", "networkRules.highPort", PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" ) );
    }
  }

  private enum FilterFunctions implements Function<NetworkGroup,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final NetworkGroup group ) {
        return group.getOwnerAccountNumber();
      }
    },
    DESCRIPTION {
      @Override
      public String apply( final NetworkGroup group ) {
        return group.getDescription();
      }
    },
    GROUP_ID {
      @Override
      public String apply( final NetworkGroup group ) {
        return group.getGroupId();
      }
    },
  }

  private enum FilterSetFunctions implements Function<NetworkGroup,Set<String>> {
    PERMISSION_CIDR {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          result.addAll( rule.getIpRanges() );
        }
        return result;
      }
    },
    PERMISSION_FROM_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          result.add( Integer.toString( rule.getLowPort() ) );
        }
        return result;
      }
    },
    PERMISSION_GROUP {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getGroupName() != null ) result.add( peer.getGroupName() );
          }
        }
        return result;
      }
    },
    PERMISSION_PROTOCOL {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          if ( rule.getProtocol() != null ) result.add( rule.getProtocol().name() );
        }
        return result;
      }
    },
    PERMISSION_TO_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          result.add( Integer.toString( rule.getHighPort() ) );
        }
        return result;
      }
    },
    PERMISSION_ACCOUNT_ID {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getUserQueryKey() != null ) result.add( peer.getUserQueryKey() );
          }
        }
        return result;
      }
    }
  }

  @RestrictedTypes.QuantityMetricFunction( CloudMetadata.NetworkGroupMetadata.class )
  public enum CountNetworkGroups implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( NetworkGroup.class );
      try {
        return Entities.count( NetworkGroup.withOwner( input ) );
      } finally {
        db.rollback( );
      }
    }
  }
}
