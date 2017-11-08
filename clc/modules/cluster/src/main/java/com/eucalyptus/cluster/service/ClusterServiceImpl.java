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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.broadcast.NIInstance;
import com.eucalyptus.cluster.common.broadcast.NetworkInfo;
import com.eucalyptus.cluster.common.msgs.AttachedVolume;
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
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksType;
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
import com.eucalyptus.cluster.common.msgs.ModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.NcRebootInstanceType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceType;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.common.msgs.ResourceType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.common.msgs.VmDescribeResponseType;
import com.eucalyptus.cluster.common.msgs.VmDescribeType;
import com.eucalyptus.cluster.common.msgs.VmRunResponseType;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.cluster.service.conf.ClusterEucaConf;
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.node.ClusterNodes;
import com.eucalyptus.cluster.service.scheduler.ScheduleResource;
import com.eucalyptus.cluster.service.scheduler.Scheduler;
import com.eucalyptus.cluster.service.scheduler.Schedulers;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.eucalyptus.cluster.service.vm.VmVolumeAttachment;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Assert;
import com.eucalyptus.util.FUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import javaslang.collection.Stream;

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

  @Inject
  public ClusterServiceImpl(
      final ClusterEucaConfLoader clusterEucaConfLoader,
      final ClusterNodes clusterNodes
  ) {
    this.clusterEucaConfLoader = Assert.notNull( clusterEucaConfLoader, "clusterEucaConfLoader" );
    this.clusterNodes = Assert.notNull( clusterNodes, "clusterNodes" );
  }

  @Override
  public BroadcastNetworkInfoResponseType broadcastNetworkInfo(
      final BroadcastNetworkInfoType request
  ) {
    try {
      final Stream<ClusterNode> nodes = nodes( );

      //update address assignments
      String lastVersion = lastBroadcastVersion.get( );
      if ( ( lastVersion == null || !lastVersion.equals( request.getVersion( ) ) ) &&
          lastBroadcastVersion.compareAndSet( lastVersion, request.getVersion( ) ) ) {
        final String infoText = new String( B64.standard.dec( request.getNetworkInfo( ) ), StandardCharsets.UTF_8 );
        final JAXBContext jc = JAXBContext.newInstance( NetworkInfo.class.getPackage( ).getName( ) );
        final NetworkInfo info = (NetworkInfo) jc.createUnmarshaller( ).unmarshal( new StringReader( infoText ) );
        for ( final ClusterNode node : nodes ) {
          final NodeService nodeService = nodeService( node );
          for ( final VmInfo vmInfo : node.getVms( ) ) {
            for ( final NIInstance instance : info.getInstances( ) ) {
              if ( vmInfo.getId( ).equals( instance.getName( ) ) ) {
                final String requestedPublicIp = MoreObjects.firstNonNull( instance.getPublicIp( ), "0.0.0.0" );
                final String lastSeenPublicIp = MoreObjects.firstNonNull( vmInfo.getPrimaryInterface( ).getPublicAddress( ), "0.0.0.0" );
                if ( !requestedPublicIp.equals( lastSeenPublicIp ) ) {
                  nodeService.assignAddress( instance.getName( ), requestedPublicIp ); //TODO:STEVE: threading?
                }
              }
            }
          }
        }
      }

      //broadcast to ncs
      for ( final ClusterNode node : nodes ) {
        final NodeService nodeService = nodeService( node );
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
  public ClusterMigrateInstancesResponseType migrateInstancesResponseType(
      final ClusterMigrateInstancesType request
  ) {
    return request.getReply( );
  }

  @Override
  public ClusterBundleInstanceResponseType bundleInstance(
      final ClusterBundleInstanceType request
  ) {
    final ClusterBundleInstanceResponseType response = request.getReply( );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NodeService nodeService = nodeService( node );
        final NcBundleInstanceType bundleInstance = new NcBundleInstanceType( );
        bundleInstance.setArchitecture( request.getArchitecture( ) );
        bundleInstance.setBucketName( request.getBucket( ) );
        bundleInstance.setCloudPublicKey( request.getAwsAccessKeyId( ) ); //TODO??
        bundleInstance.setFilePrefix( request.getPrefix( ) );
        bundleInstance.setInstanceId( request.getInstanceId( ) );
        bundleInstance.setObjectStorageURL( request.getUrl( ) );
        bundleInstance.setS3Policy( request.getUploadPolicy( ) );
        bundleInstance.setS3PolicySig( request.getUploadPolicySignature( ) );
        bundleInstance.setUserPublicKey( request.getUserKey( ) );
        nodeService.bundleInstance( bundleInstance );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterBundleRestartInstanceResponseType bundleRestartInstance(
      final ClusterBundleRestartInstanceType request
  ) {
    final ClusterBundleRestartInstanceResponseType response = request.getReply( );
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        final NodeService nodeService = nodeService( node );
        final NcRebootInstanceType rebootInstance = new NcRebootInstanceType( ); //TODO?
        rebootInstance.setInstanceId( request.getInstanceId( ) );
        nodeService.rebootInstance( rebootInstance );
      } );
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  @Override
  public ClusterCancelBundleTaskResponseType cancelBundleTask(
      final ClusterCancelBundleTaskType request
  ) {
    final ClusterCancelBundleTaskResponseType response = request.getReply( );
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
  public ClusterDescribeBundleTasksResponseType describeBundleTasks(
      final ClusterDescribeBundleTasksType request
  ) {
    return request.getReply( );
  }

  @Override
  public ClusterRebootInstancesResponseType rebootInstances(
      final ClusterRebootInstancesType request
  ) {
    final ClusterRebootInstancesResponseType response = request.getReply( );
    try {
      request.getInstancesSet( ).forEach( vmId -> clusterNodes.nodeForVm( vmId ).forEach( node -> {
        nodeService( node ).rebootInstance( vmId );
      } ) );
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
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        nodeService( node ).startInstance( request.getInstanceId( ) );
      } );
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
    try {
      clusterNodes.nodeForVm( request.getInstanceId( ) ).forEach( node -> {
        nodeService( node ).stopInstance( request.getInstanceId( ) );
      } );
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
    try {
      clusterNodes.refreshResources( ); //TODO oob

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
        final NodeType nodeType = new NodeType( );
        nodeType.setServiceTag( node.getServiceTag( ) );
        nodeType.setIqn( node.getIqn( ) );
        nodeType.setHypervisor( node.getHypervisor( ) );
        response.getNodes( ).add( nodeType );
        response.getServiceTags( ).add( node.getServiceTag( ) ); //TODO: should we populate this??
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

    final Iterable<String> instanceIds = request.getInstanceIds( );
    final int history = request.getHistorySize( );
    final int interval = request.getCollectionIntervalTimeMs( );
    try {
      clusterNodes.refreshSensors( System.currentTimeMillis( ), history, interval );

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
    //noinspection deprecation
    response.markWinning( ).setUserId( "eucalyptus" ); //TODO:remove?

    try {
      clusterNodes.refreshVms( );

      for ( final ClusterNode node : nodes( ) ) {
        for ( final VmInfo vmInfo : node.getVms( ) ) {
          final VmTypeInfo vmTypeInfo = new VmTypeInfo( );
          vmTypeInfo.setName( vmInfo.getInstanceTypeName( ) );
          vmTypeInfo.setCores( vmInfo.getInstanceTypeCores( ) );
          vmTypeInfo.setDisk( vmInfo.getInstanceTypeDisk( ) );
          vmTypeInfo.setMemory( vmInfo.getInstanceTypeMemory( ) );
          //TODO: need VBR here?

          final NetworkConfigType netConfig = new NetworkConfigType( );
          netConfig.setMacAddress( vmInfo.getPrimaryInterface( ).getMac( ) );
          netConfig.setIpAddress( vmInfo.getPrimaryInterface( ).getPrivateAddress( ) );
          netConfig.setIgnoredPublicIp( vmInfo.getPrimaryInterface( ).getPublicAddress( ) );
          netConfig.setVlan( -1 );
          netConfig.setNetworkIndex( -1L );

          final List<NetworkConfigType> secondaryNetConfigList = Lists.newArrayList( );
          if ( vmInfo.getVpcId( ) != null ) {
            netConfig.setInterfaceId( Strings.nullToEmpty( vmInfo.getPrimaryInterface( ).getInterfaceId( ) ) );
            netConfig.setAttachmentId( Strings.nullToEmpty( vmInfo.getPrimaryInterface( ).getAttachmentId( ) ) );

            // secondary interfaces here
            vmInfo.getSecondaryInterfaceAttachments( ).values( ).stream( ).map( fvi -> {
              NetworkConfigType secNetConfig = new NetworkConfigType( fvi.getInterfaceId( ), fvi.getDevice( ) );
              secNetConfig.setAttachmentId( fvi.getAttachmentId( ) );
              secNetConfig.setMacAddress( fvi.getMac( ) );
              secNetConfig.setIpAddress( fvi.getPrivateAddress( ) );
              secNetConfig.setIgnoredPublicIp( fvi.getPublicAddress( ) );
              return secNetConfig;
            } ).collect( Collectors.toCollection( ( ) -> secondaryNetConfigList ) );
          } else {
            netConfig.setInterfaceId( "" ); //TODO why are these required?
            netConfig.setAttachmentId( "" );
          }

          final com.eucalyptus.cluster.common.msgs.VmInfo msgVmInfo = new com.eucalyptus.cluster.common.msgs.VmInfo( );
          msgVmInfo.setInstanceId( vmInfo.getId( ) );
          msgVmInfo.setUuid( vmInfo.getUuid( ) );
          msgVmInfo.setInstanceType( vmTypeInfo );
          msgVmInfo.setLaunchTime( new Date( vmInfo.getLaunchtime( ) ) );
          msgVmInfo.setLaunchIndex( String.valueOf( vmInfo.getLaunchIndex( ) ) );
          msgVmInfo.setStateName( vmInfo.getState( ) );
          msgVmInfo.setNetParams( netConfig );
          msgVmInfo.setSecondaryNetConfigList( secondaryNetConfigList );
          msgVmInfo.setOwnerId( vmInfo.getOwnerId( ) );
          msgVmInfo.setAccountId( vmInfo.getAccountId( ) );
          msgVmInfo.setReservationId( vmInfo.getReservationId( ) );
          msgVmInfo.setServiceTag( node.getServiceTag( ) );
          //TODO: user data
          msgVmInfo.setNetworkBytes( 0L );
          msgVmInfo.setBlockBytes( 0L );
          msgVmInfo.setPlatform( vmInfo.getPlatform( ) );
          msgVmInfo.setBundleTaskStateName( "none" );
          msgVmInfo.setBundleTaskProgress( 0d );
          msgVmInfo.setGuestStateName( "poweredOn" );
          msgVmInfo.setMigrationStateName( "none" );
          //TODO: security group names?

          for ( final VmVolumeAttachment volumeAttachment : vmInfo.getVolumeAttachments( ).values( ) ) {
            final AttachedVolume volume = new AttachedVolume(  );
            volume.setVolumeId( volumeAttachment.getVolumeId( ) );
            volume.setInstanceId( vmInfo.getId( ) );
            volume.setDevice( volumeAttachment.getDevice( ) );
            volume.setRemoteDevice( volumeAttachment.getRemoteDevice( ) );
            volume.setAttachTime( new Date( volumeAttachment.getAttachmentTimestamp( ) ) );
            volume.setStatus( volumeAttachment.getState( ) );
            msgVmInfo.getVolumes().add( volume );
          }

          response.getVms( ).add( msgVmInfo );
        }
      }
    } catch ( Exception e ) {
      logger.error( e, e );
    }
    return response;
  }

  /**
   * TODO
   * Image cache: http://%s:8776/%s?
   */
  @Override
  public VmRunResponseType runVm(
      final VmRunType request
  ) {
    final VmRunResponseType response = request.getReply( );
    final ClusterEucaConf conf = clusterEucaConfLoader.load( );
    try ( final ScheduleResource scheduleResource = Schedulers.context( ) ) {
      final ClusterNode node = schedulerForName.apply( conf.getScheduler( ) )
          .schedule( nodes( ), request.getVmTypeInfo( ) ).getOrElseThrow( ( ) -> new RuntimeException( "resources" ) );
      final long currentTime = System.currentTimeMillis( );

      final VmInfo vmInfo = VmInfo.create( request, currentTime );
      node.vm( vmInfo );

      final VmTypeInfo vmTypeInfo = new VmTypeInfo( );
      vmTypeInfo.setName( vmInfo.getInstanceTypeName( ) );
      vmTypeInfo.setCores( vmInfo.getInstanceTypeCores( ) );
      vmTypeInfo.setDisk( vmInfo.getInstanceTypeDisk( ) );
      vmTypeInfo.setMemory( vmInfo.getInstanceTypeMemory( ) );

      final NetworkConfigType netConfig = new NetworkConfigType( );
      netConfig.setMacAddress( vmInfo.getPrimaryInterface( ).getMac( ) );
      netConfig.setIpAddress( vmInfo.getPrimaryInterface( ).getPrivateAddress( ) );
      netConfig.setIgnoredPublicIp( "0.0.0.0" );
      netConfig.setVlan( -1 );
      netConfig.setNetworkIndex( -1L );
      netConfig.setInterfaceId( Strings.nullToEmpty( vmInfo.getPrimaryInterface( ).getInterfaceId( ) ) );
      netConfig.setAttachmentId( Strings.nullToEmpty( vmInfo.getPrimaryInterface( ).getAttachmentId( ) ) );

      final com.eucalyptus.cluster.common.msgs.VmInfo msgVmInfo = new com.eucalyptus.cluster.common.msgs.VmInfo( );
      msgVmInfo.setInstanceId( vmInfo.getId( ) );
      msgVmInfo.setUuid( vmInfo.getUuid( ) );
      msgVmInfo.setInstanceType( vmTypeInfo );
      msgVmInfo.setKeyValue( vmInfo.getSshKeyValue( ) );
      msgVmInfo.setLaunchTime( new Date( vmInfo.getLaunchtime( ) ) );
      msgVmInfo.setLaunchIndex( String.valueOf( vmInfo.getLaunchIndex( ) ) );
      msgVmInfo.setStateName( vmInfo.getState( ) );
      msgVmInfo.setNetParams( netConfig );
      msgVmInfo.setOwnerId( vmInfo.getOwnerId( ) );
      msgVmInfo.setAccountId( vmInfo.getAccountId( ) );
      msgVmInfo.setReservationId( vmInfo.getReservationId( ) );
      msgVmInfo.setServiceTag( node.getServiceTag( ) );
      msgVmInfo.setNetworkBytes( 0L );
      msgVmInfo.setBlockBytes( 0L );
      msgVmInfo.setPlatform( vmInfo.getPlatform( ) );
      msgVmInfo.setBundleTaskProgress( 0d );
      msgVmInfo.setMigrationStateName( "none" );
      response.getVms( ).add( msgVmInfo );

      final NodeService nodeService = nodeService( node );
      final NcRunInstanceType ncRun = new NcRunInstanceType( );

      ncRun.setOwnerId( vmInfo.getOwnerId( ) );
      ncRun.setAccountId( vmInfo.getAccountId( ) );
      ncRun.setReservationId( vmInfo.getReservationId( ) );
      ncRun.setInstanceId( vmInfo.getId( ) );
      ncRun.setUuid( vmInfo.getUuid( ) );

      final VirtualMachineType vmType = new VirtualMachineType( );
      vmType.setCores( request.getVmTypeInfo( ).getCores( ) );
      vmType.setDisk( request.getVmTypeInfo( ).getDisk( ) );
      vmType.setMemory( request.getVmTypeInfo( ).getMemory( ) );
      vmType.setName( request.getVmTypeInfo( ).getName( ) );
      final ArrayList<VirtualBootRecordType> vbr = Lists.newArrayList( );
      if ( request.getVmTypeInfo( ).getVirtualBootRecord( ) != null ) {
        for ( final VirtualBootRecord rec : request.getVmTypeInfo( ).getVirtualBootRecord( ) ){
          VirtualBootRecordType vbrt = new VirtualBootRecordType( );
          vbrt.setResourceLocation( rec.getResourceLocation( ) );
          vbrt.setGuestDeviceName( rec.getGuestDeviceName( ) );
          vbrt.setSize( rec.getSize( ) );
          vbrt.setFormat( rec.getFormat( ) );
          vbrt.setId( rec.getId( ) );
          vbrt.setType( rec.getType( ) );
          vbr.add( vbrt );
        }
      }
      vmType.setVirtualBootRecord( vbr );
      ncRun.setInstanceType( vmType );

      ncRun.setKeyName( request.getKeyInfo( ) != null ? request.getKeyInfo( ).getValue( ) : null );

      final NetConfigType networkConfigType = new NetConfigType( );
      networkConfigType.setInterfaceId( vmInfo.getId( ) );
      networkConfigType.setDevice( 0 );
      networkConfigType.setPrivateMacAddress( request.getMacAddress( ) );
      networkConfigType.setPrivateIp( request.getPrivateAddress( ) );
      networkConfigType.setPublicIp( "0.0.0.0" );
      networkConfigType.setVlan( -1 );
      networkConfigType.setNetworkIndex( -1 );
      ncRun.setNetParams( networkConfigType );

      ncRun.setUserData( request.getUserData( ) );
      ncRun.setCredential( request.getCredential( ) );
      ncRun.setLaunchIndex( String.valueOf( vmInfo.getLaunchIndex( ) ) );
      ncRun.setPlatform( vmInfo.getPlatform( ) );
      //ncRun.setExpiryTime(  );
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
    return request.getReply( );
  }

  @Override
  public ClusterDescribeServicesResponseType describeServices( final ClusterDescribeServicesType request ) {
    final ClusterDescribeServicesResponseType response = request.getReply( );
    nodes( ).forEach( node -> {
      for ( final ServiceId serviceId : request.getServices( ) ) {
        if ( node.getNode( ).equals( serviceId.getName( ) ) ) {
          final ServiceStatusType serviceStatus = new ServiceStatusType( );
          serviceStatus.setServiceId( serviceId );
          serviceStatus.setLocalEpoch( request.get_epoch( ) );
          serviceStatus.setLocalState( node.getNodeStatus( ) );
          serviceStatus.setDetails( Lists.newArrayList( "the node is operating normally" ) );
          response.getServiceStatuses( ).add( serviceStatus );
        }
      }
    } );
    return response;
  }

  @Override
  public ClusterDisableServiceResponseType disableService( final ClusterDisableServiceType request ) {
    clusterNodes.status( "DISABLED", Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterEnableServiceResponseType enableService( final ClusterEnableServiceType request ) {
    clusterNodes.status( "ENABLED", Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterStartServiceResponseType startService( final ClusterStartServiceType request ) {
    clusterNodes.status( "NOTREADY", Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  @Override
  public ClusterStopServiceResponseType stopService( final ClusterStopServiceType request ) {
    clusterNodes.status( "STOPPED", Stream.ofAll( request.getServices( ) ).map( ServiceId::getName ) );
    return request.getReply( );
  }

  private Stream<ClusterNode> nodes( ) {
    return clusterNodes.nodes( );
  }

  private NodeService nodeService( final ClusterNode node ) {
    return clusterNodes.nodeService( node );
  }

}
