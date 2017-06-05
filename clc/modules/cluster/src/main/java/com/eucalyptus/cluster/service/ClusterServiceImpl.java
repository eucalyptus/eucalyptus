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
package com.eucalyptus.cluster.service;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.broadcast.NIInstance;
import com.eucalyptus.cluster.common.broadcast.NIProperty;
import com.eucalyptus.cluster.common.broadcast.NetworkInfo;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeServicesType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDisableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterEnableServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputType;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStartServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopServiceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopServiceType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.NcBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceType;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.common.msgs.ResourceType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.common.msgs.VmDescribeResponseType;
import com.eucalyptus.cluster.common.msgs.VmDescribeType;
import com.eucalyptus.cluster.common.msgs.VmInfo;
import com.eucalyptus.cluster.common.msgs.VmRunResponseType;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.cluster.service.conf.ClusterEucaConf;
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader;
import com.eucalyptus.cluster.service.migration.Migrations;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.node.ClusterNodeActivities;
import com.eucalyptus.cluster.service.node.ClusterNodes;
import com.eucalyptus.cluster.service.scheduler.ScheduleResource;
import com.eucalyptus.cluster.service.scheduler.Scheduler;
import com.eucalyptus.cluster.service.scheduler.Schedulers;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.eucalyptus.cluster.service.vm.ClusterVmBootDevice;
import com.eucalyptus.cluster.service.vm.ClusterVmInterface;
import com.eucalyptus.cluster.service.vm.ClusterVms;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.FUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
@ComponentNamed("clusterService")
public class ClusterServiceImpl implements ClusterService, ClusterEmpyreanService {

  private static final Logger logger = Logger.getLogger( ClusterServiceImpl.class );

  private final AtomicReference<String> lastBroadcastVersion = new AtomicReference<>( );

  private final Function<String,Scheduler> schedulerForName = FUtils.memoizeLast( Schedulers::forName );

  private final ClusterEucaConfLoader clusterEucaConfLoader;
  private final ClusterNodes clusterNodes;
  private final ClusterNodeActivities clusterNodeActivities;
  private final Clock clock;

  @SuppressWarnings( "WeakerAccess" )
  public ClusterServiceImpl(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodes clusterNodes,
      final ClusterNodeActivities clusterNodeActivities,
      final Clock clock
  ) {
    this.clusterEucaConfLoader = Assert.notNull( clusterEucaConfLoader, "clusterEucaConfLoader" );
    this.clusterNodes = Assert.notNull( clusterNodes, "clusterNodes" );
    this.clusterNodeActivities = Assert.notNull( clusterNodeActivities, "clusterNodeActivities" );
    this.clock = Assert.notNull( clock, "clock" );
  }

  @Inject
  public ClusterServiceImpl(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodes clusterNodes,
      final ClusterNodeActivities clusterNodeActivities
  ) {
    this( clusterEucaConfLoader, clusterNodes, clusterNodeActivities, Clock.systemDefaultZone( ) );
  }

  @Override
  public BroadcastNetworkInfoResponseType broadcastNetworkInfo(
      final BroadcastNetworkInfoType request
  ) {
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Broadcast network info requested, version " + request.getVersion( ) +
          ", applied" + request.getAppliedVersion() );
    }
    try {
      final Stream<ClusterNode> nodes = nodes( );

      //update address assignments
      String lastVersion = lastBroadcastVersion.get( );
      if ( ( lastVersion == null || !lastVersion.equals( request.getVersion( ) ) ) &&
          lastBroadcastVersion.compareAndSet( lastVersion, request.getVersion( ) ) ) {
        final String infoText = new String( B64.standard.dec( request.getNetworkInfo( ) ), StandardCharsets.UTF_8 );
        final JAXBContext jc = JAXBContext.newInstance( NetworkInfo.class.getPackage( ).getName( ) );
        final NetworkInfo info = (NetworkInfo) jc.createUnmarshaller( ).unmarshal( new StringReader( infoText ) );
        final Optional<NIProperty> property = info.getConfiguration( ).getProperties( ).stream( )
                .filter( prop -> "mode".equals( prop.getName( ) ) )
                .findFirst( );
        final String networkMode = property.map( prop -> Iterables.get( prop.getValues( ), 0 ) ).orElse( "EDGE" );
        for ( final ClusterNode node : nodes ) {
          final NodeService nodeService = clusterNodes.nodeServiceWithAsyncErrorHandling( node );
          for ( final ClusterVm vmInfo : node.getVms( ) ) {
            for ( final NIInstance instance : info.getInstances( ) ) {
              if ( vmInfo.getId( ).equals( instance.getName( ) ) ) {
                if ( "EDGE".equals( networkMode ) ) {
                  final String requestedPublicIp = MoreObjects.firstNonNull( instance.getPublicIp( ), "0.0.0.0" );
                  final String lastSeenPublicIp = MoreObjects.firstNonNull( vmInfo.getPrimaryInterface( ).getPublicAddress( ), "0.0.0.0" );
                  if ( !requestedPublicIp.equals( lastSeenPublicIp ) ) {
                    nodeService.assignAddressAsync( instance.getName( ), requestedPublicIp );
                  }
                } else if ( "VPCMIDO".equals( networkMode ) ) {
                  if ( vmInfo.getVpcId( ) == null && instance.getVpc( ) != null ) {
                    vmInfo.setVpcId( instance.getVpc( ) );
                  }
                }
              }
            }
          }
        }
      }

      //broadcast to ncs
      for ( final ClusterNode node : nodes ) {
        final NodeService nodeService = clusterNodes.nodeServiceWithAsyncErrorHandling( node );
        final NcBroadcastNetworkInfoType broadcastNetworkInfo = new NcBroadcastNetworkInfoType( );
        broadcastNetworkInfo.setNetworkInfo( request.getNetworkInfo( ) );
        nodeService.broadcastNetworkInfoAsync( broadcastNetworkInfo );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    return request.getReply( );
  }

  @Override
  public ClusterAttachVolumeResponseType attachVolume(
      final ClusterAttachVolumeType request
  ) {
    final ClusterAttachVolumeResponseType response = request.getReply( );
    logger.info( String.format( "Attach volume requested, instance %s, volume %s, device %s",
        request.getInstanceId( ), request.getVolumeId( ), request.getDevice( ) ) );
    clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
      final NodeService nodeService = nodeService( node );
      final NcAttachVolumeType attachVolume = new NcAttachVolumeType( );
      attachVolume.setInstanceId( request.getInstanceId( ) );
      attachVolume.setVolumeId( request.getVolumeId( ) );
      attachVolume.setLocalDev( request.getDevice( ) );
      attachVolume.setRemoteDev( request.getRemoteDevice( ) );
      nodeService.attachVolume( attachVolume );
    } );
    return response;
  }

  @Override
  public ClusterDetachVolumeResponseType detachVolume(
      final ClusterDetachVolumeType request
  ) {
    final ClusterDetachVolumeResponseType response = request.getReply( );
    logger.info( String.format( "Detach volume requested, instance %s, volume %s",
        request.getInstanceId( ), request.getVolumeId( ) ) );
    clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
      final NodeService nodeService = nodeService( node );
      final NcDetachVolumeType detachVolume = new NcDetachVolumeType( );
      detachVolume.setInstanceId( request.getInstanceId( ) );
      detachVolume.setVolumeId( request.getVolumeId( ) );
      detachVolume.setLocalDev( request.getDevice( ) );
      detachVolume.setRemoteDev( request.getRemoteDevice( ) );
      detachVolume.setForce( request.getForce( ) );
      nodeService.detachVolume( detachVolume );
    } );
    return response;
  }

  @Override
  public ClusterGetConsoleOutputResponseType getConsoleOutput(
      final ClusterGetConsoleOutputType request
  ) {
    final ClusterGetConsoleOutputResponseType response = request.getReply( );
    logger.info( String.format( "Get console output requested, instance %s", request.getInstanceId( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NcGetConsoleOutputResponseType getConsoleOutputResponse =
            nodeService( node ).getConsoleOutput( request.getInstanceId( ) );
        response.setInstanceId( request.getInstanceId( ) );
        response.setTimestamp( new Date( ) );
        response.setOutput( getConsoleOutputResponse.getConsoleOutput( ) );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterMigrateInstancesResponseType migrateInstances(
      final ClusterMigrateInstancesType request
  ) {
    final ClusterMigrateInstancesResponseType response = request.getReply( );
    logger.info( String.format( "Migrate instances requested, instance %s, source host %s",
        request.getInstanceId( ), request.getSourceHost( ) ) );
    if ( !Strings.isNullOrEmpty( request.getInstanceId( ) ) || !Strings.isNullOrEmpty( request.getSourceHost( ) ) ) {
      final Option<ClusterNode> sourceNode;
      final Set<String> instanceIds = Sets.newTreeSet( );
      if ( !Strings.isNullOrEmpty( request.getInstanceId( ) ) ) { // migrating a specific instance
        sourceNode = clusterNodes.nodeForVm( request.getInstanceId( ) );
        instanceIds.add( request.getInstanceId( ) );
      } else { // migrating off host
        sourceNode = clusterNodes.node( request.getSourceHost( ) );
        instanceIds.addAll( sourceNode.map( n -> n.getVms( ).filter( vm -> vm.getState( ).equals( "Extant" ) ).map( ClusterVm::getId ) ).getOrElse( Stream.empty( ) ).toJavaSet( ) );
      }
      if ( sourceNode.isDefined( ) && !instanceIds.isEmpty( ) ) {
        // schedule a destination node, using provided white or black list (user configured scheduler)
        final ClusterEucaConf conf = clusterEucaConfLoader.load( clock.millis( ) );
        final List<Tuple2<ClusterNode,InstanceType>> destinationAndInstances = Lists.newArrayList( );
        try ( final ScheduleResource scheduleResource = Schedulers.context( ) ) {
          final Predicate<ClusterNode> nodePredicate =
              nodePredicate( request.getAllowHosts( ), request.getDestinationHosts( ), sourceNode.get().getNode( ) );
          for ( final String instanceId : instanceIds ) {
            final ClusterVm vm = sourceNode.get( ).vm( instanceId ).getOrElseThrow( () -> new RuntimeException( instanceId ) );
            final InstanceType instance = ClusterNodes.vmToInstanceType( vm );
            final ClusterNode destinationNode = schedulerForName.apply( conf.getScheduler( ) )
                  .schedule( nodes( ).filter( nodePredicate ), vm.getVmType( ) ).getOrElseThrow( () -> new RuntimeException( "resources" ) );
            instance.setMigrationStateName( "none" );
            instance.setMigrationSource( sourceNode.get( ).getNode( ) );
            instance.setMigrationDestination( destinationNode.getNode( ) );
            destinationAndInstances.add( Tuple.of( destinationNode, instance ) );
          }

          scheduleResource.commit( );
        }

        // generate migration credentials
        final String credentials = Migrations.generateCredential( );

        // call prepare on source and update instance state if successful
        clusterNodes.nodeService( sourceNode.get( ) ).migrateInstancesPrepare(
            credentials,
            request.getResourceLocations( ),
            destinationAndInstances.stream( ).map( Tuple2::_2 ).collect( Collectors.toCollection( ArrayList::new ) ) );

        for ( final Tuple2<ClusterNode,InstanceType> destinationAndInstance : destinationAndInstances ) {
          final ClusterNode destinationNode = destinationAndInstance._1;
          final InstanceType instance = destinationAndInstance._2;
          clusterNodes.nodeServiceWithAsyncErrorHandling( destinationNode ).migrateInstancesPrepareAsync(
              credentials,
              request.getResourceLocations( ),
              Lists.newArrayList( instance ) );
        }
      } else {
        response.markFailed( );
      }
    } else {
      response.markFailed( );
    }

    return response;
  }

  @Override
  public ClusterBundleInstanceResponseType bundleInstance(
      final ClusterBundleInstanceType request
  ) {
    final ClusterBundleInstanceResponseType response = request.getReply( );
    logger.info( String.format( "Bundle instance requested, instance %s, bucket %s",
        request.getInstanceId( ), request.getBucket( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NodeService nodeService = nodeService( node );
        final NcBundleInstanceType bundleInstance = new NcBundleInstanceType( );
        bundleInstance.setArchitecture( request.getArchitecture( ) );
        bundleInstance.setBucketName( request.getBucket( ) );
        bundleInstance.setFilePrefix( request.getPrefix( ) );
        bundleInstance.setInstanceId( request.getInstanceId( ) );
        bundleInstance.setObjectStorageURL( request.getUrl( ) );
        bundleInstance.setS3Policy( request.getUploadPolicy( ) );
        bundleInstance.setS3PolicySig( request.getUploadPolicySignature( ) );
        bundleInstance.setUserPublicKey( request.getAwsAccessKeyId( ) );
        nodeService.bundleInstance( bundleInstance );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
      response.markFailed( );
    }
    return response;
  }

  @Override
  public ClusterBundleRestartInstanceResponseType bundleRestartInstance(
      final ClusterBundleRestartInstanceType request
  ) {
    final ClusterBundleRestartInstanceResponseType response = request.getReply( );
    logger.info( String.format( "Bundle restart instance requested, instance %s", request.getInstanceId( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NodeService nodeService = nodeService( node );
        final NcBundleRestartInstanceType restartInstance = new NcBundleRestartInstanceType( );
        restartInstance.setInstanceId( request.getInstanceId( ) );
        nodeService.bundleRestartInstance( restartInstance );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
      response.markFailed( );
    }
    return response;
  }

  @Override
  public ClusterCancelBundleTaskResponseType cancelBundleTask(
      final ClusterCancelBundleTaskType request
  ) {
    final ClusterCancelBundleTaskResponseType response = request.getReply( );
    logger.info( String.format( "Cancel bundle task requested, instance %s, bundle %s",
        request.getInstanceId( ), request.getBundleId( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NodeService nodeService = nodeService( node );
        final NcCancelBundleTaskType cancelBundleTask = new NcCancelBundleTaskType( );
        cancelBundleTask.setInstanceId( request.getInstanceId( ) );
        nodeService.cancelBundleTask( cancelBundleTask );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterRebootInstancesResponseType rebootInstances(
      final ClusterRebootInstancesType request
  ) {
    final ClusterRebootInstancesResponseType response = request.getReply( );
    logger.info( "Reboot instances requested, instances " +
        request.getInstancesSet( ).stream( ).collect( Collectors.joining( "," ) ) );
    try {
      request.getInstancesSet( ).forEach( vmId -> clusterNodes.nodeForVm( vmId ).forEach( node ->
          nodeService( node ).rebootInstance( vmId ) ) );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterStartInstanceResponseType startInstance(
      final ClusterStartInstanceType request
  ) {
    final ClusterStartInstanceResponseType response = request.getReply( );
    logger.info( String.format( "Start instance requested, instance %s", request.getInstanceId( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node ->
          nodeService( node ).startInstance( request.getInstanceId( ) ) );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterStopInstanceResponseType stopInstance(
      final ClusterStopInstanceType request
  ) {
    final ClusterStopInstanceResponseType response = request.getReply( );
    logger.info( String.format( "Stop instance requested, instance %s", request.getInstanceId( ) ) );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node ->
          nodeService( node ).stopInstance( request.getInstanceId( ) ) );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterTerminateInstancesResponseType terminateInstances(
      final ClusterTerminateInstancesType request
  ) {
    final ClusterTerminateInstancesResponseType response = request.getReply( );
    logger.info( "Terminate instances requested, instances " +
        request.getInstancesSet( ).stream( ).collect( Collectors.joining( "," ) ) );
    try {
      request.getInstancesSet( ).forEach( vmId -> clusterNodes.nodeForVm( vmId ).forEach( node -> {
        nodeService( node ).terminateInstance( vmId, false );
        response.setTerminated( true );
      } ) );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public DescribeResourcesResponseType describeResources(
      final DescribeResourcesType request
  ) {
    final DescribeResourcesResponseType response = request.getReply( );
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Describe resources requested, instance types " +
          request.getInstanceTypes( ).stream( ).map( VmTypeInfo::getName ).collect( Collectors.joining( "," ) ) );
    }
    try {
      for ( final VmTypeInfo info : request.getInstanceTypes( ) ) {
        int available = 0;
        int max = 0;
        for ( final ClusterNode node : nodes( ) ) {
          int nodeAvailableByCore = info.getCores( ) <= 0 ? 0 : node.getCoresAvailable( ) / info.getCores( );
          int nodeMaxByCore = info.getCores( ) <= 0 ? 0 : node.getCoresTotal( ) / info.getCores( );

          int nodeAvailableByDisk = info.getDisk( ) <= 0 ? 0 : node.getDiskAvailable( ) / info.getDisk( );
          int nodeMaxByDisk = info.getDisk( ) <= 0 ? 0 : node.getDiskTotal( ) / info.getDisk( );

          int nodeAvailableByMem = info.getMemory( ) <= 0 ? 0 : node.getMemoryAvailable( ) / info.getMemory( );
          int nodeMaxByMem = info.getMemory( ) <= 0 ? 0 : node.getMemoryTotal( ) / info.getMemory( );

          available += Math.min( nodeAvailableByCore, Math.min( nodeAvailableByDisk, nodeAvailableByMem ) );
          max += Math.min( nodeMaxByCore, Math.min( nodeMaxByDisk, nodeMaxByMem ) );
        }
        final ResourceType resourceType = new ResourceType( );
        resourceType.setInstanceType( info );
        resourceType.setAvailableInstances( available );
        resourceType.setMaxInstances( max );
        response.getResources( ).add( resourceType );
      }

      for ( final ClusterNode node : nodes( ) ) {
        if ( node.getHypervisor( ) == null ) continue; // basic validity check
        final NodeType nodeType = new NodeType( );
        nodeType.setServiceTag( node.getServiceTag( ) );
        nodeType.setIqn( node.getIqn( ) );
        nodeType.setHypervisor( node.getHypervisor( ) );
        response.getNodes( ).add( nodeType );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    return response;
  }

  @Override
  public DescribeSensorsResponseType describeSensors(
      final DescribeSensorsType request
  ) {
    final DescribeSensorsResponseType response = request.getReply( );
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Describe sensors requested, instances " +
          request.getInstanceIds( ).stream( ).collect( Collectors.joining( "," ) ) );
    }
    final Iterable<String> instanceIds = request.getInstanceIds( );
    final int history = request.getHistorySize( );
    final int interval = request.getCollectionIntervalTimeMs( );
    if ( clusterNodeActivities.configureSensorPolling( history, interval ) ) {
      logger.info( "Updated sensor polling with history size " + history + ", interval " + interval + "ms" );
    }
    try {
      for ( final String instanceId : instanceIds ) {
        clusterNodes.vm( instanceId ).forEach( vmInfo -> {
          final SensorsResourceType sensorsResource = new SensorsResourceType( );
          sensorsResource.setResourceName( vmInfo.getId( ) );
          sensorsResource.setResourceType( "instance" );
          sensorsResource.setResourceUuid( vmInfo.getUuid( ) );
          sensorsResource.setMetrics( vmInfo.getMetrics( ) );
          response.getSensorsResources( ).add( sensorsResource );
        } );
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public VmDescribeResponseType describeVms(
      final VmDescribeType request
  ) {
    final VmDescribeResponseType response = request.getReply( );
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Describe vms requested, instances " +
          request.getInstancesSet( ).stream( ).collect( Collectors.joining( "," ) ) );
    }

    //noinspection deprecation
    response.markWinning( ).setUserId( "eucalyptus" );

    try {
      for ( final ClusterNode node : nodes( ) ) {
        for ( final ClusterVm vm : node.getVms( ) ) {
          final VmInfo vmInfo = ClusterVms.vmToVmInfo( vm );
          vmInfo.setServiceTag( node.getServiceTag( ) );
          response.getVms( ).add( vmInfo );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  /**
   *
   */
  @Override
  public VmRunResponseType runVm(
      final VmRunType request
  ) {
    final VmRunResponseType response = request.getReply( );
    logger.info( String.format( "Run vm requested, instance %s, type %s ",
        request.getInstanceId( ), request.getVmTypeInfo( ).getName( ) ) );
    final ClusterEucaConf conf = clusterEucaConfLoader.load( clock.millis( ) );
    try ( final ScheduleResource scheduleResource = Schedulers.context( ) ) {
      final long currentTime = clock.millis( );
      final ClusterVm vm = ClusterVm.create( request, currentTime );
      final ClusterNode node = schedulerForName.apply( conf.getScheduler( ) )
          .schedule( nodes( ), vm.getVmType( ) ).getOrElseThrow( ( ) -> new RuntimeException( "resources" ) );
      node.vm( vm );
      final VmInfo vmInfo = ClusterVms.vmToVmInfo( vm );
      vmInfo.setServiceTag( node.getServiceTag( ) );
      response.getVms( ).add( vmInfo );

      final NodeService nodeService = nodeService( node );
      final NcRunInstanceType ncRun = new NcRunInstanceType( );

      //noinspection deprecation
      ncRun.setUserId( request.getUserId( ) );
      ncRun.setOwnerId( vm.getOwnerId( ) );
      ncRun.setAccountId( vm.getAccountId( ) );
      ncRun.setReservationId( vm.getReservationId( ) );
      ncRun.setInstanceId( vm.getId( ) );
      ncRun.setUuid( vm.getUuid( ) );

      final VirtualMachineType vmType = ClusterNodes.vmToVirtualMachineType( vm );
      final ArrayList<VirtualBootRecordType> vbr = Lists.newArrayList( );
      if ( request.getVmTypeInfo( ).getVirtualBootRecord( ) != null ) {
        for ( final VirtualBootRecord rec : request.getVmTypeInfo( ).getVirtualBootRecord( ) ){
          vbr.add( ClusterNodes.vmBootDeviceToVirtualBootRecordType( ClusterVmBootDevice.from( rec ) ) );
        }
      }
      vmType.setVirtualBootRecord( vbr );
      ncRun.setInstanceType( vmType );

      ncRun.setKeyName( request.getKeyInfo( ) != null ? request.getKeyInfo( ).getValue( ) : null );
      final NetConfigType networkConfigType = ClusterNodes.vmToNetConfigType( vm );
      ncRun.setNetParams( networkConfigType );
      if ( request.getSecondaryNetConfigList( ) != null ) {
        final List<NetConfigType> secondaryNetConfigs = Lists.newArrayList( );
        final Set<Integer> attachmentDevices = Sets.newTreeSet( vm.getSecondaryInterfaceAttachments( ).keySet( ) );
        for ( final Integer attachmentDevice : attachmentDevices ) {
          final ClusterVmInterface vmInterface = vm.getSecondaryInterfaceAttachments( ).get( attachmentDevice );
          if ( vmInterface != null ) {
            secondaryNetConfigs.add( ClusterNodes.vmInterfaceToNetConfigType( vmInterface ) );
          }
        }
        ncRun.getSecondaryNetConfig( ).addAll( secondaryNetConfigs );
      }
      ncRun.setUserData( request.getUserData( ) );
      ncRun.setCredential( request.getCredential( ) );
      ncRun.setLaunchIndex( String.valueOf( vm.getLaunchIndex( ) ) );
      ncRun.setPlatform( vm.getPlatform( ) );
//      <n:expiryTime>2017-05-30T20:32:24.339Z</n:expiryTime>
//      ncRun.setExpiryTime( request. );
      ncRun.setRootDirective( request.getRootDirective( ) );
      ncRun.getGroupNames( ).addAll( request.getNetworkNames() );
      ncRun.getGroupIds( ).addAll( request.getNetworkIds( ) );

      nodeService.runInstance( ncRun );
      scheduleResource.commit( );
    } catch ( Exception e ) {
      logger.error( e, e );
    }

    return response;
  }

  @Override
  public ModifyNodeResponseType modifyNode(
      final ModifyNodeType request
  ) {
    final ModifyNodeResponseType response = request.getReply( );
    logger.info( "Modify node "+request.getNodeName()+" requested " + request.getStateName( ) );
    try {
      clusterNodes.node( request.getNodeName( ) ).forEach( node -> {
        final NcModifyNodeType modifyNode = new NcModifyNodeType( );
        modifyNode.setStateName( request.getStateName( ) );
        nodeService( node ).modifyNode( modifyNode );
      } );
    } catch ( Exception e ) {
      response.markFailed( );
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterDescribeServicesResponseType describeServices( final ClusterDescribeServicesType request ) {
    final ClusterDescribeServicesResponseType response = request.getReply( );
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Describe services requested" + servicesDescription( ", services ", request.getServices( ) ) );
    }
    nodes( ).forEach( node -> {
      for ( final ServiceId serviceId : request.getServices( ) ) {
        if ( node.getNode( ).equals( serviceId.getName( ) ) ) {
          final ServiceStatusType serviceStatus = new ServiceStatusType( );
          serviceStatus.setServiceId( serviceId );
          serviceStatus.setLocalEpoch( Topology.epoch( ) );
          serviceStatus.setLocalState( node.getNodeStatus( ) );
          serviceStatus.setDetails( Lists.newArrayList( node.getNodeStatusDetail( ) ) );
          response.getServiceStatuses( ).add( serviceStatus );
        }
      }
    } );
    return response;
  }

  @Override
  public ClusterDisableServiceResponseType disableService( final ClusterDisableServiceType request ) {
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Disable services requested" + servicesDescription( ", services ", request.getServices( ) ) );
    }
    clusterNodes.status( Component.State.DISABLED.name( ), Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterEnableServiceResponseType enableService( final ClusterEnableServiceType request ) {
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Enable services requested" + servicesDescription( ", services ", request.getServices( ) ) );
    }
    clusterNodes.status( Component.State.ENABLED.name( ), Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterStartServiceResponseType startService( final ClusterStartServiceType request ) {
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Start services requested" + servicesDescription( ", services ", request.getServices( ) ) );
    }
    clusterNodes.status( Component.State.NOTREADY.name( ), Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterStopServiceResponseType stopService( final ClusterStopServiceType request ) {
    if ( logger.isDebugEnabled( ) ) {
      logger.debug( "Stop services requested" + servicesDescription( ", services ", request.getServices( ) ) );
    }
    clusterNodes.status( Component.State.STOPPED.name( ), Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  private Stream<ClusterNode> nodes( ) {
    return clusterNodes.nodes( );
  }

  private Predicate<ClusterNode> nodePredicate( final Boolean allowHosts, final Collection<String> nodes, final String skipNode ) {
    final boolean whitelist = MoreObjects.firstNonNull( allowHosts, true );
    final Set<String> nodeSet = Sets.newHashSet( MoreObjects.firstNonNull( nodes, Collections.emptySet( ) ) );
    return n ->
        !skipNode.equals( n.getNode( ) ) && (
        ( whitelist && nodeSet.contains( n.getNode( ) ) ) ||
        ( !whitelist && !nodeSet.contains( n.getNode( ) ) ) );
  }

  private NodeService nodeService( final ClusterNode node ) {
    return clusterNodes.nodeService( node );
  }

  private static String servicesDescription( final String prefix, final Collection<ServiceId> services ) {
    String desc = services.stream( ).map( ServiceId::getName ).collect( Collectors.joining( "," ) );
    if ( !desc.isEmpty( ) ) {
      desc = prefix + desc;
    }
    return desc;
  }
}
