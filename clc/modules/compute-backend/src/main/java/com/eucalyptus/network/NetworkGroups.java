/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.compute.common.CidrIpType;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.network.NetworkCidr;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.NetworkGroupTag;
import com.eucalyptus.compute.common.internal.network.NetworkPeer;
import com.eucalyptus.compute.common.internal.network.NetworkRule;
import com.eucalyptus.compute.common.internal.network.NoSuchGroupMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataConstraintException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.records.Logs;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ConfigurableClass( root = "cloud.network",
                    description = "Default values used to bootstrap networking state discovery." )
public class NetworkGroups extends com.eucalyptus.compute.common.internal.network.NetworkGroups {

  public static final Pattern VPC_GROUP_NAME_PATTERN       = Pattern.compile( "[a-zA-Z0-9 ._\\-:/()#,@\\[\\]+=&;{}!$*]{1,255}" );
  public static final Pattern VPC_GROUP_DESC_PATTERN       = Pattern.compile( "[a-zA-Z0-9 ._\\-:/()#,@\\[\\]+=&;{}!$*]{0,255}" );
  private static Logger       LOG                          = Logger.getLogger( NetworkGroups.class );

  @ConfigurableField( description = "Minutes before a pending index allocation timesout and is released.", initial = "35" )
  public static volatile Integer      NETWORK_INDEX_PENDING_TIMEOUT = 35;
  @ConfigurableField( description = "Minutes before a pending system public address allocation timesout and is released.", initial = "35" )
  public static volatile Integer      ADDRESS_PENDING_TIMEOUT       = 35;
  @ConfigurableField(
      description = "Network configuration document.",
      changeListener = NetworkConfigurations.NetworkConfigurationPropertyChangeListener.class )
  public static volatile String       NETWORK_CONFIGURATION         = "";
  @ConfigurableField( description = "Minimum interval between broadcasts of network information (seconds).", initial = "5" )
  public static volatile Integer      MIN_BROADCAST_INTERVAL        = 5;
  @ConfigurableField( description = "Maximum time to apply network information (seconds).", initial = "120" )
  public static volatile Integer      MAX_BROADCAST_APPLY           = 120;

  public static NetworkGroup delete( final String groupId ) throws MetadataException {
    try ( final TransactionResource db = Entities.transactionFor( NetworkGroup.class ) ) {
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
    }
  }

  public static NetworkGroup lookupByGroupId( final String groupId ) throws NoSuchMetadataException {
    return lookupByGroupId( null, groupId );
  }

  public static NetworkGroup lookupByGroupId( @Nullable final OwnerFullName ownerFullName,
                                              final String groupId ) throws NoSuchMetadataException {
      try ( final TransactionResource db = Entities.transactionFor( NetworkGroup.class ) ) {
        NetworkGroup entity = Entities.uniqueResult( NetworkGroup.withGroupId(ownerFullName, groupId) );
        db.commit( );
        return entity;
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        throw new NoSuchMetadataException( "Failed to find security group: " + groupId, ex );
      }
    }
  
  public static NetworkGroup lookup( final OwnerFullName ownerFullName, final String groupName ) throws MetadataException {
    if ( defaultNetworkName( ).equals( groupName ) ) {
      createDefault( ownerFullName );
    }
    try ( final TransactionResource db = Entities.transactionFor( NetworkGroup.class ) ) {
      NetworkGroup ret = Entities.uniqueResult( NetworkGroup.named( ownerFullName, groupName ) );
      db.commit( );
      return ret;
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      LOG.debug( "Failed to find security group: " + groupName + " for " + ownerFullName, ex );
      throw new NoSuchGroupMetadataException( "The security group '" + groupName + "' does not exist" );
    }
  }

  public static NetworkGroup lookupDefault( final OwnerFullName ownerFullName,
                                            final String vpcId ) throws MetadataException {
    return lookup( ownerFullName, vpcId, defaultNetworkName( ) );
  }

  public static NetworkGroup lookup( final OwnerFullName ownerFullName,
                                     final String vpcId,
                                     final String name ) throws MetadataException {
    try ( final TransactionResource db = Entities.transactionFor( NetworkGroup.class ) ) {
      return Entities.uniqueResult( ownerFullName == null && vpcId != null ?
              NetworkGroup.namedForVpc( vpcId, name ) :
              NetworkGroup.withUniqueName( ownerFullName, vpcId, name )
      );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new NoSuchMetadataException( "Failed to find security group: " + name +", for vpc: " + vpcId + " for " + ownerFullName, ex );
    }
  }

  /**
   * Resolve Group Names / Identifiers for the given permissions.
   *
   * <p>Caller must have open transaction.</p>
   *
   * @param permissions - The permissions to update
   * @param defaultUserId - The account number to use when not specified
   * @param vpcId - The identifier for the VPC if the a VPC security group
   * @param revoke - True if resolving for a revoke operation
   * @throws MetadataException If an error occurs
   */
  public static void resolvePermissions( final Iterable<IpPermissionType> permissions,
                                         final String defaultUserId,
                                         @Nullable final String vpcId,
                                         final boolean revoke ) throws MetadataException {
    for ( final IpPermissionType ipPermission : permissions ) {
      if ( ipPermission.getGroups() != null ) for ( final UserIdGroupPairType groupInfo : ipPermission.getGroups() ) {
        if ( !Strings.isNullOrEmpty( groupInfo.getSourceGroupId( ) ) ) {
          try{
            final NetworkGroup networkGroup = NetworkGroups.lookupByGroupId( groupInfo.getSourceGroupId() );
            if ( vpcId != null && !vpcId.equals( networkGroup.getVpcId( ) ) ) {
              throw new NoSuchMetadataException( "Group ("+groupInfo.getSourceGroupId()+") not found." );
            }
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
            final AccountFullName accountFullName = AccountFullName.getInstance(
                MoreObjects.firstNonNull( Strings.emptyToNull( groupInfo.getSourceUserId() ), defaultUserId ) );
            final NetworkGroup networkGroup = vpcId == null ?
                NetworkGroups.lookup( accountFullName, groupInfo.getSourceGroupName( ) ) :
                NetworkGroups.lookup( accountFullName, vpcId, groupInfo.getSourceGroupName( ) );
            groupInfo.setSourceGroupId( networkGroup.getGroupId( ) );
          }catch(final NoSuchMetadataException ex){
            if(!revoke)
              throw ex;
          }
        }
      }
    }
  }

  public static void flushRules( ) {
    NetworkInfoBroadcaster.requestNetworkInfoBroadcast( );
  }

  @TypeMapper
  public enum NetworkPeerAsUserIdGroupPairType implements CompatFunction<NetworkPeer, UserIdGroupPairType> {
    INSTANCE;
    
    @Override
    public UserIdGroupPairType apply( final NetworkPeer peer ) {
      return new UserIdGroupPairType(
          peer.getUserQueryKey( ),
          peer.getGroupName( ),
          peer.getGroupId( ),
          peer.getDescription( ) );
    }
  }

  @TypeMapper
  public enum NetworkCidrAsCidrIpType implements CompatFunction<NetworkCidr, CidrIpType> {
    INSTANCE;

    @Override
    public CidrIpType apply( final NetworkCidr cidr ) {
      return new CidrIpType(
          cidr.getCidrIp(),
          cidr.getDescription( ) );
    }
  }

  @TypeMapper
  public enum NetworkRuleAsIpPerm implements CompatFunction<NetworkRule, IpPermissionType> {
    INSTANCE;
    
    @Override
    public IpPermissionType apply( final NetworkRule rule ) {
      final IpPermissionType ipPerm = new IpPermissionType(
          rule.getDisplayProtocol( ), rule.getLowPort( ), rule.getHighPort( ) );
      ipPerm.getGroups( ).addAll( rule.getNetworkPeers( ).stream( )
          .map( TypeMappers.lookupF( NetworkPeer.class, UserIdGroupPairType.class ) )
          .collect( Collectors.toList( ) ) );
      ipPerm.getIpRanges( ).addAll( rule.getIpRanges( ).stream( )
          .map( TypeMappers.lookupF( NetworkCidr.class, CidrIpType.class ) )
          .collect( Collectors.toList( ) ) );
      return ipPerm;
    }
  }
  
  @TypeMapper
  public enum NetworkGroupAsSecurityGroupItem implements CompatFunction<NetworkGroup, SecurityGroupItemType> {
    INSTANCE;
    @Override
    public SecurityGroupItemType apply( final NetworkGroup input ) {
      try ( final TransactionResource tx = Entities.transactionFor( NetworkGroup.class ) ) {
        final NetworkGroup netGroup = Entities.merge( input );
        final SecurityGroupItemType groupInfo = new SecurityGroupItemType( netGroup.getOwnerAccountNumber( ),
                                                                           netGroup.getGroupId( ),
                                                                           netGroup.getDisplayName( ),
                                                                           netGroup.getDescription( ),
                                                                           netGroup.getVpcId( ) );
        final Iterable<IpPermissionType> ipPerms = Iterables.transform(
            netGroup.getIngressNetworkRules( ),
            TypeMappers.lookup( NetworkRule.class, IpPermissionType.class ) );
        Iterables.addAll( groupInfo.getIpPermissions( ), ipPerms );

        final Iterable<IpPermissionType> ipPermsEgress = Iterables.transform(
            netGroup.getEgressNetworkRules( ),
            TypeMappers.lookup( NetworkRule.class, IpPermissionType.class ) );
        Iterables.addAll( groupInfo.getIpPermissionsEgress( ), ipPermsEgress );

        return groupInfo;
      }
    }
  }
  
  public enum IpPermissionTypeExtractNetworkPeers implements CompatFunction<IpPermissionType, Collection<NetworkPeer>> {
    INSTANCE;
    
    @Override
    public Collection<NetworkPeer> apply( IpPermissionType ipPerm ) {
      final Collection<NetworkPeer> networkPeers = Lists.newArrayList();
      for ( UserIdGroupPairType peerInfo : ipPerm.getGroups( ) ) {
        networkPeers.add( new NetworkPeer(
            peerInfo.getSourceUserId(),
            peerInfo.getSourceGroupName(),
            peerInfo.getSourceGroupId(),
            peerInfo.getDescription( ) ) );
      }
      return networkPeers;
    }
  }

  private static class IpPermissionTypeAsNetworkRule implements CompatFunction<IpPermissionType, List<NetworkRule>> {
    private final boolean anyProtocolAllowed;
    private final boolean forRevoke;

    private IpPermissionTypeAsNetworkRule( boolean anyProtocolAllowed, boolean forRevoke ) {
      this.anyProtocolAllowed = anyProtocolAllowed;
      this.forRevoke = forRevoke;
    }
    
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Nonnull
    @Override
    public List<NetworkRule> apply( IpPermissionType ipPerm ) {
      List<NetworkRule> ruleList = new ArrayList<NetworkRule>( );
      if ( !ipPerm.getGroups( ).isEmpty( ) ) {
        if ( ipPerm.getFromPort()!=null && ipPerm.getFromPort( ) == 0 && ipPerm.getToPort( ) != null && ipPerm.getToPort( ) == 0 ) {
          ipPerm.setToPort( 65535 );
        }
        List<NetworkCidr> empty = Collections.emptyList( );
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
          NetworkRule rule = NetworkRule.create( ipPerm.getIpProtocol( ), anyProtocolAllowed, ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                                 IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), empty );
          ruleList.add( rule );
        }
      } else if ( !ipPerm.getIpRanges( ).isEmpty( ) ) {
        List<NetworkCidr> ipRanges = Lists.newArrayList( );
        List<NetworkCidr> literalIpRanges = Lists.newArrayList( );
        for ( final CidrIpType range : ipPerm.getIpRanges( ) ) {
          try {
            if ( range.getCidrIp()==null || range.getCidrIp().indexOf( '/' ) < 0 ) {
              throw new IllegalArgumentException( "Invalid ip range" );
            }
            try {
              ipRanges.add( NetworkCidr.create(
                  Cidr.parse( range.getCidrIp(), true ).toString( ),
                  range.getDescription( ) ) );
            } catch ( IllegalArgumentException e ) {
              if ( forRevoke ) {
                ipRanges.add( NetworkCidr.create(
                    range.getCidrIp( ),
                    range.getDescription( ) ) );
              } else {
                throw e;
              }
            }
            literalIpRanges.add( NetworkCidr.create( range.getCidrIp( )  ) );
          } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Invalid IP range: '"+range+"'" );
          }
        }
        NetworkRule rule = NetworkRule.create( ipPerm.getIpProtocol( ), anyProtocolAllowed, ipPerm.getFromPort( ), ipPerm.getToPort( ),
                                               IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), ipRanges );
        ruleList.add( rule );
        if ( forRevoke && !ipRanges.equals( literalIpRanges ) ) {
          // When removing rules we must allow removal of rules that were created before we
          // fully validated CIDRs so create an additional network rule for matching if needed
          ruleList.add( NetworkRule.create( ipPerm.getIpProtocol( ), anyProtocolAllowed, ipPerm.getFromPort( ), ipPerm.getToPort( ),
              IpPermissionTypeExtractNetworkPeers.INSTANCE.apply( ipPerm ), literalIpRanges ) );
        }
      } else {
        throw new IllegalArgumentException( "Invalid Ip Permissions:  must specify either a source cidr or user" );
      }
      return ruleList;
    }
  }

  static List<NetworkRule> ipPermissionAsNetworkRules( final IpPermissionType ipPermission,
                                                       final boolean vpc,
                                                       final boolean forRevoke ) {
    return ipPermissionsAsNetworkRules( Collections.singletonList( ipPermission ), vpc, forRevoke );
  }

  static List<NetworkRule> ipPermissionsAsNetworkRules( final List<IpPermissionType> ipPermissions,
                                                        final boolean vpc,
                                                        final boolean forRevoke ) {
    return Lists.newArrayList( Iterables.concat( Iterables.transform(
        ipPermissions,
        new IpPermissionTypeAsNetworkRule( vpc, forRevoke )
    ) ) );
  }

  @RestrictedTypes.Resolver( NetworkGroup.class )
  public enum Lookup implements CompatFunction<String, NetworkGroup> {
    INSTANCE;

    @Override
    public NetworkGroup apply( final String identifier ) {
      try {
        return NetworkGroups.lookupByGroupId( identifier );
      } catch ( NoSuchMetadataException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  public static class NetworkGroupFilterSupport extends FilterSupport<NetworkGroup> {
    public NetworkGroupFilterSupport() {
      super( builderFor( NetworkGroup.class )
          .withTagFiltering( NetworkGroupTag.class, "networkGroup" )
          .withStringProperty( "description", NetworkGroup.description( ) )
          .withStringSetProperty( "egress.ip-permission.cidr", FilterSetFunctions.EGRESS_PERMISSION_CIDR )
          .withStringSetProperty( "egress.ip-permission.from-port", FilterSetFunctions.EGRESS_PERMISSION_FROM_PORT )
          .withStringSetProperty( "egress.ip-permission.group-id", FilterSetFunctions.EGRESS_PERMISSION_GROUP_ID )
          .withStringSetProperty( "egress.ip-permission.group-name", FilterSetFunctions.EGRESS_PERMISSION_GROUP )
          .withStringSetProperty( "egress.ip-permission.protocol", FilterSetFunctions.EGRESS_PERMISSION_PROTOCOL )
          .withStringSetProperty( "egress.ip-permission.to-port", FilterSetFunctions.EGRESS_PERMISSION_TO_PORT )
          .withStringSetProperty( "egress.ip-permission.user-id", FilterSetFunctions.EGRESS_PERMISSION_ACCOUNT_ID )
          .withStringProperty( "group-id", NetworkGroup.groupId() )
          .withStringProperty( "group-name", CloudMetadatas.toDisplayName( ) )
          .withStringSetProperty( "ip-permission.cidr", FilterSetFunctions.PERMISSION_CIDR )
          .withStringSetProperty( "ip-permission.from-port", FilterSetFunctions.PERMISSION_FROM_PORT )
          .withStringSetProperty( "ip-permission.group-id", FilterSetFunctions.PERMISSION_GROUP_ID )
          .withStringSetProperty( "ip-permission.group-name", FilterSetFunctions.PERMISSION_GROUP )
          .withStringSetProperty( "ip-permission.protocol", FilterSetFunctions.PERMISSION_PROTOCOL )
          .withStringSetProperty( "ip-permission.to-port", FilterSetFunctions.PERMISSION_TO_PORT )
          .withStringSetProperty( "ip-permission.user-id", FilterSetFunctions.PERMISSION_ACCOUNT_ID )
          .withStringProperty( "owner-id", NetworkGroup.accountNumber() )
          .withStringProperty( "vpc-id", NetworkGroup.vpcId() )
          .withPersistenceAlias( "networkRules", "networkRules" )
          .withPersistenceFilter( "description" )
          .withPersistenceFilter( "group-id", "groupId" )
          .withPersistenceFilter( "group-name", "displayName" )
          .withPersistenceFilter( "ip-permission.from-port", "networkRules.lowPort", PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "ip-permission.protocol", "networkRules.protocol", FUtils.valueOfFunction( NetworkRule.Protocol.class ) )
          .withPersistenceFilter( "ip-permission.to-port", "networkRules.highPort", PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "vpc-id", "vpcId" )
          .withUnsupportedProperty("egress.ip-permission.ipv6-cidr")
          .withUnsupportedProperty("egress.ip-permission.prefix-list-id")
          .withUnsupportedProperty("ip-permission.ipv6-cidr")
          .withUnsupportedProperty("ip-permission.prefix-list-id")
      );
    }
  }

  private enum FilterSetFunctions implements CompatFunction<NetworkGroup,Set<String>> {
    EGRESS_PERMISSION_CIDR {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          result.addAll( rule.getIpRanges( ).stream( ).map( NetworkCidr::getCidrIp ).collect( Collectors.toList( ) ) );
        }
        return result;
      }
    },
    EGRESS_PERMISSION_FROM_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          result.addAll( Optional.fromNullable( rule.getLowPort() ).transform( Functions.toStringFunction() ).asSet() );
        }
        return result;
      }
    },
    EGRESS_PERMISSION_GROUP {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getGroupName() != null ) result.add( peer.getGroupName() );
          }
        }
        return result;
      }
    },
    EGRESS_PERMISSION_GROUP_ID {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getGroupId() != null ) result.add( peer.getGroupId() );
          }
        }
        return result;
      }
    },
    EGRESS_PERMISSION_PROTOCOL {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          result.add( rule.getDisplayProtocol( ) );
        }
        return result;
      }
    },
    EGRESS_PERMISSION_TO_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          result.addAll( Optional.fromNullable( rule.getHighPort() ).transform( Functions.toStringFunction() ).asSet() );
        }
        return result;
      }
    },
    EGRESS_PERMISSION_ACCOUNT_ID {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getEgressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getUserQueryKey() != null ) result.add( peer.getUserQueryKey() );
          }
        }
        return result;
      }
    },
    PERMISSION_CIDR {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          result.addAll( rule.getIpRanges( ).stream( ).map( NetworkCidr::getCidrIp ).collect( Collectors.toList( ) ) );
        }
        return result;
      }
    },
    PERMISSION_FROM_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          result.addAll( Optional.fromNullable( rule.getLowPort() ).transform( Functions.toStringFunction() ).asSet() );
        }
        return result;
      }
    },
    PERMISSION_GROUP {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getGroupName() != null ) result.add( peer.getGroupName() );
          }
        }
        return result;
      }
    },
    PERMISSION_GROUP_ID {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getGroupId() != null ) result.add( peer.getGroupId() );
          }
        }
        return result;
      }
    },
    PERMISSION_PROTOCOL {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          result.add( rule.getDisplayProtocol( ) );
        }
        return result;
      }
    },
    PERMISSION_TO_PORT {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          result.addAll( Optional.fromNullable( rule.getHighPort() ).transform( Functions.toStringFunction() ).asSet() );
        }
        return result;
      }
    },
    PERMISSION_ACCOUNT_ID {
      @Override
      public Set<String> apply( final NetworkGroup group ) {
        final Set<String> result = Sets.newHashSet();
        for ( final NetworkRule rule : group.getIngressNetworkRules() ) {
          for ( final NetworkPeer peer : rule.getNetworkPeers() ) {
            if ( peer.getUserQueryKey() != null ) result.add( peer.getUserQueryKey() );
          }
        }
        return result;
      }
    }
  }

  @RestrictedTypes.QuantityMetricFunction( CloudMetadata.NetworkGroupMetadata.class )
  public enum CountNetworkGroups implements CompatFunction<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor( NetworkGroup.class ) ) {
        return Entities.count( NetworkGroup.withOwner( input ) ); //TODO:STEVE: Quota for regular vs VPC security groups
      }
    }
  }
}
