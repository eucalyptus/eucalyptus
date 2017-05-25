/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.cluster.service.conf.ClusterEucaConf;
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader;
import com.eucalyptus.cluster.service.scheduler.Scheduler;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.eucalyptus.cluster.service.vm.VmVolumeAttachment;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 * Tracks resources, service state and metrics for a clusters nodes.
 */
@ComponentNamed
public class ClusterNodes {

  private static final int DEFAULT_PORT = 8775;

  private final ConcurrentMap<String,ClusterNode> nodesByIp = Maps.newConcurrentMap( );
  private final AtomicReference<Integer> nodePort = new AtomicReference<>( DEFAULT_PORT );
  private final AtomicLong lastSensorRefresh = new AtomicLong( );
  private final ClusterEucaConfLoader clusterEucaConfLoader;
  private final ClusterNodeServiceFactory nodeServiceFactory;

  @Inject
  public ClusterNodes(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodeServiceFactory nodeServiceFactory
  ) {
    this.clusterEucaConfLoader = Assert.notNull( clusterEucaConfLoader, "clusterEucaConfLoader" );
    this.nodeServiceFactory = Assert.notNull( nodeServiceFactory, "nodeServiceFactory" );
  }

  public ClusterNode getClusterNode( final String node ) {
    return nodesByIp.computeIfAbsent( node, ClusterNode::new );
  }

  public int getNodePort( ) {
    return MoreObjects.firstNonNull( nodePort.get( ), DEFAULT_PORT );
  }

  public NodeService nodeService( final ClusterNode node ) {
    final int port = getNodePort( );
    return nodeServiceFactory.nodeService( node, port );
  }

  public Option<VmInfo> vm( final String vmId ) {
    return nodeWithVm( vmId ).map( Tuple2::_2 );
  }

  public Option<ClusterNode> nodeForVm( final String vmId ) {
    return nodeWithVm( vmId ).map( Tuple2::_1 );
  }

  public Option<Tuple2<ClusterNode,VmInfo>> nodeWithVm( final String vmId ) {
    for ( final ClusterNode clusterNode : nodes( ) ) {
      for ( final VmInfo vmInfo : clusterNode.getVms( ) ) {
        if ( vmInfo.getId( ).equals( vmId ) ) {
          return Option.some( Tuple.of( clusterNode, vmInfo ) );
        }
      }
    }
    return Option.none( );
  }

  public Stream<ClusterNode> nodes( ) {
    final ClusterEucaConf conf = clusterEucaConfLoader.load( );
    return Stream.ofAll( conf.getNodes( ) ).sorted( ).map( this::getClusterNode );
  }

  public void status( final String status, final Stream<String> nodes ) {
    nodes.forEach( node -> nodes( )
        .filter( clusterNode -> clusterNode.getNode( ).equals( node ) )
        .forEach( clusterNode -> clusterNode.setNodeStatus( status ) ) );
  }

  public void refreshResources( ) throws Exception {
    final Stream<ClusterNode> nodes = nodes( );
    final List<CheckedListenableFuture<NcDescribeResourceResponseType>> replyFutures = Lists.newArrayList( );
    for ( final ClusterNode node : nodes ) {
      final NodeService nodeService = nodeService( node );
      final NcDescribeResourceType ncDescribeResource = new NcDescribeResourceType( );
      replyFutures.add( nodeService.describeResourceAsync( ncDescribeResource ) );
    }

    final Iterator<NcDescribeResourceResponseType> replies = Futures.allAsList( replyFutures ).get( ).iterator( );
    for ( final ClusterNode node : nodes ) {
      final NcDescribeResourceResponseType reply = replies.next( );
      node.setHypervisor( reply.getHypervisor( ) );
      node.setIqn( reply.getIqn( ) );
      node.setMigrationCapable( reply.getMigrationCapable( ) );
      node.setNodeStatus( reply.getNodeStatus( ) );
      node.setPublicSubnets( reply.getPublicSubnets( ) );
      Scheduler.withLock( () -> { //TODO accounting not correct here for reserved/pending resources
        node.setCoresAvailable( MoreObjects.firstNonNull( reply.getNumberOfCoresAvailable( ), 0 ) );
        node.setCoresTotal( MoreObjects.firstNonNull( reply.getNumberOfCoresMax( ), 0 ) );
        node.setDiskAvailable( MoreObjects.firstNonNull( reply.getDiskSizeAvailable( ), 0 ) );
        node.setDiskTotal( MoreObjects.firstNonNull( reply.getDiskSizeMax( ), 0 ) );
        node.setMemoryAvailable( MoreObjects.firstNonNull( reply.getMemorySizeAvailable( ), 0 ) );
        node.setMemoryTotal( MoreObjects.firstNonNull( reply.getMemorySizeMax( ), 0 ) );
        Scheduler.adjust( node );
        return true;
      } );
    }
  }

  /**
   * Refresh sensors if necessary based on interval
   */
  public void refreshSensors(
      final long now,
      final int historySize,
      final int collectionIntervalTimeMs
  ) throws Exception {
    final long lastRefresh = lastSensorRefresh.get( );
    if ( ( (now - lastRefresh) >= collectionIntervalTimeMs ) && lastSensorRefresh.compareAndSet( lastRefresh, now ) ) {
      final Stream<ClusterNode> nodes = nodes( );
      final List<CheckedListenableFuture<NcDescribeSensorsResponseType>> replyFutures = Lists.newArrayList( );
      for ( final ClusterNode node : nodes ) {
        final NodeService nodeService = nodeService( node );
        final NcDescribeSensorsType describeSensors = new NcDescribeSensorsType( );
        describeSensors.setInstanceIds( node.getVms( ).map( VmInfo::getId ).toJavaList( ArrayList::new ) ); //TODO batching
        describeSensors.setHistorySize( historySize );
        describeSensors.setCollectionIntervalTimeMs( collectionIntervalTimeMs );
        replyFutures.add( nodeService.describeSensorsAsync( describeSensors ) );
      }

      for ( final NcDescribeSensorsResponseType reply : Futures.allAsList( replyFutures ).get( ) ) {
        for ( final SensorsResourceType sensorsResource : reply.getSensorsResources() ) {
          if ( "instance".equals( sensorsResource.getResourceType( ) ) ) {
            vm( sensorsResource.getResourceName( ) ).forEach(
                vmInfo -> vmInfo.setMetrics( sensorsResource.getMetrics( ) ) );
          }
        }
      }
    }
  }

  /**
   * Refresh vm info from each node on request
   */
  public void refreshVms( ) throws Exception {
    final Stream<ClusterNode> nodes = nodes( );
    final List<CheckedListenableFuture<NcDescribeInstancesResponseType>> replyFutures = Lists.newArrayList( );
    for ( final ClusterNode node : nodes ) {
      final NodeService nodeService = nodeService( node );
      final NcDescribeInstancesType ncDescribeInstances = new NcDescribeInstancesType( );
      replyFutures.add( nodeService.describeInstancesAsync( ncDescribeInstances ) );
    }

    final Iterator<NcDescribeInstancesResponseType> replies = Futures.allAsList( replyFutures ).get( ).iterator( );
    for ( final ClusterNode node : nodes ) {
      final NcDescribeInstancesResponseType reply = replies.next( );
      reply.getInstances( ).forEach( nodeVm -> {
        vm( nodeVm.getInstanceId( ) ).map( vm -> {
          vm.state( nodeVm.getStateName( ), System.currentTimeMillis( ) );
          vm.getPrimaryInterface( ).assignPublic( nodeVm.getNetParams( ).getPublicIp( ) );
          Set<String> volumeIds = Sets.newHashSet( );
          nodeVm.getVolumes( ).forEach( volume -> {
            volumeIds.add( volume.getVolumeId( ) );
            final VmVolumeAttachment attachment = vm.getVolumeAttachments( ).get( volume.getVolumeId( ) );
            if ( attachment == null || !attachment.getDevice( ).equals( volume.getLocalDev( ) ) ||
                !attachment.getState( ).equals( volume.getState( ) ) ) {
              vm.getVolumeAttachments( ).put( volume.getVolumeId( ), new VmVolumeAttachment(
                  attachment != null ? attachment.getAttachmentTimestamp( ) : System.currentTimeMillis( ),
                  volume.getVolumeId( ),
                  volume.getLocalDev( ),
                  volume.getRemoteDev( ),
                  volume.getState( )
              ) );
            }
          } );
          vm.getVolumeAttachments( ).keySet( ).retainAll( volumeIds );
          return vm;
        } ).orElse( ( ) -> Option.of( node.vm( VmInfo.create( nodeVm ) ) ) );
      } );
    }
  }
}
