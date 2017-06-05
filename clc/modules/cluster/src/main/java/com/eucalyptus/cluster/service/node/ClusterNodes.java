/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.service.node;

import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.common.msgs.VolumeType;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.cluster.service.conf.ClusterEucaConf;
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader;
import com.eucalyptus.cluster.service.scheduler.Scheduler;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.eucalyptus.cluster.service.vm.ClusterVmBootDevice;
import com.eucalyptus.cluster.service.vm.ClusterVmBootRecord;
import com.eucalyptus.cluster.service.vm.ClusterVmInterface;
import com.eucalyptus.cluster.service.vm.ClusterVmMigrationState;
import com.eucalyptus.cluster.service.vm.ClusterVmVolume;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple3;
import javaslang.collection.Stream;
import javaslang.control.Either;
import javaslang.control.Option;

/**
 * Tracks resources, service state and metrics for a clusters nodes.
 */
@ComponentNamed
public class ClusterNodes {

  private static final Logger logger = Logger.getLogger( ClusterNodes.class );

  private static final int DEFAULT_PORT = 8775;

  private final ConcurrentMap<String,ClusterNode> nodesByIp = Maps.newConcurrentMap( );
  private final AtomicReference<Integer> nodePort = new AtomicReference<>( DEFAULT_PORT );
  private final AtomicLong lastSensorRefresh = new AtomicLong( );
  private final AtomicReference<ClusterNodesStatus> statusRef = new AtomicReference<>( ClusterNodesStatus.none( ) );
  private final ClusterEucaConfLoader clusterEucaConfLoader;
  private final ClusterNodeServiceFactory nodeServiceFactory;
  private final Clock clock;

  @SuppressWarnings( "WeakerAccess" )
  public ClusterNodes(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodeServiceFactory nodeServiceFactory,
      final Clock clock
  ) {
    this.clusterEucaConfLoader = Assert.notNull( clusterEucaConfLoader, "clusterEucaConfLoader" );
    this.nodeServiceFactory = Assert.notNull( nodeServiceFactory, "nodeServiceFactory" );
    this.clock = Assert.notNull( clock, "clock" );
  }

  @Inject
  public ClusterNodes(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodeServiceFactory nodeServiceFactory
  ) {
    this( clusterEucaConfLoader, nodeServiceFactory, Clock.systemDefaultZone( ) );
  }

  @SuppressWarnings( "WeakerAccess" )
  public ClusterNode getClusterNode( final String node ) {
    return nodesByIp.computeIfAbsent( node, ClusterNode::new );
  }

  @SuppressWarnings( "WeakerAccess" )
  public int getNodePort( ) {
    return MoreObjects.firstNonNull( nodePort.get( ), DEFAULT_PORT );
  }

  public NodeService nodeService( final ClusterNode node ) {
    final int port = getNodePort( );
    return nodeServiceFactory.nodeService( node, port );
  }

  public NodeService nodeServiceWithAsyncErrorHandling( final ClusterNode node ) {
    final NodeService service = nodeService( node );
    return (NodeService) Proxy.newProxyInstance(
        NodeService.class.getClassLoader( ),
        new Class[]{ NodeService.class },
        ( proxy, method, args ) -> {
      final Object result = method.invoke( service, args );
      if ( result instanceof CheckedListenableFuture ) {
        final CheckedListenableFuture<?> resultFuture = (CheckedListenableFuture<?>) result;
        resultFuture.addListener( ( ) -> {
          try {
            resultFuture.get( );
          } catch ( final Exception ex ) {
            handleNodeException( Option.of( node ), method.getName( ).toLowerCase( ), ex );
          }
        } );
      }
      return result;
    } );
  }

  public Option<ClusterVm> vm( final String vmId ) {
    return nodeWithVm( vmId ).map( Tuple2::_2 );
  }

  public Option<ClusterNode> node( final String node ) {
    return nodes( ).find( n -> node.equals( n.getNode( ) ) );
  }

  public Option<ClusterNode> nodeForVm( final String vmId ) {
    return nodeWithVm( vmId ).map( Tuple2::_1 );
  }

  @SuppressWarnings( "WeakerAccess" )
  public Option<Tuple2<ClusterNode,ClusterVm>> nodeWithVm( final String vmId ) {
    for ( final ClusterNode clusterNode : nodes( ) ) {
      for ( final ClusterVm vmInfo : clusterNode.getVms( ) ) {
        if ( vmInfo.getId( ).equals( vmId ) ) {
          return Option.some( Tuple.of( clusterNode, vmInfo ) );
        }
      }
    }
    return Option.none( );
  }

  public Stream<ClusterNode> nodes( ) {
    final ClusterEucaConf conf = clusterEucaConfLoader.load( clock.millis( ) );
    return Stream.ofAll( conf.getNodes( ) ).sorted( ).map( this::getClusterNode );
  }

  public void status( final String status, final Stream<String> nodes ) {
    nodes.forEach( node -> nodes( )
        .filter( clusterNode -> clusterNode.getNode( ).equals( node ) )
        .forEach( clusterNode -> {
          if ( clusterNode.setStatus(
              status,
              ClusterNode.detailForStatus( status, "empyrean status" ) ) ) {
            String nodeStateToModify = null;
            switch ( status ) {
              case "ENABLED":
                nodeStateToModify = "enabled";
                break;
              case "STOPPED":
                nodeStateToModify = "disabled";
                break;
            }
            if ( nodeStateToModify != null ) {
              final NcModifyNodeType modifyNode = new NcModifyNodeType( );
              modifyNode.setStateName( nodeStateToModify );
              nodeServiceWithAsyncErrorHandling( clusterNode ).modifyNode( modifyNode );
            }
            logger.info( "Node status updated " + clusterNode );
          }
        } ) );
  }

  public void refreshResources( ) {
    final Stream<ClusterNode> nodes = nodes( );
    final List<CheckedListenableFuture<NcDescribeResourceResponseType>> replyFutures = Lists.newArrayList( );
    for ( final ClusterNode node : nodes ) {
      final NodeService nodeService = nodeService( node );
      final NcDescribeResourceType ncDescribeResource = new NcDescribeResourceType( );
      replyFutures.add( nodeService.describeResourceAsync( ncDescribeResource ) );
    }

    try {
      final Iterator<Either<Exception, NcDescribeResourceResponseType>> replies =
          Futures.allAsEitherList( replyFutures ).get( ).iterator( );
      for ( final ClusterNode node : nodes ) {
        final Either<Exception, NcDescribeResourceResponseType> replyEither = replies.next( );
        replyEither.swap( ).forEach( e -> handleNodeException( Option.of( node ), "refresh resources", e ) );
        replyEither.forEach( reply -> {
          if ( node.setDetails(
              reply.getHypervisor( ),
              reply.getIqn( ),
              reply.getMigrationCapable( ),
              reply.getPublicSubnets( )
          ) ) {
            logger.info( "Node " + node.getNode( ) + " details updated " + node );
          }
          if ( node.setStatusFromNode( reply.getNodeStatus( ) ) ) {
            logger.info( "Node " + node.getNode( ) + " status updated " + node );
          }

          Scheduler.withLock( () -> { //TODO:STEVE: accounting not correct here for reserved/pending resources
            if ( Component.State.STOPPED.name( ).equals( node.getNodeStatus( ) ) ) {
              node.clearCapacity( );
            } else if ( node.setAvailability(
                MoreObjects.firstNonNull( reply.getNumberOfCoresAvailable( ), 0 ),
                MoreObjects.firstNonNull( reply.getNumberOfCoresMax( ), 0 ),
                MoreObjects.firstNonNull( reply.getDiskSizeAvailable( ), 0 ),
                MoreObjects.firstNonNull( reply.getDiskSizeMax( ), 0 ),
                MoreObjects.firstNonNull( reply.getMemorySizeAvailable( ), 0 ),
                MoreObjects.firstNonNull( reply.getMemorySizeMax( ), 0 )
            ) ) {
              logger.info( "Node " + node.getNode( ) + " availability updated " + node );
            }
            Scheduler.adjust( node );
            return true;
          } );
        } );
      }
    } catch ( final Exception ex ) {
      handleRefreshException( "resources", ex );
    }
  }

  /**
   * Refresh sensors if necessary based on interval
   */
  @SuppressWarnings( "WeakerAccess" )
  public void refreshSensors(
      final long now,
      final int historySize,
      final int collectionIntervalTimeMs
  ) {
    final long lastRefresh = lastSensorRefresh.get( );
    if ( ( (now - lastRefresh) >= collectionIntervalTimeMs ) && lastSensorRefresh.compareAndSet( lastRefresh, now ) ) {
      final Stream<ClusterNode> nodes = nodes( );
      final List<Tuple2<ClusterNode,CheckedListenableFuture<NcDescribeSensorsResponseType>>> replyFutures = Lists.newArrayList( );
      for ( final ClusterNode node : nodes ) {
        final NodeService nodeService = nodeService( node );
        for ( final List<String> instanceBatch :
            Iterables.partition( node.getVms( ).map( ClusterVm::getId ).toJavaList( ArrayList::new ), 10 ) ) {
          final NcDescribeSensorsType describeSensors = new NcDescribeSensorsType( );
          describeSensors.setInstanceIds( Lists.newArrayList( instanceBatch ) );
          describeSensors.setHistorySize( historySize );
          describeSensors.setCollectionIntervalTimeMs( collectionIntervalTimeMs );
          replyFutures.add( Tuple.of( node, nodeService.describeSensorsAsync( describeSensors ) ) );
        }
      }

      for ( final Tuple2<ClusterNode,CheckedListenableFuture<NcDescribeSensorsResponseType>> nodeAndFuture : replyFutures ) {
        final ClusterNode node = nodeAndFuture._1;
        final Either<Exception,NcDescribeSensorsResponseType> replyEither = Futures.asEither( nodeAndFuture._2 );
        replyEither.swap( ).forEach( e -> handleNodeException( Option.of( node ), "refresh sensors", e ) );
        replyEither.forEach( reply -> {
          for ( final SensorsResourceType sensorsResource : reply.getSensorsResources() ) {
            if ( "instance".equals( sensorsResource.getResourceType( ) ) ) {
              vm( sensorsResource.getResourceName( ) ).forEach( vm ->
                  vm.setMetrics( sensorsResource.getMetrics( ) )
              );
            }
          }
        } );
      }
    }
  }

  /**
   * Refresh vm info from each node on request
   */
  @SuppressWarnings( "WeakerAccess" )
  public void refreshVms(
      final long now
  ) {
    final Stream<ClusterNode> nodes = nodes( );
    final List<CheckedListenableFuture<NcDescribeInstancesResponseType>> replyFutures = Lists.newArrayList( );
    for ( final ClusterNode node : nodes ) {
      final NodeService nodeService = nodeService( node );
      replyFutures.add( nodeService.describeInstancesAsync( new NcDescribeInstancesType( ) ) );
    }

    final Set<Tuple3<String, String, String>> migrationActions = Sets.newLinkedHashSet( );
    try {
      final Iterator<Either<Exception, NcDescribeInstancesResponseType>> replies =
          Futures.allAsEitherList( replyFutures ).get( ).iterator( );
      for ( final ClusterNode node : nodes ) {
        final Either<Exception, NcDescribeInstancesResponseType> replyEither = replies.next( );
        replyEither.swap( ).forEach( e -> handleNodeException( Option.of( node ), "refresh vms", e ) );
        replyEither.forEach( reply -> reply.getInstances( ).forEach( nodeVm -> nodeWithVm( nodeVm.getInstanceId( ) ).map( nodeVmTuple -> {
          final ClusterVm vm = nodeVmTuple._2;
          final boolean nodeIsVmOwner = node.equals( nodeVmTuple._1 );
          if ( node.getNode( ).equals( nodeVm.getMigrationSource( ) ) &&
              "cleaning".equals( nodeVm.getMigrationStateName( ) ) ) {
            if ( nodeIsVmOwner && "Teardown".equals( nodeVm.getStateName( ) ) ) {
              vm.migrateState( now, nodeVm.getMigrationStateName( ), nodeVm.getMigrationSource( ), nodeVm.getMigrationDestination( ) )
                  .forEach( update -> logger.info( "Migration state for " + vm.getId( ) + " updated (source teardown) from/to " + update ) );
            }
            return vm; // ignore updates from source once cleaning
          } else if ( !nodeIsVmOwner &&
              !"cleaning".equals( vm.getMigrationState( ).getState( ) ) &&
              node.getNode( ).equals( vm.getMigrationState( ).getDestinationHost( ) ) ) {
            vm.destinationMigrateState( now, nodeVm.getMigrationStateName( ), nodeVm.getMigrationSource( ), nodeVm.getMigrationDestination( ) )
                .forEach( update -> logger.info( "Destination migration state for " + vm.getId( ) + " updated from/to " + update ) );
            if ( "ready".equals( nodeVm.getMigrationStateName( ) ) && "Extant".equals( nodeVm.getStateName( ) ) ) {
              if ( "ready".equals( vm.getMigrationState( ).getState( ) ) ) {
                migrationActions.add( Tuple.of( vm.getMigrationState( ).getSourceHost( ), vm.getId( ), "commit" ) );
              } else if ( ClusterVmMigrationState.none( ).equals( vm.getMigrationState( ) ) ) {
                migrationActions.add( Tuple.of( vm.getMigrationState( ).getDestinationHost( ), vm.getId( ), "rollback" ) );
              }
            } else if ( ( ImmutableSet.of( "cleaning", "ready" ).contains( nodeVm.getMigrationStateName( ) ) && "Teardown".equals( nodeVm.getStateName( ) ) ) ) {
              migrationActions.add( Tuple.of( vm.getMigrationState( ).getSourceHost( ), vm.getId( ), "rollback" ) );
            }
            return vm; // ignore updates from destination while migrating
          }
          if ( !nodeIsVmOwner ) {
            if ( node.getNode( ).equals( vm.getMigrationState( ).getDestinationHost( ) ) ) {
              logger.info( "Migrating instance " + vm.getId( ) + " metadata moved to node " + node.getNode( ) );
              node.vm( nodeVmTuple._1.rvm( vm ) );
              vm.destinationMigrateState( now, ClusterVmMigrationState.none().getState( ), null, null )
                  .forEach( update -> logger.info( "Destination migration state for " + vm.getId( ) + " updated (migrated) from/to " + update ) );
            } else {
              logger.info( "Ignoring instance " + vm.getId( ) + " report from unexpected node " + node.getNode( ) );
              return vm;
            }
          }
          vm.setReportedTimestamp( now );
          vm.state( nodeVm.getStateName( ), nodeVm.getGuestStateName( ), now )
              .forEach( update -> logger.info( "Instance state for " + vm.getId( ) + " via " + node.getNode( ) + " updated from/to " + update ) );
          vm.bundleState( nodeVm.getBundleTaskStateName( ), nodeVm.getBundleTaskProgress( ) )
              .forEach( update -> logger.info( "Bundle state for " + vm.getId( ) + " via " + node.getNode( ) + " updated from/to " + update ) );
          vm.migrateState( now, nodeVm.getMigrationStateName( ), nodeVm.getMigrationSource( ), nodeVm.getMigrationDestination( ) )
              .forEach( update -> logger.info( "Migration state for " + vm.getId( ) + " via " + node.getNode( ) + " updated from/to " + update ) );
          vm.primaryPublicAddress( nodeVm.getNetParams( ).getPublicIp( ) )
              .forEach( update -> logger.info( "Public address for " + vm.getId( ) + " via " + node.getNode( ) + " updated from/to " + update  ) );
          final Set<Integer> secondaryEniDevices = Sets.newHashSet( );
          nodeVm.getSecondaryNetConfig( ).forEach( netConfig -> {
            secondaryEniDevices.add( netConfig.getDevice( ) );
            final ClusterVmInterface attachment = vm.getSecondaryInterfaceAttachments( ).get( netConfig.getDevice( ) );
            if ( attachment == null || !attachment.getAttachmentId( ).equals( netConfig.getAttachmentId( ) ) ||
                !attachment.getInterfaceId( ).equals( netConfig.getInterfaceId( ) ) ||
                !attachment.getPublicAddress( ).equals( netConfig.getPublicIp( ) ) ) {
              final ClusterVmInterface vmInterface = ClusterVmInterface.fromNodeInterface( netConfig );
              vm.getSecondaryInterfaceAttachments( ).put( netConfig.getDevice( ), vmInterface );
              logger.info( "Updated secondary network interface for " + vm.getId( ) + " via " + node.getNode( ) + " as " + vmInterface );
            }
          } );
          vm.getSecondaryInterfaceAttachments( ).keySet( ).retainAll( secondaryEniDevices );
          vm.updateBootRecord( ClusterVmBootRecord.fromNodeRecord( nodeVm.getInstanceType( ).getVirtualBootRecord( ) ) );
          final Set<String> volumeIds = Sets.newHashSet( );
          nodeVm.getVolumes( ).forEach( volume -> {
            volumeIds.add( volume.getVolumeId( ) );
            final ClusterVmVolume attachment = vm.getVolumeAttachments( ).get( volume.getVolumeId( ) );
            if ( attachment == null || !attachment.getDevice( ).equals( volume.getLocalDev( ) ) ||
                !attachment.getState( ).equals( volume.getState( ) ) ) {
              final ClusterVmVolume vmVolume = ClusterVmVolume.fromNodeVolume(
                  attachment != null ? attachment.getAttachmentTimestamp( ) : now, volume );
              vm.getVolumeAttachments( ).put( volume.getVolumeId( ), vmVolume );
              logger.info( "Updated volume for " + vm.getId( ) + " via " + node.getNode( ) + " as " + vmVolume );
            }
          } );
          vm.getVolumeAttachments( ).keySet( ).retainAll( volumeIds );
          return vm;
        } ).orElse( () -> Option.of( node.vm( ClusterVm.create( nodeVm, now ) ) ) ) ) );
      }
    } catch ( final Exception ex ) {
      handleRefreshException( "vms", ex );
    }

    // periodic cleanup
    final ClusterEucaConf conf = clusterEucaConfLoader.load( clock.millis( ) );
    final long unreportedTimeout = now - TimeUnit.SECONDS.toMillis( conf.getInstanceTimeout( ) );
    nodes.forEach( node -> node.getVms( ).forEach( vm -> {
      if ( vm.getReportedTimestamp( ) < unreportedTimeout ) {
        logger.info( "Removing metadata for unreported instance " + vm.getId( ) + " in state " + vm.getState( ) +
            " migration state " + vm.getMigrationState( ) + " for node " + node.getNode( ) );
        node.rvm( vm );
      } else if ( !ClusterVmMigrationState.none( ).equals( vm.getMigrationState( ) ) &&
          ClusterVmMigrationState.none( ).equals( vm.getDestinationMigrationState( ) ) &&
          !Strings.isNullOrEmpty( vm.getMigrationState( ).getSourceHost( ) ) &&
          vm.getMigrationState( ).getStateTimestamp( ).getOrElse( now ) < unreportedTimeout ) {
        logger.info( "Rolling back migration for instance " + vm.getId( ) + " on source node " +
            vm.getMigrationState( ).getSourceHost( ) + ", destination migration unreported" );
        migrationActions.add( Tuple.of( vm.getMigrationState( ).getSourceHost( ), vm.getId( ), "rollback" ) );
      }
    } ) );

    // handle actions
    for ( final Tuple3<String,String,String> nodeAndInstanceAndAction : migrationActions ) {
      final String node = nodeAndInstanceAndAction._1;
      final String instanceId = nodeAndInstanceAndAction._2;
      final String action = nodeAndInstanceAndAction._3;
      final ClusterNode clusterNode = node( node ).get( );
      final ClusterVm vmInfo = vm( instanceId ).get( );
      final InstanceType instance = vmToInstanceTypeWithMigration( vmInfo );
      nodeServiceWithAsyncErrorHandling( clusterNode ).migrateInstancesActionAsync( action, instance );
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  public void logStatus( ) {
    while ( true ) {
      final ClusterNodesStatus oldStatus = statusRef.get( );
      final ClusterNodesStatus newStatus = ClusterNodesStatus.from( this );
      if ( oldStatus.equals( newStatus ) ) {
        break;
      } else if ( statusRef.weakCompareAndSet( oldStatus, newStatus ) ) {
        for ( final String status : newStatus.status( ) ) {
          logger.info( "status: " + status );
        }
        break;
      }
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  public static InstanceType vmToInstanceTypeWithMigration( final ClusterVm vm ) {
    final InstanceType instance = vmToInstanceType( vm );
    final ClusterVmMigrationState migrationState = vm.getMigrationState( );
    if ( !ClusterVmMigrationState.none( ).equals( migrationState ) ) {
      instance.setMigrationStateName( migrationState.getState( ) );
      instance.setMigrationSource( migrationState.getSourceHost( ) );
      instance.setMigrationDestination( migrationState.getDestinationHost( ) );
    }
    return instance;
  }

  public static InstanceType vmToInstanceType( final ClusterVm vm ) {
    final InstanceType instance = new InstanceType( );
    instance.setInstanceId( vm.getId( ) );
    instance.setUuid( vm.getUuid( ) );
    instance.setReservationId( vm.getReservationId( ) );
    instance.setUserId( vm.getOwnerId( ) );
    instance.setOwnerId( vm.getOwnerId( ) );
    instance.setAccountId( vm.getAccountId( ) );
    instance.setLaunchIndex( String.valueOf( vm.getLaunchIndex( ) ) );
    instance.setLaunchTime( new Date( vm.getLaunchtime( ) ) );
    //instance.setExpiryTime( vm. );
    instance.setPlatform( vm.getPlatform( ) );
    instance.setStateName( vm.getState( ) );
    instance.setImageId( "" );
    instance.setKeyName( vm.getSshKeyValue( ) );
    instance.setInstanceType( vmToVirtualMachineType( vm ) );
    instance.setVolumes( Stream.ofAll( vm.getVolumeAttachments( ).values( ) ).map(
        attachment -> {
          final VolumeType volume = new VolumeType( );
          volume.setVolumeId( attachment.getVolumeId( ) );
          volume.setLocalDev( attachment.getDevice( ) );
          volume.setRemoteDev( attachment.getRemoteDevice( ) );
          volume.setState( attachment.getState( ) );
          return volume;
        }
    ).toJavaList( ArrayList::new ) );
    instance.setNetParams( vmToNetConfigType( vm ) );
    instance.setSecondaryNetConfig( Stream.ofAll( vm.getSecondaryInterfaceAttachments( ).values( ) )
        .map( ClusterNodes::vmInterfaceToNetConfigType )
        .toJavaList( ArrayList::new ) );

    return instance;
  }

  public static VirtualMachineType vmToVirtualMachineType( final ClusterVm vm ) {
    final VirtualMachineType vmType = new VirtualMachineType( );
    vmType.setName( vm.getVmType( ).getName( ) );
    vmType.setCores( vm.getVmType( ).getCores( ) );
    vmType.setDisk( vm.getVmType( ).getDisk( ) );
    vmType.setMemory( vm.getVmType( ).getMemory( ) );
    vmType.setVirtualBootRecord( Stream.ofAll( vm.getNodeBootRecord( ).getDevices( ) )
        .map( ClusterNodes::vmBootDeviceToVirtualBootRecordType )
        .toJavaList( ArrayList::new ) );
    return vmType;
  }

  public static VirtualBootRecordType vmBootDeviceToVirtualBootRecordType( final ClusterVmBootDevice bootDevice ) {
    final VirtualBootRecordType bootRecord = new VirtualBootRecordType( );
    bootRecord.setGuestDeviceName( bootDevice.getDevice( ) );
    bootRecord.setType( bootDevice.getType( ) );
    bootRecord.setSize( bootDevice.getSize( ) );
    bootRecord.setFormat( bootDevice.getFormat( ) );
    bootRecord.setId( bootDevice.getResource( ).map( Tuple2::_1 ).getOrElse( "none" ) );
    bootRecord.setResourceLocation( bootDevice.getResource( ).map( Tuple2::_2 ).getOrElse( "none" ) );
    return bootRecord;
  }

  public static NetConfigType vmToNetConfigType( final ClusterVm vm ) {
    final NetConfigType netConfig = vmInterfaceToNetConfigType( vm.getPrimaryInterface( ) );
    netConfig.setInterfaceId( MoreObjects.firstNonNull( netConfig.getInterfaceId( ), vm.getId( ) ) );
    return netConfig;
  }

  public static NetConfigType vmInterfaceToNetConfigType( final ClusterVmInterface vmInterface ) {
    final NetConfigType netConfig = new NetConfigType( );
    netConfig.setInterfaceId( vmInterface.getInterfaceId( ) );
    netConfig.setAttachmentId( vmInterface.getAttachmentId( ) );
    netConfig.setDevice( vmInterface.getDevice( ) );
    netConfig.setPrivateMacAddress( vmInterface.getMac( ) );
    netConfig.setPrivateIp( vmInterface.getPrivateAddress( ) );
    netConfig.setPublicIp( MoreObjects.firstNonNull( vmInterface.getPublicAddress( ), "0.0.0.0" ) );
    netConfig.setVlan( -1 );
    netConfig.setNetworkIndex( -1 );
    return netConfig;
  }

  private void handleRefreshException( final String refresh, final Exception ex ) {
    if ( !handleNodeException( "refreshing " + refresh, ex ) ) {
      logger.error( "Unexpected error refreshing " + refresh, ex );
    }
  }

  private boolean handleNodeException( final String activity, final Exception ex ) {
    return handleNodeException( Option.none( ), activity, ex );
  }

  private boolean handleNodeException( final Option<ClusterNode> node, final String activity, final Exception ex ) {
    boolean handled = false;
    final ConnectException connectException = Exceptions.findCause( ex, ConnectException.class );
    if ( connectException != null ) {
      if ( node.isDefined( ) ) {
        node.forEach( clusterNode -> {
          clusterNode.clearCapacity( );
          if ( !Component.State.NOTREADY.name( ).equals(clusterNode.getNodeStatus( ) ) ) {
            logger.info( "Error connecting to node " + clusterNode.getNode( ) + " for " + activity );
          }
          if ( clusterNode.setStatus(
              Component.State.NOTREADY.name( ),
              ClusterNode.NODE_STATUS_DETAIL_NOTREADY ) ) {
            logger.info( "Node status updated due to network issue " + clusterNode );
          }
        } );
      } else {
        logger.info( "Error connecting to node for " + activity );
      }
      handled = true;
    }
    return handled;
  }

  private static class ClusterNodesStatus {
    private final int instancesTotal;
    private final int instancesExtant;
    private final int instancesPending;
    private final int instancesTerminated;
    private final int nodesTotal;
    private final int nodesBusy;
    private final int nodesIdle;
    private final int nodesUnresponsive;

    ClusterNodesStatus(
        final int instancesExtant,
        final int instancesPending,
        final int instancesTerminated,
        final int nodesBusy,
        final int nodesIdle,
        final int nodesUnresponsive
    ) {
      this.instancesTotal = instancesExtant + instancesPending + instancesTerminated;
      this.instancesExtant = instancesExtant;
      this.instancesPending = instancesPending;
      this.instancesTerminated = instancesTerminated;
      this.nodesTotal = nodesBusy + nodesIdle + nodesUnresponsive;
      this.nodesBusy = nodesBusy;
      this.nodesIdle = nodesIdle;
      this.nodesUnresponsive = nodesUnresponsive;
    }

    static ClusterNodesStatus of(
        final int instancesExtant,
        final int instancesPending,
        final int instancesTerminated,
        final int nodesBusy,
        final int nodesIdle,
        final int nodesUnresponsive
    ) {
      return new ClusterNodesStatus(
          instancesExtant,
          instancesPending,
          instancesTerminated,
          nodesBusy,
          nodesIdle,
          nodesUnresponsive );
    }

    static ClusterNodesStatus from( final ClusterNodes clusterNodes ) {
      int instancesExtant = 0;
      int instancesPending = 0;
      int instancesTerminated = 0;
      int nodesBusy = 0;
      int nodesIdle = 0;
      int nodesUnresponsive = 0;

      for ( final ClusterNode node : clusterNodes.nodes( ) )  {
        boolean activeInstance = false;
        for ( final ClusterVm vm : node.getVms( ) ) {
          if ( "Pending".equals( vm.getState( ) ) ) {
            activeInstance = true;
            instancesPending++;
          } else if ( "Extant".equals( vm.getState( ) ) ) {
            activeInstance = true;
            instancesExtant++;
          } else {
            instancesTerminated++;
          }
        }

        if ( !Component.State.ENABLED.name( ).equals( node.getNodeStatus( ) ) &&
            !Component.State.STOPPED.name( ).equals( node.getNodeStatus( ) ) ) {
          nodesUnresponsive++;
        } else if ( activeInstance ) {
          nodesBusy++;
        } else {
          nodesIdle++;
        }
      }

      return of(
          instancesExtant,
          instancesPending,
          instancesTerminated,
          nodesBusy,
          nodesIdle,
          nodesUnresponsive
      );
    }

    static ClusterNodesStatus none( ) {
      return of( 0, 0, 0, 0, 0, 0 );
    }

    Iterable<String> status( ) {
      return ImmutableList.of(
        String.format( "instances: %05d (%05d extant + %05d pending + %05d terminated)",
            instancesTotal, instancesExtant, instancesPending, instancesTerminated ),
        String.format( "    nodes: %05d (%05d busy + %05d idle + %05d unresponsive)",
            nodesTotal, nodesBusy, nodesIdle, nodesUnresponsive )
      );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ClusterNodesStatus that = (ClusterNodesStatus) o;
      return instancesTotal == that.instancesTotal &&
          instancesExtant == that.instancesExtant &&
          instancesPending == that.instancesPending &&
          instancesTerminated == that.instancesTerminated &&
          nodesTotal == that.nodesTotal &&
          nodesBusy == that.nodesBusy &&
          nodesIdle == that.nodesIdle &&
          nodesUnresponsive == that.nodesUnresponsive;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          instancesTotal, instancesExtant, instancesPending, instancesTerminated,
          nodesTotal, nodesBusy, nodesIdle, nodesUnresponsive );
    }
  }
}
