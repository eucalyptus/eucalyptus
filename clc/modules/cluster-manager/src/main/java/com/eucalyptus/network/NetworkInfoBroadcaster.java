/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.NIInstance;
import com.eucalyptus.cluster.NINetworkInterface;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.cluster.callback.BroadcastNetworkInfoCallback;
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
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.network.NetworkInfoBroadcasts.NatGatewayNetworkView;
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
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.PrimitiveSink;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN;
import static com.google.common.hash.Hashing.goodFastHash;

/**
 *
 */
public class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster.class );

  private static final AtomicLong lastBroadcastTime = new AtomicLong( 0L );
  private static final Lock lastBroadcastTimeLock = new ReentrantLock( );
  private static final AtomicReference<LastBroadcastInfo> lastBroadcastInformation = new AtomicReference<>( );
  private static final AtomicReference<Pair<Long,String>> lastAppliedNetworkInformation = new AtomicReference<>( );
  private static final ConcurrentMap<String,Long> activeBroadcastMap = Maps.newConcurrentMap( );
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

  private static final class LastBroadcastInfo {
    private final int version;
    private final String appliedVersion;
    private final NetworkInfo networkInfo;
    private final String encodedNetworkInfo;
    private final long lastConvergedTimestamp;

    public LastBroadcastInfo(
        final int version,
        final String appliedVersion,
        final NetworkInfo networkInfo,
        final String encodedNetworkInfo,
        final long lastConvergedTimestamp
    ) {
      this.version = version;
      this.appliedVersion = appliedVersion;
      this.networkInfo = networkInfo;
      this.encodedNetworkInfo = encodedNetworkInfo;
      this.lastConvergedTimestamp = lastConvergedTimestamp;
    }
  }

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
      @Override
      public Void call( ) throws Exception {
        boolean shouldBroadcast = false;
        boolean shouldRetryWithDelay = false;
        try ( final LockResource lock = LockResource.lock( lastBroadcastTimeLock ) ) {
          final long currentTime = System.currentTimeMillis( );
          final long lastBroadcast = lastBroadcastTime.get( );
          if ( requestedTime >= lastBroadcast &&
              lastBroadcast + TimeUnit.SECONDS.toMillis( NetworkGroups.MIN_BROADCAST_INTERVAL ) < currentTime  ) {
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
        } else if ( shouldRetryWithDelay ) {
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

  @SuppressWarnings("UnnecessaryQualifiedReference")
  static void broadcastNetworkInfo( ){
    try {
      // populate with info directly from configuration
      final Optional<NetworkConfiguration> networkConfiguration = NetworkConfigurations.getNetworkConfiguration( );
      final List<com.eucalyptus.cluster.Cluster> clusters = Clusters.getInstance( ).listValues( );

      final NetworkInfoSource source = cacheSource( );
      final Set<String> dirtyPublicAddresses = PublicAddresses.dirtySnapshot( );
      final int sourceFingerprint = fingerprint( source, clusters, dirtyPublicAddresses, NetworkGroups.NETWORK_CONFIGURATION );
      final LastBroadcastInfo lastBroadcast = lastBroadcastInformation.get( );
      final Pair<Long,String> appliedVersion = lastAppliedNetworkInformation.get( );
      final String encodedNetworkInfo;
      if ( lastBroadcast != null && lastBroadcast.version == sourceFingerprint &&
          ( appliedVersion == null || appliedVersion.getRight( ).equals( lastBroadcast.appliedVersion ) ) ) {
        encodedNetworkInfo = lastBroadcast.encodedNetworkInfo;
        clearDirtyPublicAddresses( networkConfiguration, lastBroadcast.networkInfo );
      } else {
        final int networkInfoFingerprint;
        final NetworkInfo info;
        final boolean converged = lastBroadcast != null &&
            ( appliedVersion != null && !appliedVersion.getRight( ).equals( lastBroadcast.appliedVersion ) ) &&
            ( System.currentTimeMillis() - lastBroadcast.lastConvergedTimestamp > TimeUnit.SECONDS.toMillis( 150 ) );
        if ( converged ) {
          info = lastBroadcast.networkInfo;
          networkInfoFingerprint = lastBroadcast.version;
        } else {
          info = NetworkInfoBroadcasts.buildNetworkConfiguration(
              networkConfiguration,
              source,
              Suppliers.ofInstance( clusters ),
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
              dirtyPublicAddresses
          );
          info.setVersion( BaseEncoding.base16( ).lowerCase( ).encode( Ints.toByteArray( sourceFingerprint ) ) );
          networkInfoFingerprint = sourceFingerprint;
        }
        if ( appliedVersion != null ) {
          info.setAppliedTime( Timestamps.formatIso8601Timestamp( new Date( appliedVersion.getLeft( ) ) ) );
          info.setAppliedVersion( appliedVersion.getRight( ) );
        }

        final JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" );
        final StringWriter writer = new StringWriter( 8192 );
        jc.createMarshaller().marshal( info, writer );

        final String networkInfo = writer.toString( );
        if ( logger.isTraceEnabled( ) ) {
          logger.trace( "Broadcasting network information:\n${networkInfo}" );
        }

        final File newView = BaseDirectory.RUN.getChildFile( "global_network_info.xml.temp" );
        if ( newView.exists( ) && !newView.delete( ) ) {
          logger.warn( "Error deleting stale network view " + newView.getAbsolutePath( ) );
        }
        com.google.common.io.Files.write( networkInfo, newView, Charsets.UTF_8 );
        Files.move( newView.toPath( ), BaseDirectory.RUN.getChildFile( "global_network_info.xml" ).toPath( ), StandardCopyOption.REPLACE_EXISTING );

        encodedNetworkInfo = new String( B64.standard.enc( networkInfo.getBytes( Charsets.UTF_8 ) ), Charsets.UTF_8 );
        lastBroadcastInformation.set( new LastBroadcastInfo(
            networkInfoFingerprint,
            appliedVersion == null ? null : appliedVersion.getRight( ),
            info,
            encodedNetworkInfo,
            info.getAppliedVersion( ) == null || info.getAppliedVersion( ).equals( info.getVersion( ) ) ? System.currentTimeMillis( ) : lastBroadcast == null ? 0 : lastBroadcast.lastConvergedTimestamp
        ) );
      }

      final BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback( encodedNetworkInfo );
      for ( final com.eucalyptus.cluster.Cluster cluster : clusters ) {
        final Long broadcastTime = System.currentTimeMillis( );
        if ( null == activeBroadcastMap.putIfAbsent( cluster.getPartition( ), broadcastTime ) ) {
          try {
            AsyncRequests.newRequest( callback.newInstance( ) ).then( new UnconditionalCallback<BroadcastNetworkInfoResponseType>() {
              @Override
              public void fire( ) {
                activeBroadcastMap.remove( cluster.getPartition( ), broadcastTime );
              }
            } ).dispatch( cluster.getConfiguration( ) );
          } catch ( Exception e ) {
            activeBroadcastMap.remove( cluster.getPartition( ), broadcastTime );
            logger.error( "Error broadcasting network information to cluster (" + cluster.getPartition() + ") ("+cluster.getName()+")", e );
          }
        } else {
          logger.warn( "Skipping network information broadcast for active partition " + cluster.getPartition( ) );
        }
      }
    } catch ( IOException | JAXBException e ) {
      logger.error( "Error during network broadcast", e );
    }
  }

  private static int fingerprint(
      final NetworkInfoSource source,
      final List<com.eucalyptus.cluster.Cluster> clusters,
      final Set<String> dirtyPublicAddresses,
      final String networkConfiguration
  ) {
    final HashFunction hashFunction = goodFastHash( 32 );
    final Hasher hasher = hashFunction.newHasher( );
    final Funnel<VersionedNetworkView> versionedItemFunnel = new Funnel<VersionedNetworkView>() {
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

  private static void clearDirtyPublicAddresses(
      final Optional<NetworkConfiguration> networkConfiguration,
      final NetworkInfo networkInfo
  ) {
    final boolean vpcmido =
        networkConfiguration.transform( config -> "VPCMIDO".equals( config.getMode( ) ) ).or( false );
    if ( !vpcmido ) return;

    final Set<String> broadcastPublicIps = networkInfo.getInstances( ).stream( )
        .flatMap( FUtils.chain( NIInstance::getNetworkInterfaces, Collection::stream ) )
        .map( NINetworkInterface::getPublicIp )
        .filter( Objects::nonNull )
        .collect( Collectors.toSet( ) );

    PublicAddresses.dirtySnapshot( ).stream( )
        .filter( publicIp -> !broadcastPublicIps.contains( publicIp ) )
        .forEach( PublicAddresses::clearDirty );
  }

  public static class AppliedNetworkInfoEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register( ClockTick.class, new AppliedNetworkInfoEventListener( ) );
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      final Pair<Long,String> appliedVersion = lastAppliedNetworkInformation.get( );
      final File newView = BaseDirectory.RUN.getChildFile( "global_network_info.version" );
      if ( newView.isFile( ) && ( appliedVersion == null || appliedVersion.getLeft( ) != newView.lastModified( ) ) ) {
        try {
          final String version = com.google.common.io.Files.toString( newView, StandardCharsets.UTF_8 ).trim( );
          lastAppliedNetworkInformation.set( Pair.pair( newView.lastModified( ), version ) );
          requestNetworkInfoBroadcast( );
        } catch ( IOException e ) {
          logger.error( "Error reading last applied network broadcast version" );
        }
      }
    }
  }

  public static class NetworkInfoBroadcasterEventListener implements EventListener<ClockTick> {
    private final int intervalTicks = 3;
    private final int activeBroadcastTimeoutMins = 3;
    private volatile int counter = 0;

    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkInfoBroadcasterEventListener( ) );
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      for ( final Map.Entry<String,Long> entry : NetworkInfoBroadcaster.activeBroadcastMap.entrySet( ) ) {
        if ( entry.getValue() + TimeUnit.MINUTES.toMillis( activeBroadcastTimeoutMins ) < System.currentTimeMillis( ) &&
            NetworkInfoBroadcaster.activeBroadcastMap.remove( entry.getKey( ), entry.getValue( ) ) ) {
          logger.warn( "Timed out active network information broadcast for partition" + entry.getKey( ) );
        }
      }

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
