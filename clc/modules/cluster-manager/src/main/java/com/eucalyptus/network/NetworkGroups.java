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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.util.DuplicateMetadataException;
import com.eucalyptus.cloud.util.IllegalMetadataAccessException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.Properties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.NetworkGroups.NetworkRangeConfiguration;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
  @ConfigurableField( description = "Minutes before a pending index allocation timesout and is released." )
  public static Integer       NETWORK_INDEX_PENDING_TIMEOUT = 5;
  
  public static class NetworkRangeConfiguration {
    private Boolean useNetworkTags  = Boolean.TRUE;
    private Integer minNetworkTag   = GLOBAL_MIN_NETWORK_TAG;
    private Integer maxNetworkTag   = GLOBAL_MAX_NETWORK_TAG;
    private Long    minNetworkIndex = GLOBAL_MIN_NETWORK_INDEX;
    private Long    maxNetworkIndex = GLOBAL_MAX_NETWORK_INDEX;
    
    public Boolean hasNetworking( ) {
      return this.useNetworkTags;
    }
    
    public Boolean getUseNetworkTags( ) {
      return this.useNetworkTags;
    }
    
    public void setUseNetworkTags( final Boolean useNetworkTags ) {
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
  
  static NetworkRangeConfiguration netConfig = new NetworkRangeConfiguration( );
  
  public static synchronized void updateNetworkRangeConfiguration( ) {
    final AtomicReference<NetworkRangeConfiguration> equalityCheck = new AtomicReference( null );
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
  
  public static NetworkRangeConfiguration networkingConfiguration( ) {
    return netConfig;
  }
  
  public static NetworkGroup delete( final OwnerFullName ownerFullName, final String groupName ) throws MetadataException {
    if ( defaultNetworkName( ).equals( groupName ) ) {
      createDefault( ownerFullName );
    }
    final EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      final NetworkGroup ret = Entities.uniqueResult( new NetworkGroup( ownerFullName, groupName ) );
      Entities.delete( ret );
      db.commit( );
      return ret;
    } catch ( final ConstraintViolationException ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new IllegalMetadataAccessException( "Failed to delete security group: " + groupName + " for " + ownerFullName + " because of: "
                                                + Exceptions.causeString( ex ), ex );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find security group: " + groupName + " for " + ownerFullName, ex );
    }
  }
  
  public static NetworkGroup lookup( final String groupId ) throws NoSuchMetadataException {
    EntityTransaction db = Entities.get( NetworkGroup.class );
    try {
      NetworkGroup entity = Entities.uniqueResult( NetworkGroup.named( null, groupId ) );
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
      final NetworkGroup ret = Entities.uniqueResult( new NetworkGroup( ownerFullName, groupName ) );
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
  
  @TypeMapper
  public enum NetworkPeerAsUserIdGroupPairType implements Function<NetworkPeer, UserIdGroupPairType> {
    INSTANCE;
    
    @Override
    public UserIdGroupPairType apply( final NetworkPeer peer ) {
      return new UserIdGroupPairType( peer.getUserQueryKey( ), peer.getGroupName( ) );
    }
  }
  
  @TypeMapper
  public enum NetworkRuleAsIpPerm implements Function<NetworkRule, IpPermissionType> {
    INSTANCE;
    
    @Override
    public IpPermissionType apply( final NetworkRule rule ) {
      final IpPermissionType ipPerm = new IpPermissionType( rule.getProtocol( ), rule.getLowPort( ), rule.getHighPort( ) );
      final Iterable<UserIdGroupPairType> peers = Iterables.transform( rule.getNetworkPeers( ),
                                                                       TypeMappers.lookup( NetworkPeer.class, UserIdGroupPairType.class ) );
      Iterables.addAll( ipPerm.getGroups( ), peers );
      ipPerm.getIpRanges( ).addAll( rule.getIpRanges( ) );
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
        final SecurityGroupItemType groupInfo = new SecurityGroupItemType( netGroup.getOwnerAccountNumber( ), netGroup.getDisplayName( ),
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
  
  @TypeMapper
  public enum IpPermissionTypeExtractNetworkPeers implements Function<IpPermissionType, Multimap<String, String>> {
    INSTANCE;
    
    @Override
    public Multimap<String, String> apply( IpPermissionType ipPerm ) {
      Multimap<String, String> networkPeers = ArrayListMultimap.create( );
      for ( UserIdGroupPairType peerInfo : ipPerm.getGroups( ) ) {
        networkPeers.put( peerInfo.getSourceUserId( ), peerInfo.getSourceGroupName( ) );
      }
      return networkPeers;
    }
  }
  
  @TypeMapper
  public enum IpPermissionTypeAsNetworkRule implements Function<IpPermissionType, List<NetworkRule>> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
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
      } else if ( !ipPerm.getIpRanges( ).isEmpty( ) ) {
        List<String> ipRanges = Lists.newArrayList( );
        for ( String range : ipPerm.getIpRanges( ) ) {
          String[] rangeParts = range.split( "/" );
          try {
            if ( Integer.parseInt( rangeParts[1] ) > 32 || Integer.parseInt( rangeParts[1] ) < 0 ) continue;
            if ( rangeParts.length != 2 ) continue;
            if ( InetAddress.getByName( rangeParts[0] ) != null ) {
              ipRanges.add( range );
            }
          } catch ( NumberFormatException e ) {} catch ( UnknownHostException e ) {}
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
  
}
