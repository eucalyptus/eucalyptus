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

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet;
import com.eucalyptus.compute.common.internal.vpc.InternetGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.vpc.EventFiringVpcRouteStateInvalidator;
import com.eucalyptus.compute.vpc.RouteKey;
import com.eucalyptus.compute.vpc.VpcRouteStateInvalidator;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.network.NetworkInfoBroadcasts.NatGatewayNetworkView;
import com.eucalyptus.network.applicator.ApplicatorException;
import com.eucalyptus.network.applicator.Applicators;
import com.eucalyptus.network.config.NetworkConfiguration;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.network.NetworkInfoBroadcasts.VmInstanceNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.NetworkGroupNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.VpcNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.SubnetNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.DhcpOptionSetNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.NetworkAclNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.RouteTableNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.InternetGatewayNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.NetworkInterfaceNetworkView;
import com.eucalyptus.network.NetworkInfoBroadcasts.NetworkInfoSource;
import com.eucalyptus.network.NetworkInfoBroadcasts.VersionedNetworkView;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.SemaphoreResource;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.PrimitiveSink;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN;
import static com.google.common.hash.Hashing.goodFastHash;

/**
 *
 */
@SuppressWarnings( { "Guava", "Convert2Lambda", "StaticPseudoFunctionalStyleMethod" } )
public class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster.class );

  private static final AtomicLong lastBroadcastTime = new AtomicLong( 0L );
  private static final Lock lastBroadcastTimeLock = new ReentrantLock( );
  private static final Semaphore activeBroadcastSemaphore = new Semaphore( 1 );
  private static final EntityCache<VmInstance,NetworkInfoBroadcasts.VmInstanceNetworkView> instanceCache = new EntityCache<>(
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
      @Override public Map<String, Iterable<? extends VersionedNetworkView>> getView() {
        return ImmutableMap.<String, Iterable<? extends VersionedNetworkView>>builder( )
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
      final Optional<NetworkConfiguration> networkConfiguration = NetworkConfigurations.getNetworkConfiguration( );
      final List<Cluster> clusters = Clusters.list( );
      final List<Cluster> otherClusters = Clusters.listDisabled( );

      final NetworkInfoSource source = cacheSource( );
      final Set<String> dirtyPublicAddresses = PublicAddresses.dirtySnapshot( );
      final Set<RouteKey> invalidStateRoutes = Sets.newHashSetWithExpectedSize( 50 );
      final int sourceFingerprint = fingerprint( source, clusters, dirtyPublicAddresses, NetworkGroups.NETWORK_CONFIGURATION );
      final BNetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
              networkConfiguration,
              source,
              BaseEncoding.base16( ).lowerCase( ).encode( Ints.toByteArray( sourceFingerprint ) ),
              Suppliers.ofInstance( clusters ),
              Suppliers.ofInstance( otherClusters ),
              new Supplier<String>( ) {
                @Override
                public String get() {
                  return Topology.lookup( Eucalyptus.class ).getInetAddress( ).getHostAddress( );
                }
              },
              new Function<List<String>, List<String>>( ) {
                @Nullable
                @Override
                public List<String> apply( final List<String> defaultServers ) {
                  return NetworkConfigurations.loadSystemNameservers( defaultServers );
                }
              },
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
