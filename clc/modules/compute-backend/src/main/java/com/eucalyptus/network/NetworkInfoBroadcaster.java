/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.network;

import static java.util.function.Function.identity;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.broadcast.BNI;
import com.eucalyptus.cluster.common.broadcast.BNICluster;
import com.eucalyptus.cluster.common.broadcast.BNIClusters;
import com.eucalyptus.cluster.common.broadcast.BNIHasName;
import com.eucalyptus.cluster.common.broadcast.BNIInstance;
import com.eucalyptus.cluster.common.broadcast.BNIManagedSubnets;
import com.eucalyptus.cluster.common.broadcast.BNIMidonet;
import com.eucalyptus.cluster.common.broadcast.BNINetworkAclEntry;
import com.eucalyptus.cluster.common.broadcast.BNINode;
import com.eucalyptus.cluster.common.broadcast.BNIPropertyBase;
import com.eucalyptus.cluster.common.broadcast.BNIRoute;
import com.eucalyptus.cluster.common.broadcast.BNISecurityGroup;
import com.eucalyptus.cluster.common.broadcast.BNISubnets;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.broadcast.ImmutableBNICluster;
import com.eucalyptus.cluster.common.broadcast.ImmutableBNIClusters;
import com.eucalyptus.cluster.common.broadcast.ImmutableBNIConfiguration;
import com.eucalyptus.cluster.common.broadcast.ImmutableBNetworkInfo;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.network.NetworkRule;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet;
import com.eucalyptus.compute.common.internal.vpc.InternetGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl;
import com.eucalyptus.compute.common.internal.vpc.NetworkAclEntry;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcls;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.internal.vpc.Route;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.RouteTableAssociation;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.vpc.EventFiringVpcRouteStateInvalidator;
import com.eucalyptus.compute.vpc.RouteKey;
import com.eucalyptus.compute.vpc.VpcRouteStateInvalidator;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.network.NetworkView.IPPermissionNetworkView;
import com.eucalyptus.network.NetworkView.NetworkAclEntryNetworkView;
import com.eucalyptus.network.NetworkView.RouteNetworkView;
import com.eucalyptus.network.applicator.ApplicatorException;
import com.eucalyptus.network.applicator.Applicators;
import com.eucalyptus.network.config.NetworkConfigurationApi;
import com.eucalyptus.network.config.NetworkConfigurationApi.ManagedSubnet;
import com.eucalyptus.network.config.NetworkConfigurationApi.NetworkConfiguration;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.network.NetworkView.NatGatewayNetworkView;
import com.eucalyptus.network.NetworkView.VmInstanceNetworkView;
import com.eucalyptus.network.NetworkView.NetworkGroupNetworkView;
import com.eucalyptus.network.NetworkView.VpcNetworkView;
import com.eucalyptus.network.NetworkView.SubnetNetworkView;
import com.eucalyptus.network.NetworkView.DhcpOptionSetNetworkView;
import com.eucalyptus.network.NetworkView.NetworkAclNetworkView;
import com.eucalyptus.network.NetworkView.RouteTableNetworkView;
import com.eucalyptus.network.NetworkView.InternetGatewayNetworkView;
import com.eucalyptus.network.NetworkView.NetworkInterfaceNetworkView;
import com.eucalyptus.network.NetworkView.VersionedNetworkView;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.CompatPredicate;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Lens;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.SemaphoreResource;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.PrimitiveSink;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.eucalyptus.cluster.common.broadcast.BNI.clusterArrayLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.configuration;
import static com.eucalyptus.cluster.common.broadcast.BNI.dhcpOptionSetsLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.gateway;
import static com.eucalyptus.cluster.common.broadcast.BNI.gateways;
import static com.eucalyptus.cluster.common.broadcast.BNI.instancesLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.internetGatewaysLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.managedSubnets;
import static com.eucalyptus.cluster.common.broadcast.BNI.midonet;
import static com.eucalyptus.cluster.common.broadcast.BNI.networkAclEntries;
import static com.eucalyptus.cluster.common.broadcast.BNI.networkInfo;
import static com.eucalyptus.cluster.common.broadcast.BNI.node;
import static com.eucalyptus.cluster.common.broadcast.BNI.nodeArrayLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.nodes;
import static com.eucalyptus.cluster.common.broadcast.BNI.property;
import static com.eucalyptus.cluster.common.broadcast.BNI.route;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroup;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroupIpPermission;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroupLens;
import static com.eucalyptus.cluster.common.broadcast.BNI.securityGroupRules;
import static com.eucalyptus.cluster.common.broadcast.BNI.subnet;
import static com.eucalyptus.cluster.common.broadcast.BNI.subnets;
import static com.eucalyptus.cluster.common.broadcast.BNI.vpcSubnet;
import static com.eucalyptus.cluster.common.broadcast.BNI.vpcsLens;
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.hash.Hashing.goodFastHash;
import io.vavr.collection.Array;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
@SuppressWarnings( { "Guava", "Convert2Lambda", "StaticPseudoFunctionalStyleMethod" } )
public class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster.class );

  private static volatile boolean MODE_CLEAN =
      Boolean.valueOf( System.getProperty( "com.eucalyptus.network.broadcastModeClean", "true" ) );

  private static final AtomicLong lastBroadcastTime = new AtomicLong( 0L );
  private static final Lock lastBroadcastTimeLock = new ReentrantLock( );
  private static final Semaphore activeBroadcastSemaphore = new Semaphore( 1 );
  private static final EntityCache<VmInstance,NetworkView.VmInstanceNetworkView> instanceCache = new EntityCache<>(
      VmInstance.named(null),
      Restrictions.not( VmInstance.criterion( TORNDOWN.array( ) ) ),
      Sets.newHashSet( "networkGroups" ),
      Sets.newHashSet( "bootRecord.machineImage", "bootRecord.vmType" ),
      TypeMappers.lookup( VmInstance.class, VmInstanceNetworkView.class )  );
  private static final EntityCache<NetworkGroup,NetworkGroupNetworkView> securityGroupCache =
      new EntityCache<>( NetworkGroup.withNaturalId( null ), TypeMappers.lookup( NetworkGroup.class, NetworkGroupNetworkView.class )  );
  private static final EntityCache<Vpc,VpcNetworkView> vpcCache =
      new EntityCache<>( Vpc.exampleWithOwner( null ), TypeMappers.lookup( Vpc.class, VpcNetworkView.class )  );
  private static final EntityCache<Subnet,SubnetNetworkView> subnetCache =
      new EntityCache<>( Subnet.exampleWithOwner( null ), TypeMappers.lookup( Subnet.class, SubnetNetworkView.class )  );
  private static final EntityCache<DhcpOptionSet,DhcpOptionSetNetworkView> dhcpOptionsCache =
      new EntityCache<>( DhcpOptionSet.exampleWithOwner( null ), TypeMappers.lookup( DhcpOptionSet.class, DhcpOptionSetNetworkView.class )  );
  private static final EntityCache<NetworkAcl,NetworkAclNetworkView> networkAclCache =
      new EntityCache<>( NetworkAcl.exampleWithOwner( null ), TypeMappers.lookup( NetworkAcl.class, NetworkAclNetworkView.class )  );
  private static final EntityCache<RouteTable,RouteTableNetworkView> routeTableCache =
      new EntityCache<>( RouteTable.exampleWithOwner( null ), TypeMappers.lookup( RouteTable.class, RouteTableNetworkView.class )  );
  private static final EntityCache<InternetGateway,InternetGatewayNetworkView> internetGatewayCache =
      new EntityCache<>( InternetGateway.exampleWithOwner( null ), TypeMappers.lookup( InternetGateway.class, InternetGatewayNetworkView.class )  );
  private static final EntityCache<NetworkInterface,NetworkInterfaceNetworkView> networkInterfaceCache =
      new EntityCache<>( NetworkInterface.exampleWithOwner( null ), TypeMappers.lookup( NetworkInterface.class, NetworkInterfaceNetworkView.class )  );
  private static final EntityCache<NatGateway,NatGatewayNetworkView> natGatewayCache =
      new EntityCache<>( NatGateway.exampleWithOwner( null ), TypeMappers.lookup( NatGateway.class, NatGatewayNetworkView.class )  );
  private static final VpcRouteStateInvalidator vpcRouteStateInvalidator = new EventFiringVpcRouteStateInvalidator( );

  private static NetworkInfoSource cacheSource( ) {
    final Supplier<Iterable<VmInstanceNetworkView>> instanceSupplier = Suppliers.memoize( instanceCache );
    final Supplier<Iterable<NetworkGroupNetworkView>> securityGroupSupplier = Suppliers.memoize( securityGroupCache );
    final Supplier<Iterable<VpcNetworkView>> vpcSupplier = Suppliers.memoize( vpcCache );
    final Supplier<Iterable<SubnetNetworkView>> subnetSupplier = Suppliers.memoize( subnetCache );
    final Supplier<Iterable<DhcpOptionSetNetworkView>> dhcpOptionsSupplier = Suppliers.memoize( dhcpOptionsCache );
    final Supplier<Iterable<NetworkAclNetworkView>> networkAclSupplier = Suppliers.memoize( networkAclCache );
    final Supplier<Iterable<RouteTableNetworkView>> routeTableSupplier = Suppliers.memoize( routeTableCache );
    final Supplier<Iterable<InternetGatewayNetworkView>> internetGatewaySupplier = Suppliers.memoize( internetGatewayCache );
    final Supplier<Iterable<NetworkInterfaceNetworkView>> networkInterfaceSupplier = Suppliers.memoize( networkInterfaceCache );
    final Supplier<Iterable<NatGatewayNetworkView>> natGatewaySupplier = Suppliers.memoize( natGatewayCache );
    return new NetworkInfoSource( ) {
      @Override public Iterable<VmInstanceNetworkView> getInstances( ) { return instanceSupplier.get( ); }
      @Override public Iterable<NetworkGroupNetworkView> getSecurityGroups( ) { return securityGroupSupplier.get( ); }
      @Override public Iterable<VpcNetworkView> getVpcs( ) { return vpcSupplier.get( ); }
      @Override public Iterable<SubnetNetworkView> getSubnets( ) { return subnetSupplier.get( ); }
      @Override public Iterable<DhcpOptionSetNetworkView> getDhcpOptionSets( ) { return dhcpOptionsSupplier.get( ); }
      @Override public Iterable<NetworkAclNetworkView> getNetworkAcls( ) { return networkAclSupplier.get( ); }
      @Override public Iterable<RouteTableNetworkView> getRouteTables( ) { return routeTableSupplier.get( ); }
      @Override public Iterable<InternetGatewayNetworkView> getInternetGateways( ) { return internetGatewaySupplier.get( ); }
      @Override public Iterable<NetworkInterfaceNetworkView> getNetworkInterfaces( ) { return networkInterfaceSupplier.get( ); }
      @Override public Iterable<NatGatewayNetworkView> getNatGateways( ) { return natGatewaySupplier.get( ); }
      @Override public Map<String, Iterable<? extends NetworkView.VersionedNetworkView>> getView() {
        return ImmutableMap.<String, Iterable<? extends NetworkView.VersionedNetworkView>>builder( )
            .put( "instance", getInstances( ) )
            .put( "security-group", getSecurityGroups( ) )
            .put( "vpc", getVpcs( ) )
            .put( "subnet", getSubnets( ) )
            .put( "dhcp-option-set", getDhcpOptionSets( ) )
            .put( "network-acl", getNetworkAcls( ) )
            .put( "route-table", getRouteTables( ) )
            .put( "internet-gateway", getInternetGateways( ) )
            .put( "network-interface", getNetworkInterfaces( ) )
            .put( "nat-gateway", getNatGateways( ) )
            .build( );
      }
    };
  }

  public static void requestNetworkInfoBroadcast( ) {
    final long requestedTime = System.currentTimeMillis( );
    final Callable<Void> broadcastRequest = new Callable<Void>( ) {
      @SuppressWarnings( "unused" )
      @Override
      public Void call( ) throws Exception {
        boolean shouldBroadcast = false;
        boolean shouldRetryWithDelay = false;
        try ( final LockResource lock = LockResource.lock( lastBroadcastTimeLock ) ) {
          final long currentTime = System.currentTimeMillis( );
          final long lastBroadcast = lastBroadcastTime.get( );
          if ( requestedTime >= lastBroadcast &&
              lastBroadcast + TimeUnit.SECONDS.toMillis( NetworkGroups.MIN_BROADCAST_INTERVAL ) < currentTime &&
              activeBroadcastSemaphore.availablePermits( ) > 0 ) {
            if ( lastBroadcastTime.compareAndSet( lastBroadcast, currentTime ) ) {
              shouldBroadcast = true;
            } else { // re-evaluate
              broadcastTask( this );
            }
          } else if ( requestedTime >= lastBroadcastTime.get() ) {
            shouldRetryWithDelay = true;
          }
        }
        if ( shouldBroadcast ) {
          try {
            broadcastNetworkInfo( );
          } catch( Exception e ) {
            logger.error( "Error broadcasting network information", e );
          }
        } else if ( shouldRetryWithDelay && !Bootstrap.isShuttingDown( ) ) {
          Thread.sleep( 100 ); // pause and re-evaluate to allow for min time between broadcasts
          broadcastTask( this );
        }
        return null;
      }
    };
    broadcastTask( broadcastRequest );
  }

  private static void broadcastTask( Callable<Void> task ) {
    Threads.enqueue( Eucalyptus.class, NetworkInfoBroadcaster.class, 5, task );
  }

  @SuppressWarnings( { "UnnecessaryQualifiedReference", "WeakerAccess", "unused" } )
  static void broadcastNetworkInfo( ) {
    try ( final SemaphoreResource semaphore = SemaphoreResource.acquire( activeBroadcastSemaphore ) ) {
      // populate with info directly from configuration
      final Option<NetworkConfiguration> networkConfiguration = NetworkConfigurations.getNetworkConfiguration( );
      final List<Cluster> clusters = Clusters.list( );
      final List<Cluster> otherClusters = Clusters.listDisabled( );

      final NetworkInfoSource source = cacheSource( );
      final Set<String> dirtyPublicAddresses = PublicAddresses.dirtySnapshot( );
      final Set<RouteKey> invalidStateRoutes = Sets.newHashSetWithExpectedSize( 50 );
      final int sourceFingerprint = fingerprint( source, clusters, dirtyPublicAddresses, NetworkGroups.NETWORK_CONFIGURATION );
      final BNetworkInfo info = buildNetworkConfiguration(
              networkConfiguration,
              source,
              BaseEncoding.base16( ).lowerCase( ).encode( Ints.toByteArray( sourceFingerprint ) ),
              Suppliers.ofInstance( clusters ),
              Suppliers.ofInstance( otherClusters ),
              () -> Topology.lookup( Eucalyptus.class ).getInetAddress( ).getHostAddress( ),
              NetworkConfigurations::loadSystemNameservers,
              dirtyPublicAddresses,
              invalidStateRoutes
          );

      if ( !invalidStateRoutes.isEmpty( ) ) {
        vpcRouteStateInvalidator.accept( invalidStateRoutes );
      }

      Applicators.apply( clusters, info );

    } catch ( ApplicatorException e ) {
      logger.error( "Error during network broadcast", e );
    }
  }

  @SuppressWarnings( "serial" )
  private static int fingerprint(
      final NetworkInfoSource source,
      final List<Cluster> clusters,
      final Set<String> dirtyPublicAddresses,
      final String networkConfiguration
  ) {
    final HashFunction hashFunction = goodFastHash( 32 );
    final Hasher hasher = hashFunction.newHasher( );
    final Funnel<VersionedNetworkView> versionedItemFunnel = new Funnel<VersionedNetworkView>() {
      @SuppressWarnings( "NullableProblems" )
      @Override
      public void funnel( final VersionedNetworkView o, final PrimitiveSink primitiveSink ) {
        primitiveSink.putString( o.getId( ), StandardCharsets.UTF_8 );
        primitiveSink.putChar( '=' );
        primitiveSink.putInt( o.getVersion( ) );
      }
    };
    for ( final Map.Entry<String,Iterable<? extends VersionedNetworkView>> entry : source.getView( ).entrySet( ) ) {
      hasher.putString( entry.getKey( ), StandardCharsets.UTF_8 );
      for ( final VersionedNetworkView item : entry.getValue( ) ) {
        hasher.putObject( item, versionedItemFunnel );
      }
    }
    hasher.putString( Joiner.on( ',' ).join( Sets.newTreeSet( Iterables.transform( clusters, HasName.GET_NAME ) ) ), StandardCharsets.UTF_8 );
    hasher.putString( Joiner.on( ',' ).join( Sets.newTreeSet( dirtyPublicAddresses ) ), StandardCharsets.UTF_8 );
    hasher.putInt( networkConfiguration.hashCode( ) );
    return hasher.hash( ).asInt( );
  }

  private static boolean validInstanceMetadata( final VmInstance instance) {
    return !Strings.isNullOrEmpty( instance.getPrivateAddress() ) &&
        !VmNetworkConfig.DEFAULT_IP.equals( instance.getPrivateAddress() ) &&
        !instance.getNetworkGroups().isEmpty( ) &&
        !Strings.isNullOrEmpty( VmInstances.toNodeHost( ).apply( instance ) );
  }

  private static String address( final String hostOrAddress ) {
    try {
      return InetAddress.getByName( hostOrAddress ).getHostAddress( );
    } catch ( UnknownHostException e ) {
      return hostOrAddress;
    }
  }

  private static Array<IPPermissionNetworkView> explodePermissions( NetworkRule networkRule ) {
    final Set<IPPermissionNetworkView> rules = Sets.newLinkedHashSet( );

    // Rules without a protocol number are pre-VPC support
    if ( networkRule.getProtocolNumber( ) != null ) {
      Option<NetworkRule.Protocol> protocol = Option.of( networkRule.getProtocol() );
      Integer protocolNumber = networkRule.getProtocolNumber();
      Option<Integer> fromPort = protocol.map( p -> p.extractLowPort( networkRule ) );
      Option<Integer> toPort = protocol.map( p -> p.extractHighPort( networkRule ) );
      Option<Integer> icmpType = protocol.map( p -> p.extractIcmpType( networkRule ) );
      Option<Integer> icmpCode = protocol.map( p -> p.extractIcmpCode( networkRule ) );

      rules.addAll( Array.ofAll( networkRule.getNetworkPeers( ) ).<IPPermissionNetworkView>map( peer ->
        ImmutableNetworkView.IPPermissionNetworkView.builder( )
            .protocol( protocolNumber )
            .fromPort( fromPort )
            .toPort( toPort )
            .icmpType(icmpType )
            .icmpCode( icmpCode )
            .groupId( Option.of( peer.getGroupId() ) )
            .groupOwnerAccountNumber( Option.of( peer.getUserQueryKey() ) )
            .o( )
      ).toJavaList( ) );
      rules.addAll( Array.ofAll( networkRule.getIpRanges( ) ).<IPPermissionNetworkView>map( cidr ->
        ImmutableNetworkView.IPPermissionNetworkView.builder( )
            .protocol( protocolNumber )
            .fromPort( fromPort )
            .toPort( toPort )
            .icmpType(icmpType )
            .icmpCode( icmpCode )
            .cidr( Option.of( cidr ) )
            .o( )
      ).toJavaList( ) );
    }

    return Array.ofAll( rules );
  }

  private static CompatPredicate<RouteNetworkView> activeRoutePredicate(
      final Iterable<InternetGatewayNetworkView> internetGateways,
      final Iterable<NatGatewayNetworkView> natGateways,
      final Iterable<VmInstanceNetworkView> instances,
      final Iterable<NetworkInterfaceNetworkView> networkInterfaces,
      final Set<RouteKey> invalidRoutes
  ) {
    final Map<String,String> networkInterfaceAndInstanceIds = Stream.ofAll( networkInterfaces )
        .filter( eni -> eni.instanceId( ).isDefined( ) )
        .foldLeft( Maps.newHashMapWithExpectedSize( 500 ), ( map, eni ) -> {
          map.put( eni.getId( ), eni.instanceId( ).get( ) );
          return map;
        } );
    final CompatFunction<VersionedNetworkView,String> id = VersionedNetworkView::getId;
    final Set<String> instanceIds = Stream.ofAll( instances )
        .filter( vm -> vm.state( )==VmInstance.VmState.RUNNING )
        .map( id )
        .toJavaSet( );
    final Set<String> internetGatewayIds = Stream.ofAll( internetGateways )
        .filter( ig -> ig.vpcId( ).isDefined( ) ) // only attached gateways have active routes
        .map( id )
        .toJavaSet( );
    final Set<String> natGatewayIds = Stream.ofAll( natGateways )
        .filter( ng -> ng.state() == NatGateway.State.available )
        .map( id )
        .toJavaSet( );
    return routeNetworkView -> {
        @SuppressWarnings( "ConstantConditions" )
        final boolean active =
            (!routeNetworkView.gatewayId().isDefined() && !routeNetworkView.networkInterfaceId().isDefined() &&
                !routeNetworkView.instanceId().isDefined() && !routeNetworkView.natGatewayId().isDefined() ) || // local route
                internetGatewayIds.contains( routeNetworkView.gatewayId().getOrNull() ) ||
                natGatewayIds.contains( routeNetworkView.natGatewayId().getOrNull() ) ||
                ( networkInterfaceAndInstanceIds.containsKey( routeNetworkView.networkInterfaceId().getOrNull() ) &&
                    instanceIds.contains( networkInterfaceAndInstanceIds.get( routeNetworkView.networkInterfaceId().getOrNull() ) ) );
        if ( active != routeNetworkView.active() ) {
          invalidRoutes.add( new RouteKey( routeNetworkView.routeTableId( ), routeNetworkView.destinationCidr( ) ) );
        }
        return active;
    };
  }

  @SuppressWarnings( "ConstantConditions" )
  private static CompatPredicate<Cluster> uniquePartitionPredicate( ) {
    final Set<String> partitionNames = Sets.newHashSet( );
    return cluster -> partitionNames.add( cluster.getPartition( ) );
  }

  private static BNetworkInfo modeClean( final boolean vpc, final BNetworkInfo networkInfo ) {
    final Set<String> removedResources = Sets.newHashSet( );
    BNetworkInfo info;
    if ( vpc ) {
      info = modeCleanVpc( networkInfo, removedResources );
    } else {
      info = modeCleanEdge( networkInfo, removedResources );
    }
    final Lens<BNetworkInfo,Array<BNICluster>> clusterArrayLens = clusterArrayLens( );
    final Lens<BNICluster,Array<BNINode>> nodeArrayLens = nodeArrayLens( );
    return clusterArrayLens.modify( clusterArray -> clusterArray.map(
        nodeArrayLens.modify( nodeArray ->
            nodeArray.map( node ->
                BNI.node( ).from( node ).instanceIds( node.instanceIds( ).removeAll( removedResources ) ).o( ) ) )
    ) ).andThen( instancesLens( ).modify( instanceArray ->
        instanceArray.map( instance ->
            BNI.instance( ).from( instance ).securityGroups( instance.securityGroups( ).removeAll( removedResources ) ).o( ) )
    ) ).apply( info );
  }

  /**
   *
   * Remove any EC2-Classic platform info:
   * - instances running without a vpc or without any network interfaces
   */
  @SuppressWarnings( "ConstantConditions" )
  private static BNetworkInfo modeCleanVpc( final BNetworkInfo info, final Set<String> removedResources ) {
    final CompatPredicate<BNIInstance> goodInstances =
        instance -> instance.vpc( ).isDefined( ) && !instance.networkInterfaces( ).isEmpty( );
    final Array<BNIInstance> removedInstances = info.instances( ).filter( goodInstances.negate( ) );
    for ( BNIHasName named : removedInstances ) { removedResources.add( named.name( ) ); }
    return instancesLens( ).modify( instances -> info.instances( ).filter( goodInstances ) ).apply( info );
  }

  /**
   * Remove any EC2-VPC platform info:
   * - instances running in a a vpc
   * - instances running with network interfaces attached
   * - security groups using egress rules
   * - security groups using ingress rules with invalid protocols
   * - vpcs
   * - internet gateways
   * - dhcp option sets
   */
  @SuppressWarnings( "ConstantConditions" )
  private static BNetworkInfo modeCleanEdge( final BNetworkInfo info, final Set<String> removedResources  ) {
    final CompatPredicate<BNIInstance> goodInstances =
        instance -> !instance.vpc( ).isDefined( ) && instance.networkInterfaces( ).isEmpty( );
    final Array<BNIInstance> removedInstances = info.instances( ).filter( goodInstances.negate( ) );

    final CompatPredicate<BNISecurityGroup> goodGroups = securityGroup -> {
      boolean good = securityGroup.egressRules( ).isEmpty( ) || securityGroup.egressRules( ).get( ).rules( ).isEmpty( );
      if ( good && securityGroup.ingressRules( ).isDefined( ) ) {
        good = !securityGroup.ingressRules().get().rules().exists( permission ->
            !NetworkRule.isValidProtocol( permission.protocol( ) ) );
      }
      return good;
    };
    final Array<BNISecurityGroup> removedGroups = info.securityGroups( ).filter( goodGroups.negate( ) );

    removedResources.addAll( Array.<BNIHasName>narrow( info.dhcpOptionSets( ) )
        .appendAll( info.internetGateways( ) )
        .appendAll( info.vpcs( ) )
        .appendAll( removedGroups )
        .appendAll( removedInstances )
        .map( BNIHasName::name )
        .toJavaList( ) );

    return dhcpOptionSetsLens( ).modify( dhcpOptionSets -> Array.empty( ) )
        .andThen( instancesLens( ).modify( instances -> info.instances( ).filter( goodInstances ) ) )
        .andThen( internetGatewaysLens( ).modify( internetGateways -> Array.empty( ) ) )
        .andThen( securityGroupLens( ).modify( groups -> groups.filter( goodGroups ) ) )
        .andThen( vpcsLens( ).modify( vpcs -> Array.empty( ) ) )
        .apply( info );
  }

  /**
   * All state used for building network configuration must be passed in (and be part of the fingerprint)
   */
  @SuppressWarnings( { "OptionalUsedAsFieldOrParameterType", "ConstantConditions", "WeakerAccess" } )
  static BNetworkInfo buildNetworkConfiguration( final Option<NetworkConfiguration> configuration,
                                                 final NetworkInfoBroadcaster.NetworkInfoSource networkInfoSource,
                                                 final String version,
                                                 final Supplier<List<Cluster>> clusterSupplier,
                                                 final Supplier<List<Cluster>> otherClusterSupplier,
                                                 final Supplier<String> clcHostSupplier,
                                                 final Function<List<String>,List<String>> systemNameserverLookup,
                                                 final Set<String> dirtyPublicAddresses,
                                                 final Set<RouteKey> invalidStateRoutes /*out*/ ) {
    final String mode = configuration.flatMap( NetworkConfiguration::mode ).getOrElse( "EDGE" );
    final boolean vpcmido = "VPCMIDO".equals( mode );
    final boolean managed = "MANAGED".equals( mode ) || "MANAGED-NOVLAN".equals( mode );
    final Iterable<Cluster> clusters = vpcmido ?
        Lists.newArrayList( Iterables.filter( Iterables.concat( clusterSupplier.get( ), otherClusterSupplier.get( ) ), NetworkInfoBroadcaster.uniquePartitionPredicate( ) ) ) :
        clusterSupplier.get( );
    final Option<NetworkConfiguration> networkConfiguration = configuration.map( config ->
        NetworkConfigurations.explode( config, Array.ofAll(clusters).map(Cluster::getPartition) )
    );
    final ImmutableBNetworkInfo.Builder info = networkConfiguration
        .map( TypeMappers.lookupF( NetworkConfiguration.class, ImmutableBNetworkInfo.Builder.class ) )
        .getOrElse( networkInfo( ).configuration( BNI.configuration( ).o( ) ) );
    info.setValueVersion( version );
    final ImmutableBNIConfiguration.Builder bniConfiguration = BNI.configuration( )
        .addAllProperties( Array.narrow( info.o( ).configuration( ).simpleProperties( ) ) );

    // populate clusters
    BNIClusters bniClusters = BNI.clusters()
        .name( "clusters" )
        .setIterableClusters( Array.ofAll( clusters ).flatMap( new CompatFunction<Cluster,Option<BNICluster>>( ) {
          @SuppressWarnings( "ConstantConditions" )
          @Override
          public Option<BNICluster> apply( final Cluster cluster ){
            NetworkConfigurationApi.Cluster configCluster = null;
            if ( networkConfiguration.isDefined( ) ) {
              for( NetworkConfigurationApi.Cluster cc : networkConfiguration.get().clusters() ) {
                if ( Objects.equals( cluster.getPartition(), cc.name().getOrNull() ) ) {
                  configCluster = cc;
                  break;
                }
              }
            }
            return Option.of( configCluster!=null && ( vpcmido || configCluster.subnet().isDefined( ) ) ?
                BNI.cluster( )
                    .name( configCluster.name( ).get( ) )
                    .setValueSubnet( vpcmido ?
                        subnet( )
                            .name( "172.31.0.0" )
                            .property( property( "subnet", "172.31.0.0" ) )
                            .property( property( "netmask", "255.255.0.0" ) )
                            .property( property( "gateway", "172.31.0.1" ) )
                            .o( ) :
                        subnet( )
                            .name( configCluster.subnet( ).get( ).subnet( ).get( ) ) // broadcast name is always the subnet value
                            .property( property("subnet", configCluster.subnet( ).get( ).subnet( ).get( ) ) )
                            .property( property("netmask", configCluster.subnet( ).get( ).netmask( ).get( ) ) )
                            .property( property("gateway", configCluster.subnet( ).get( ).gateway( ).get( ) ) )
                            .o( )
                    )
                    .property( property( "enabledCCIp", address(cluster.getHostName()) ) )
                    .property( property( "macPrefix", configCluster.macPrefix().get( ) ) )
                    .property( vpcmido ?
                        property( "privateIps", "172.31.0.5" ) :
                        property( ).name( "privateIps" ).values( configCluster.privateIps( ) ).o( ) )
                    .property( nodes( )
                        .name( "nodes" )
                        .setIterableNodes( Stream.ofAll( cluster.getNodeMap().values() ).map( nodeInfo -> node( ).name( nodeInfo.getName( ) ).o( ) ) )
                        .o( ) )
                    .o( )
              :
            configCluster!=null && managed ? BNI.cluster( )
                .name( configCluster.name().get() )
                .property( property("enabledCCIp", address(cluster.getHostName()) ) )
                .property( property("macPrefix", configCluster.macPrefix().get( ) ) )
                .property( nodes( )
                    .name( "nodes" )
                    .setIterableNodes( Stream.ofAll( cluster.getNodeMap().values() ).map( nodeInfo -> node( ).name( nodeInfo.getName( ) ).o( ) ) )
                    .o( ) )
                .o( )
              :
            null );
          } }
        ) )
        .o( );

    // populate dynamic properties
    Array<String> dnsServers = networkConfiguration.isDefined( ) && !networkConfiguration.get().instanceDnsServers().isEmpty() ?
        networkConfiguration.get().instanceDnsServers() :
        Array.ofAll( systemNameserverLookup.apply(Lists.newArrayList("127.0.0.1")) );
    bniConfiguration
        .property( property( "enabledCLCIp", clcHostSupplier.get() ) )
        .property( property( "instanceDNSDomain", networkConfiguration.isDefined() && !Strings.isNullOrEmpty(networkConfiguration.get().instanceDnsDomain().getOrNull()) ?
            networkConfiguration.get().instanceDnsDomain().get( ) :
            com.eucalyptus.util.Strings.trimPrefix(".",VmInstances.INSTANCE_SUBDOMAIN + ".internal") ) )
        .property( property( ).name("instanceDNSServers").values(dnsServers).o( ) );


    boolean hasEdgePublicGateway = networkConfiguration.isDefined() && networkConfiguration.get().publicGateway().isDefined( );
    if ( hasEdgePublicGateway ) {
      bniConfiguration
          .property( property( "publicGateway", networkConfiguration.get().publicGateway().get() ) );
    }

    Iterable<NetworkView.VmInstanceNetworkView> instances = Iterables.filter(
        networkInfoSource.getInstances(),
        instance ->!TORNDOWN.contains(instance.state()) && !instance.omit() );

    // populate nodes
    ImmutableBNIClusters.Builder clustersBuilder = BNI.clusters( ).name( bniClusters.name( ) );
    Map<List<String>,Collection<String>> nodeInstanceMap = Stream.ofAll( instances ).foldLeft(
        HashMultimap.<List<String>,String>create( ),
        ( map, instance ) -> {
          map.put( Lists.newArrayList( instance.partition(), instance.node().get() ), instance.getId( ) );
          return map;
        }
    ).asMap( );
    for ( BNICluster cluster : bniClusters.clusters( ) ) {
      final ImmutableBNICluster.Builder clusterBuilder = BNI.cluster( )
          .name( cluster.name( ) )
          .subnet( cluster.subnet( ) )
          .properties( Array.narrow( cluster.simpleProperties( ) ) );
      cluster.nodes( ).map( node ->
        clusterBuilder.property( nodes( )
            .name( "nodes" )
            .setIterableNodes( node.nodes( ).map( bniNode -> {
              Collection<String> instanceIds = nodeInstanceMap.getOrDefault( Lists.newArrayList( cluster.name( ), bniNode.name( ) ), Collections.emptyList( ) );
              return !instanceIds.isEmpty( ) ?
                  BNI.node( ).from( bniNode ).setIterableInstanceIds( instanceIds ).o( ) :
                  bniNode;
            } ) ).o( ) )
      );
      clustersBuilder.cluster( clusterBuilder.o( ) );
    }
    for ( BNIMidonet midonet : info.o( ).configuration( ).midonet( ) ) {
      bniConfiguration.property( midonet );
    }
    for ( BNISubnets subnets : info.o( ).configuration( ).subnets( ) ) {
      bniConfiguration.property( subnets );
    }
    for ( BNIManagedSubnets subnets : info.o( ).configuration( ).managedSubnets( ) ) {
      bniConfiguration.property( subnets );
    }
    bniConfiguration.property( clustersBuilder.o( ) );
    info.configuration( bniConfiguration.o( ) );

    // populate vpcs
    Iterable<NetworkView.VpcNetworkView> vpcs = networkInfoSource.getVpcs();
    Iterable<NetworkView.SubnetNetworkView> subnets = networkInfoSource.getSubnets();
    Iterable<NetworkView.NetworkAclNetworkView> networkAcls = networkInfoSource.getNetworkAcls();
    Iterable<NetworkView.RouteTableNetworkView> routeTables = networkInfoSource.getRouteTables();
    Iterable<NetworkView.InternetGatewayNetworkView> internetGateways = networkInfoSource.getInternetGateways();
    Iterable<NetworkView.NatGatewayNetworkView> natGateways = networkInfoSource.getNatGateways();
    Iterable<NetworkView.NetworkInterfaceNetworkView> networkInterfaces = networkInfoSource.getNetworkInterfaces();
    Set<String> activeInstances = Stream.ofAll( instances ).foldLeft( Sets.newHashSetWithExpectedSize( 500 ), ( instanceIds, inst ) -> {
        instanceIds.add( inst.getId() );
        return instanceIds;
    } );
    Set<String> activeVpcs = Stream.ofAll( networkInterfaces ).foldLeft(
        Sets.newHashSetWithExpectedSize( 500 ),
        ( vpcIds, networkInterface ) -> {
          if ( networkInterface.instanceId( ).isDefined( ) && activeInstances.contains( networkInterface.instanceId().get( ) ) ) {
            vpcIds.add( networkInterface.vpcId() );
          }
          return vpcIds;
    } );
    natGateways.forEach( natGateway -> {
        if ( natGateway.state() == NatGateway.State.available ) activeVpcs.add( natGateway.vpcId() );
    } );
    Set<String> activeDhcpOptionSets = Stream.ofAll( vpcs ).foldLeft(
        Sets.newHashSetWithExpectedSize( 500 ),
        ( dhcpOptionSetIds, vpc ) -> {
          if ( activeVpcs.contains( vpc.getId() ) && vpc.dhcpOptionSetId().isDefined() ) {
            dhcpOptionSetIds.add( vpc.dhcpOptionSetId().get( ) );
          }
          return dhcpOptionSetIds;
    } );
    Map<String,Collection<String>> vpcIdToInternetGatewayIds = Stream.ofAll( internetGateways ).foldLeft(
        ArrayListMultimap.<String,String>create( ),
        ( map, internetGateway ) -> {
          if ( internetGateway.vpcId().isDefined() ) map.put( internetGateway.vpcId().get(), internetGateway.getId( ) );
          return map;
    } ).asMap( );
    CompatPredicate<RouteNetworkView> activeRoutePredicate =
        NetworkInfoBroadcaster.activeRoutePredicate( internetGateways, natGateways, instances, networkInterfaces, invalidStateRoutes );
    info.addAllVpcs( Stream.ofAll( vpcs ).filter( vpc -> activeVpcs.contains(vpc.getId( ) ) ).map( vpc ->
      BNI.vpc( )
          .name( vpc.getId() )
          .ownerId( vpc.ownerAccountNumber() )
          .cidr( vpc.cidr() )
          .dhcpOptionSet( vpc.dhcpOptionSetId() )
          .addAllSubnets( Stream.ofAll( subnets ).filter( subnet -> subnet.vpcId().equals(vpc.getId()) ).map( subnet ->
            vpcSubnet( )
              .name( subnet.getId() )
              .ownerId( subnet.ownerAccountNumber() )
              .cidr( subnet.cidr() )
              .cluster( subnet.availabilityZone() )
              .networkAcl( subnet.networkAcl( ) )
              .routeTable(
                  tryFind( routeTables, routeTable -> routeTable.subnetIds( ).contains( subnet.getId() ) ).or(
                      Iterables.find( routeTables, routeTable -> routeTable.main( ) && routeTable.vpcId( ).equals(vpc.getId() ) ) ).getId() )
              .o( ) ) )
          .addAllNetworkAcls( Stream.ofAll( networkAcls ).filter( networkAcl -> networkAcl.vpcId().equals( vpc.getId() ) ).map( networkAcl ->
            BNI.networkAcl( )
              .name( networkAcl.getId() )
              .ownerId( networkAcl.ownerAccountNumber() )
              .setValueIngressEntries( networkAclEntries( networkAcl.ingressRules( ).map(TypeMappers.lookupF( NetworkAclEntryNetworkView.class, BNINetworkAclEntry.class ) ) ) )
              .setValueEgressEntries( networkAclEntries( networkAcl.egressRules().map(TypeMappers.lookupF( NetworkAclEntryNetworkView.class, BNINetworkAclEntry.class ) ) ) )
              .o( ) ) )
          .addAllRouteTables( Stream.ofAll( routeTables ).filter( routeTable -> routeTable.vpcId( ).equals( vpc.getId() ) ).map( routeTable ->
            BNI.routeTable( )
              .name( routeTable.getId() )
              .ownerId( routeTable.ownerAccountNumber( ) )
              .addAllRoutes( routeTable.routes().filter(activeRoutePredicate).map(TypeMappers.lookupF( RouteNetworkView.class, BNIRoute.class ) ) )
              .o( )
          ) )
          .addAllNatGateways( Stream.ofAll( natGateways ).filter( natGateway -> natGateway.vpcId().equals( vpc.getId() ) && natGateway.state() == NatGateway.State.available ).map( natGateway ->
            BNI.natGateway( )
                .name( natGateway.getId() )
                .ownerId( natGateway.ownerAccountNumber() )
                .vpc( natGateway.vpcId() )
                .subnet( natGateway.subnetId() )
                .macAddress( natGateway.macAddress() )
                .publicIp( natGateway.publicIp( ).isDefined() &&
                    !natGateway.publicIp( ).get( ).equals(VmNetworkConfig.DEFAULT_IP) &&
                    !dirtyPublicAddresses.contains(natGateway.publicIp( ).get( )) ?
                    natGateway.publicIp( ) :
                    Option.none( ) )
                .privateIp( natGateway.privateIp() )
                .o( ) ) )
          .addAllInternetGateways( MoreObjects.firstNonNull( vpcIdToInternetGatewayIds.get( vpc.getId( ) ), Lists.newArrayList( ) ) )
          .o( ) ) );
    Stream.ofAll( vpcs ).filter( vpc -> !activeVpcs.contains(vpc.getId()) ).forEach( vpc -> // processing for any inactive vpcs
      Stream.ofAll( routeTables ).filter( routeTable -> routeTable.vpcId( ).equals( vpc.getId() ) ).forEach( routeTable ->
        CollectionUtils.each( routeTable.routes( ), activeRoutePredicate ) )
    );

    // populate instances
    Map<String,Collection<NetworkInterfaceNetworkView>> instanceIdToNetworkInterfaces = Stream.ofAll( networkInterfaces ).foldLeft(
        ArrayListMultimap.<String,NetworkInterfaceNetworkView>create( ),
        ( map, networkInterface ) -> {
          if ( networkInterface.instanceId( ).isDefined( ) &&
              networkInterface.state( ) != NetworkInterface.State.available &&
              networkInterface.attachmentStatus() != NetworkInterfaceAttachment.Status.detaching &&
              networkInterface.attachmentStatus() != NetworkInterfaceAttachment.Status.detached
              ) {
            map.put( networkInterface.instanceId().get(), networkInterface );
          }
          return map;
    }).asMap( );
    info.addAllInstances( Stream.ofAll( instances ).map( instance ->
      BNI.instance( )
          .name( instance.getId() )
          .ownerId( instance.ownerAccountNumber() )
          .vpc( instance.vpcId( ) )
          .subnet( instance.subnetId() )
          .macAddress( instance.macAddress( ) )
          .publicIp( instance.publicAddress( ).isDefined() &&
              !instance.publicAddress( ).get( ).equals(VmNetworkConfig.DEFAULT_IP) &&
              !dirtyPublicAddresses.contains(instance.publicAddress( ).get( )) ?
              instance.publicAddress( ) :
              Option.none( ) )
          .privateIp( instance.privateAddress( ) )
          .addAllSecurityGroups( instance.securityGroupIds( ) )
          .addAllNetworkInterfaces( Stream.ofAll( instanceIdToNetworkInterfaces.getOrDefault( instance.getId(), Collections.emptyList( ) ) ).map( networkInterface ->
        BNI.networkInterface( )
            .name( networkInterface.getId() )
            .ownerId( networkInterface.ownerAccountNumber( ) )
            .deviceIndex( networkInterface.deviceIndex( ).get( ) )
            .attachmentId( networkInterface.attachmentId( ).get( ) )
            .macAddress( networkInterface.macAddress( ) )
            .privateIp( networkInterface.privateIp( ) )
            .publicIp( networkInterface.publicIp( ).isDefined() &&
                !networkInterface.publicIp( ).get( ).equals(VmNetworkConfig.DEFAULT_IP) &&
                !dirtyPublicAddresses.contains(networkInterface.publicIp( ).get( )) ?
                networkInterface.publicIp( ) :
                Option.none( ) )
            .sourceDestCheck( networkInterface.sourceDestCheck( ))
            .vpc( networkInterface.vpcId( ) )
            .subnet( networkInterface.subnetId( ) )
            .addAllSecurityGroups( networkInterface.securityGroupIds( ) )
            .o( )
      ) ).o( )
    ) );

    // populate dhcp option sets
    Iterable<DhcpOptionSetNetworkView> dhcpOptionSets = networkInfoSource.getDhcpOptionSets( );
    info.addAllDhcpOptionSets( Stream.ofAll( dhcpOptionSets ).filter( dhcpOptionSet -> activeDhcpOptionSets.contains( dhcpOptionSet.getId( ) ) ).map( dhcpOptionSet ->
      BNI.dhcpOptionSet( )
          .name( dhcpOptionSet.getId() )
          .ownerId( dhcpOptionSet.ownerAccountNumber() )
          .addAllProperties( Array.narrow( dhcpOptionSet.options( ).map( option -> {
            if ( "domain-name-servers".equals(option.key( )) && "AmazonProvidedDNS".equals( Iterables.get( option.values( ), 0, null ) ) ) {
              return property( ).name( "domain-name-servers" ).addAllValues( dnsServers ).o( );
            } else {
              return property( ).name( option.key( ) ).addAllValues( option.values( ) ).o( );
            }
          } ) ) )
          .o( )
    ) );

    // populate internet gateways
    info.addAllInternetGateways( Stream.ofAll( internetGateways ).filter( gateway ->
      activeVpcs.contains(gateway.vpcId().get())
    ).map( internetGateway ->
      BNI.internetGateway( )
          .name( internetGateway.getId() )
          .ownerId( internetGateway.ownerAccountNumber() )
          .o( )
    ) );

    // populate security groups
    Iterable<NetworkGroupNetworkView> groups = networkInfoSource.getSecurityGroups();
    Set<String> securityGroupIds = Stream.ofAll( groups ).foldLeft(
        Sets.newHashSetWithExpectedSize( 1000 ),
        ( groupIds, securityGroup ) -> {
          groupIds.add( securityGroup.getId() );
          return groupIds;
    } );
    Set<String> activeSecurityGroups = Stream.ofAll( instances ).foldLeft(
        Sets.newHashSetWithExpectedSize( 1000 ),
        ( activeGroups, instance ) -> {
          activeGroups.addAll( instance.securityGroupIds( ).asJava( ) );
          return activeGroups;
    } );
    Stream.ofAll( networkInterfaces ).foldLeft( activeSecurityGroups, ( activeGroups, networkInterface ) -> {
        if ( networkInterface.instanceId().isDefined() && activeInstances.contains( networkInterface.instanceId( ).get() ) ) {
          activeGroups.addAll( networkInterface.securityGroupIds().asJava( ) );
        }
        return activeGroups;
    } );
    Stream.ofAll( groups ).foldLeft( activeSecurityGroups, ( activeGroups, securityGroup ) -> {
        Iterables.concat( securityGroup.egressPermissions(), securityGroup.ingressPermissions() ).forEach( permission -> {
            if ( permission.groupId().isDefined( ) && !securityGroup.getId().equals( permission.groupId().get( ) ) &&
                securityGroupIds.contains( permission.groupId().get( ) ) &&
                (!securityGroup.vpcId().isDefined() || activeVpcs.contains(securityGroup.vpcId().get()))) {
              activeGroups.add( permission.groupId().get() );
            }
          } );
        return activeGroups;
    } );
    info.addAllSecurityGroups( Stream.ofAll( groups ).filter( group -> activeSecurityGroups.contains( group.getId() ) ).map( group ->
      securityGroup( )
          .name( group.getId() )
          .ownerId( group.ownerAccountNumber() )
          .setValueIngressRules( securityGroupRules( )
              .addAllRules( group.ingressPermissions( )
                  .filter( ipPermission ->
                      !ipPermission.groupId().isDefined() || activeSecurityGroups.contains( ipPermission.groupId().get() ) )
                  .map( ipPermission ->
                    securityGroupIpPermission( )
                      .protocol( ipPermission.protocol() )
                      .fromPort( ipPermission.fromPort() )
                      .toPort( ipPermission.toPort() )
                      .icmpType( ipPermission.icmpType() )
                      .icmpCode( ipPermission.icmpCode() )
                      .groupId( ipPermission.groupId( ) )
                      .groupOwnerId( ipPermission.groupOwnerAccountNumber() )
                      .cidr( ipPermission.cidr() )
                      .o( ) ) )
              .o( ) )
          .setValueEgressRules( securityGroupRules( )
              .addAllRules( group.egressPermissions()
                  .filter( ipPermission ->
                      !ipPermission.groupId().isDefined() || activeSecurityGroups.contains( ipPermission.groupId().get() ) )
                  .map( ipPermission ->
                    securityGroupIpPermission( )
                      .protocol( ipPermission.protocol() )
                      .fromPort( ipPermission.fromPort() )
                      .toPort( ipPermission.toPort() )
                      .icmpType( ipPermission.icmpType() )
                      .icmpCode( ipPermission.icmpCode() )
                      .groupId( ipPermission.groupId() )
                      .groupOwnerId( ipPermission.groupOwnerAccountNumber() )
                      .cidr( ipPermission.cidr() )
                      .o( ) ) )
              .o( ) )
          .o( ) ) );

    BNetworkInfo bni = info.o( );

    if ( MODE_CLEAN ) {
      bni = NetworkInfoBroadcaster.modeClean( vpcmido, bni );
    }

    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Constructed network information for " + Iterables.size( instances ) +
          " instance(s), " + Iterables.size( groups ) + " security group(s)" );
    }

    return bni;
  }

  public interface NetworkInfoSource {
    Iterable<NetworkView.VmInstanceNetworkView> getInstances( );
    Iterable<NetworkView.NetworkGroupNetworkView> getSecurityGroups( );
    Iterable<NetworkView.VpcNetworkView> getVpcs( );
    Iterable<NetworkView.SubnetNetworkView> getSubnets( );
    Iterable<NetworkView.DhcpOptionSetNetworkView> getDhcpOptionSets( );
    Iterable<NetworkView.NetworkAclNetworkView> getNetworkAcls( );
    Iterable<NetworkView.RouteTableNetworkView> getRouteTables( );
    Iterable<NetworkView.InternetGatewayNetworkView> getInternetGateways( );
    Iterable<NetworkView.NetworkInterfaceNetworkView> getNetworkInterfaces( );
    Iterable<NetworkView.NatGatewayNetworkView> getNatGateways( );
    Map<String,Iterable<? extends NetworkView.VersionedNetworkView>> getView( );
  }

  @TypeMapper
  public enum NetworkConfigurationToNetworkInfo implements Function<NetworkConfiguration, ImmutableBNetworkInfo.Builder> {
    INSTANCE;

    @Override
    public ImmutableBNetworkInfo.Builder apply( final NetworkConfiguration networkConfiguration ) {
      final ManagedSubnet managedSubnet = networkConfiguration.managedSubnet( ).getOrNull( );
      return networkInfo()
          .configuration( configuration( )
              .property( property("mode", networkConfiguration.mode().getOrElse( "EDGE" ) ) )
              .property( property( )
                  .name("publicIps")
                  .values( networkConfiguration.publicIps( ) ).o( ) )
              .addAllProperties( Option.of( networkConfiguration.mido( ).isDefined( ) ? midonet( )
                  .name("mido")
                  .addAllProperties( Option.of( networkConfiguration.mido( ).get( ).gatewayHost( ).isDefined( ) ? gateways( )
                      .name("gateways")
                      .gateway( gateway( )
                          .addAllProperties( Option.of( networkConfiguration.mido( ).get( ).gatewayHost( ).isDefined( ) ?
                              property( "gatewayHost", networkConfiguration.mido( ).get( ).gatewayHost( ).get(  ) ) :
                              null ) )
                          .addAllProperties( Option.of( networkConfiguration.mido( ).get( ).gatewayIP( ).isDefined() ?
                              property( "gatewayIP", networkConfiguration.mido( ).get( ).gatewayIP( ).get() ) :
                              null ) )
                          .addAllProperties( Option.of( networkConfiguration.mido( ).get( ).gatewayInterface( ).isDefined() ?
                              property( "gatewayInterface", networkConfiguration.mido( ).get( ).gatewayInterface( ).get() ) :
                              null ) )
                          .o( ) )
                      .o( ) : null ) )
                  .addAllProperties( Option.of( !networkConfiguration.mido( ).get( ).gateways( ).isEmpty( ) ? gateways( )
                      .name("gateways")
                      .addAllGateways( Array.ofAll( networkConfiguration.mido( ).get( ).gateways( ) ).map( midoGateway ->
                        gateway( )
                          .addAllProperties( midoGateway.gatewayIP().isDefined() ?
                              Array.of( // legacy format
                                property("gatewayHost", midoGateway.gatewayHost().get() ),
                                property("gatewayIP", midoGateway.gatewayIP().get() ),
                                property("gatewayInterface", midoGateway.gatewayInterface().get() )
                              ) : Array.<BNIPropertyBase>of(
                                  property( "ip", midoGateway.ip().get() ),
                                  property( "externalCidr", midoGateway.externalCidr().get() ),
                                  property( "externalDevice", midoGateway.externalDevice().get() ),
                                  property( "externalIp", midoGateway.externalIp().get() )
                              ).appendAll(
                                Array.<Option<BNIPropertyBase>>of(
                                    Option.of( midoGateway.externalRouterIp().isDefined( ) ?
                                        property("externalRouterIp", midoGateway.externalRouterIp().get() ) :
                                        null ),
                                    Option.of( midoGateway.bgpPeerIp().isDefined( ) ?
                                        property("bgpPeerIp", midoGateway.bgpPeerIp().get() ) :
                                        null ),
                                    Option.of( midoGateway.bgpPeerAsn().isDefined( ) ?
                                        property("bgpPeerAsn", midoGateway.bgpPeerAsn().get() ) :
                                        null ),
                                    Option.of( !midoGateway.bgpAdRoutes().isEmpty( ) ?
                                        property( ).name( "bgpAdRoutes" ).values( midoGateway.bgpAdRoutes() ).o( ) :
                                        null )
                                ).flatMap( identity( ) )
                            ) )
                          .o( ) ) )
                      .o( ) : null ) )
                  .addAllProperties( Array.<Option<BNIPropertyBase>>of(
                      Option.of( networkConfiguration.mido( ).get( ).eucanetdHost().isDefined() ?
                          property("eucanetdHost", networkConfiguration.mido( ).get( ).eucanetdHost( ).get() ) :
                          null ),
                      Option.of( networkConfiguration.mido( ).get().bgpAsn().isDefined( ) ?
                          property( "bgpAsn", networkConfiguration.mido( ).get( ).bgpAsn().get() ) :
                          null ),
                      Option.of( networkConfiguration.mido( ).get().publicNetworkCidr().isDefined() ?
                          property( "publicNetworkCidr", networkConfiguration.mido( ).get( ).publicNetworkCidr( ).get() ) :
                          null ),
                      Option.of( networkConfiguration.mido( ).get().publicGatewayIP().isDefined() ?
                          property( "publicGatewayIP", networkConfiguration.mido( ).get( ).publicGatewayIP( ).get() ) :
                          null )
                  ).flatMap( identity() ) )
                  .o( ) : null ) )
              .addAllProperties( Option.of(
                !networkConfiguration.subnets( ).isEmpty( ) ? subnets( )
                  .name( "subnets" )
                  .addAllSubnets( Array.ofAll( networkConfiguration.subnets( ) ).map( edgeSubnet ->
                    subnet( )
                      .name( edgeSubnet.subnet().get() )  // broadcast name is always the subnet value
                      .property( property( "subnet", edgeSubnet.subnet().get() ) )
                      .property( property( "netmask", edgeSubnet.netmask().get() ) )
                      .property( property( "gateway", edgeSubnet.gateway().get() ) )
                      .o( )
                  ) )
                  .o( ) : null ) )
              .addAllProperties( Option.of(
                managedSubnet != null ? managedSubnets( )
                  .name( "managedSubnet" )
                  .managedSubnet( BNI.managedSubnet( )
                    .name( managedSubnet.subnet().get() ) // broadcast name is always the subnet value
                    .property( property( "subnet", managedSubnet.subnet().get() ) )
                    .property( property( "netmask", managedSubnet.netmask().get() ) )
                    .property( property( "minVlan", String.valueOf( managedSubnet.minVlan().getOrElse( ManagedSubnet.MIN_VLAN ) ) ) )
                    .property( property( "maxVlan", String.valueOf( managedSubnet.maxVlan().getOrElse( ManagedSubnet.MAX_VLAN ) ) ) )
                    .property( property( "segmentSize", String.valueOf( managedSubnet.segmentSize().getOrElse( ManagedSubnet.DEF_SEGMENT_SIZE ) ) ) )
                    .o( ) )
                  .o( ) : null ) )
              .o( ) );
    }
  }

  @TypeMapper
  public enum VmInstanceToVmInstanceNetworkView implements Function<VmInstance,VmInstanceNetworkView> {
    INSTANCE;

    @Override
    public VmInstanceNetworkView apply( final VmInstance instance ) {
      return ImmutableNetworkView.VmInstanceNetworkView.builder( )
          .id( instance.getInstanceId() )
          .version( instance.getVersion() )
          .state( instance.getState() )
          .omit( MoreObjects.firstNonNull( instance.getRuntimeState().getZombie(), false ) || !validInstanceMetadata( instance ) )
          .ownerAccountNumber( instance.getOwnerAccountNumber() )
          .vpcId( Option.of( instance.getBootRecord().getVpcId() ) )
          .subnetId( Option.of( instance.getBootRecord().getSubnetId() ) )
          .macAddress( Option.of( Strings.emptyToNull( instance.getMacAddress() ) ) )
          .privateAddress( Option.of( instance.getPrivateAddress() ) )
          .publicAddress( Option.of( instance.getPublicAddress() ) )
          .partition( instance.getPartition() )
          .node( Option.of( Strings.nullToEmpty( VmInstances.toNodeHost( ).apply( instance ) ) ) )
          .securityGroupIds( Array.ofAll( instance.getNetworkGroups( ) ).map( NetworkGroup::getGroupId ) )
          .o();
    }
  }

  @TypeMapper
  public enum NetworkGroupToNetworkGroupNetworkView implements Function<NetworkGroup,NetworkGroupNetworkView> {
    INSTANCE;

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public NetworkGroupNetworkView apply( final NetworkGroup group ) {
      return ImmutableNetworkView.NetworkGroupNetworkView.builder()
          .id( group.getGroupId() )
          .version( group.getVersion() )
          .ownerAccountNumber( group.getOwnerAccountNumber() )
          .vpcId( Option.of( group.getVpcId() ) )
          .ingressPermissions( Array.ofAll( group.getIngressNetworkRules( ) ).flatMap( NetworkInfoBroadcaster::explodePermissions ) )
          .egressPermissions( Array.ofAll( group.getEgressNetworkRules( ) ).flatMap( NetworkInfoBroadcaster::explodePermissions ) )
          .o( );
    }
  }

  @TypeMapper
  public enum VpcToVpcNetworkView implements Function<Vpc,VpcNetworkView> {
    INSTANCE;

    @Override
    public VpcNetworkView apply( final Vpc vpc ) {
      return ImmutableNetworkView.VpcNetworkView.builder()
          .id( vpc.getDisplayName() )
          .version( vpc.getVersion() )
          .ownerAccountNumber( vpc.getOwnerAccountNumber() )
          .cidr( vpc.getCidr() )
          .dhcpOptionSetId( Option.of( vpc.getDhcpOptionSet( ) ).map( CloudMetadatas.toDisplayName( ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum SubnetToSubnetNetworkView implements Function<Subnet,SubnetNetworkView> {
    INSTANCE;

    @Override
    public SubnetNetworkView apply( final Subnet subnet ) {
      return ImmutableNetworkView.SubnetNetworkView.builder()
          .id( subnet.getDisplayName( ) )
          .version( subnet.getVersion() )
          .ownerAccountNumber( subnet.getOwnerAccountNumber( ) )
          .vpcId( subnet.getVpc( ).getDisplayName( ) )
          .cidr( subnet.getCidr( ) )
          .availabilityZone( subnet.getAvailabilityZone( ) )
          .networkAcl( Option.of( subnet.getNetworkAcl( ) ).map( CloudMetadatas.toDisplayName( ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum DhcpOptionSetToDhcpOptionSetNetworkView implements Function<DhcpOptionSet,DhcpOptionSetNetworkView> {
    INSTANCE;

    @Override
    public DhcpOptionSetNetworkView apply( final DhcpOptionSet dhcpOptionSet ) {
      return ImmutableNetworkView.DhcpOptionSetNetworkView.builder( )
          .id( dhcpOptionSet.getDisplayName( ) )
          .version( dhcpOptionSet.getVersion( ) )
          .ownerAccountNumber( dhcpOptionSet.getOwnerAccountNumber( ) )
          .options( Array.ofAll( dhcpOptionSet.getDhcpOptions( ) ).map( option ->
            ImmutableNetworkView.DhcpOptionNetworkView.builder( )
              .key( option.getKey( ) )
              .values( Array.ofAll( option.getValues( ) ) )
              .o( ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum NetworkAclToNetworkAclNetworkView implements Function<NetworkAcl,NetworkAclNetworkView> {
    INSTANCE;

    @Override
    public NetworkAclNetworkView apply( final NetworkAcl networkAcl ) {
      Array<NetworkAclEntry> orderedEntries = Array.ofAll( networkAcl.getEntries( ) ).sorted( NetworkAcls.ENTRY_ORDERING );
      return ImmutableNetworkView.NetworkAclNetworkView.builder( )
          .id( networkAcl.getDisplayName( ) )
          .version( networkAcl.getVersion( ) )
          .ownerAccountNumber( networkAcl.getOwnerAccountNumber( ) )
          .vpcId( networkAcl.getVpc( ).getDisplayName( ) )
          .ingressRules( orderedEntries
              .filter( FUtils.negate( NetworkAclEntry::getEgress ) )
              .map( TypeMappers.lookupF( NetworkAclEntry.class, NetworkAclEntryNetworkView.class ) ) )
          .egressRules( orderedEntries
              .filter( NetworkAclEntry::getEgress )
              .map( TypeMappers.lookupF( NetworkAclEntry.class, NetworkAclEntryNetworkView.class ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum NetworkAclEntryToNetworkAclEntryNetworkView implements Function<NetworkAclEntry,NetworkAclEntryNetworkView> {
    INSTANCE;

    @Override
    public NetworkAclEntryNetworkView apply( final NetworkAclEntry networkAclEntry ) {
      return ImmutableNetworkView.NetworkAclEntryNetworkView.builder( )
          .number( networkAclEntry.getRuleNumber( ) )
          .protocol( networkAclEntry.getProtocol( ) )
          .action( String.valueOf( networkAclEntry.getRuleAction() ) )
          .cidr( networkAclEntry.getCidr( ) )
          .icmpType( Option.of( networkAclEntry.getIcmpType() ) )
          .icmpCode( Option.of( networkAclEntry.getIcmpCode() ) )
          .portRangeFrom( Option.of( networkAclEntry.getPortRangeFrom() ) )
          .portRangeTo( Option.of( networkAclEntry.getPortRangeTo() ) )
          .o();
    }
  }

  @TypeMapper
  public enum NetworkAclEntryNetworkViewToNINetworkAclRule implements Function<NetworkAclEntryNetworkView,BNINetworkAclEntry> {
    INSTANCE;

    @Override
    public BNINetworkAclEntry apply( final NetworkAclEntryNetworkView networkAclEntry ) {
      return BNI.networkAclEntry( )
          .number( networkAclEntry.number( ) )
          .protocol( networkAclEntry.protocol( ) )
          .action( networkAclEntry.action( ) )
          .cidr( networkAclEntry.cidr( ) )
          .icmpCode( networkAclEntry.icmpCode( ) )
          .icmpType( networkAclEntry.icmpType( ) )
          .portRangeFrom( networkAclEntry.portRangeFrom( ) )
          .portRangeTo( networkAclEntry.portRangeTo( ) )
          .o( );
    }
  }

  @TypeMapper
  public enum RouteTableToRouteTableNetworkView implements Function<RouteTable,RouteTableNetworkView> {
    INSTANCE;

    @Override
    public RouteTableNetworkView apply( final RouteTable routeTable ) {
      return ImmutableNetworkView.RouteTableNetworkView.builder( )
          .id( routeTable.getDisplayName() )
          .version( routeTable.getVersion() )
          .ownerAccountNumber( routeTable.getOwnerAccountNumber() )
          .vpcId( routeTable.getVpc().getDisplayName() )
          .main( routeTable.getMain() )
          .subnetIds( Array.ofAll( routeTable.getRouteTableAssociations( ) )
              .flatMap( FUtils.chain( RouteTableAssociation::getSubnetId, Option::of ) ) )
          .routes( Array.ofAll( routeTable.getRoutes() )
              .map( TypeMappers.lookupF( Route.class, RouteNetworkView.class ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum RouteToRouteNetworkView implements Function<Route,RouteNetworkView> {
    INSTANCE;

    @Override
    public RouteNetworkView apply(final Route route) {
      return ImmutableNetworkView.RouteNetworkView.builder( )
          .active( route.getState() == Route.State.active )
          .routeTableId( route.getRouteTable().getDisplayName() )
          .destinationCidr( route.getDestinationCidr() )
          .gatewayId( Option.of( route.getInternetGatewayId() ) )
          .natGatewayId( Option.of( route.getNatGatewayId() ) )
          .networkInterfaceId( Option.of( route.getNetworkInterfaceId() ) )
          .instanceId( Option.of( route.getInstanceId() ) )
          .o( );
    }
  }

  @TypeMapper
  public enum RouteNetworkViewToNIRoute implements Function<RouteNetworkView,BNIRoute> {
    INSTANCE;

    @Override
    public BNIRoute apply(final RouteNetworkView routeNetworkView) {
      return route( )
          .destinationCidr( routeNetworkView.destinationCidr( ) )
          .gatewayId( routeNetworkView.gatewayId( )
              .orElse( routeNetworkView.networkInterfaceId( ).orElse( routeNetworkView.natGatewayId( ) ).isDefined( ) ?
                  Option.none( ) :
                  Option.of( "local" ) ) )
          .networkInterfaceId( routeNetworkView.networkInterfaceId( ) )
          .natGatewayId( routeNetworkView.natGatewayId( ) )
          .o( );
    }
  }

  @TypeMapper
  public enum InternetGatewayToInternetGatewayNetworkView implements Function<InternetGateway,InternetGatewayNetworkView> {
    INSTANCE;

    @Override
    public InternetGatewayNetworkView apply( final InternetGateway internetGateway ) {
      return ImmutableNetworkView.InternetGatewayNetworkView.builder( )
          .id( internetGateway.getDisplayName() )
          .version( internetGateway.getVersion() )
          .ownerAccountNumber( internetGateway.getOwnerAccountNumber() )
          .vpcId( Option.of( internetGateway.getVpc( ) ).map( CloudMetadatas.toDisplayName( ) ) )
          .o( );
    }
  }

  @TypeMapper
  public enum VpcNetworkInterfaceToNetworkInterfaceNetworkView implements Function<NetworkInterface,NetworkInterfaceNetworkView> {
    INSTANCE;

    @Override
    public NetworkInterfaceNetworkView apply( final NetworkInterface networkInterface ) {
      final Option<NetworkInterfaceAttachment> attachment = Option.of( networkInterface.getAttachment( ) );
      return ImmutableNetworkView.NetworkInterfaceNetworkView.builder( )
          .id( networkInterface.getDisplayName() )
          .version( networkInterface.getVersion() )
          .state( networkInterface.getState() )
          .attachmentStatus( attachment
              .map( NetworkInterfaceAttachment::getStatus )
              .getOrElse( NetworkInterfaceAttachment.Status.detached ) )
          .ownerAccountNumber( networkInterface.getOwnerAccountNumber() )
          .instanceId( attachment.map( NetworkInterfaceAttachment::getInstanceId ) )
          .attachmentId( attachment.map( NetworkInterfaceAttachment::getAttachmentId ) )
          .deviceIndex( attachment.map( NetworkInterfaceAttachment::getDeviceIndex ) )
          .macAddress( Option.of( Strings.emptyToNull( networkInterface.getMacAddress() ) ) )
          .privateIp( Option.of( networkInterface.getPrivateIpAddress( ) ) )
          .publicIp( Option.of( networkInterface.getAssociation( ) ).map( NetworkInterfaceAssociation::getPublicIp ) )
          .sourceDestCheck( networkInterface.getSourceDestCheck( ) )
          .vpcId( networkInterface.getVpc().getDisplayName() )
          .subnetId( networkInterface.getSubnet().getDisplayName() )
          .securityGroupIds( Array.ofAll( networkInterface.getNetworkGroups( ) ).map( NetworkGroup::getGroupId ) )
          .o( );
    }
  }

  @TypeMapper
  public enum NatGatewayToNatGatewayNetworkView implements Function<NatGateway,NatGatewayNetworkView> {
    INSTANCE;

    @Override
    public NatGatewayNetworkView apply( final NatGateway natGateway ) {
      return ImmutableNetworkView.NatGatewayNetworkView.builder( )
          .id( natGateway.getDisplayName() )
          .version( natGateway.getVersion() )
          .state( natGateway.getState() )
          .ownerAccountNumber( natGateway.getOwnerAccountNumber() )
          .macAddress( Option.of( Strings.emptyToNull( natGateway.getMacAddress() ) ) )
          .privateIp( Option.of( natGateway.getPrivateIpAddress() ) )
          .publicIp( Option.of( natGateway.getPublicIpAddress() ) )
          .vpcId( natGateway.getVpcId() )
          .subnetId( natGateway.getSubnetId() )
          .o( );
    }
  }


  @SuppressWarnings( { "WeakerAccess", "unused" } )
  public static class NetworkInfoBroadcasterEventListener implements EventListener<ClockTick> {
    private final int intervalTicks = 3;
    private volatile int counter = 0;

    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkInfoBroadcasterEventListener( ) );
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      if ( counter++%intervalTicks == 0 &&
          Topology.isEnabledLocally( Eucalyptus.class ) &&
          Hosts.isCoordinator( ) &&
          Bootstrap.isOperational( ) &&
          !Databases.isVolatile( ) ) {
        requestNetworkInfoBroadcast( );
      }
    }
  }
}
