/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cluster.service.fake;

import static com.google.common.base.MoreObjects.firstNonNull;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDescribeBundleTasksType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.ModifyNodeType;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cluster.NIInstance;
import com.eucalyptus.cluster.NINetworkInterface;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.BroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterGetConsoleOutputType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterTerminateInstancesType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.VmDescribeResponseType;
import com.eucalyptus.cluster.common.msgs.VmDescribeType;
import com.eucalyptus.cluster.common.msgs.VmRunResponseType;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.eucalyptus.cluster.service.vm.VmInterface;
import com.eucalyptus.cluster.service.vm.VmVolumeAttachment;
import com.eucalyptus.cluster.service.ClusterService;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.eucalyptus.cluster.common.msgs.AttachedVolume;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterMigrateInstancesType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterRebootInstancesType;
import com.eucalyptus.cluster.common.msgs.MetricCounterType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsValuesType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.common.msgs.ResourceType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceType;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;

/**
 *
 */
@SuppressWarnings( "unused" )
@ComponentNamed
public class FakeClusterService implements ClusterService {
  private static final Logger logger = Logger.getLogger( FakeClusterService.class );

  @Override
  public BroadcastNetworkInfoResponseType broadcastNetworkInfo( BroadcastNetworkInfoType request ) {
    try {
      final String networkInfo = new String( BaseEncoding.base64().decode( request.getNetworkInfo( ) ), StandardCharsets.UTF_8 );
      final JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" );
      final NetworkInfo info = (NetworkInfo) jc.createUnmarshaller( ).unmarshal( new InputSource( new StringReader( networkInfo ) ) );
      instances:
      for ( final NIInstance instance : info.getInstances( ) ) {
        for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
          for ( final VmInfo vmInfo : node.getVms( ) ) {
            if ( vmInfo.getId( ).equals( instance.getName( ) ) ) {
              vmInfo.setVpcId( instance.getVpc( ) );
              vmInfo.getPrimaryInterface( ).assignPublic( instance.getPublicIp( ) );
              vmInfo.getSecondaryInterfaceAttachments( ).keySet( ).retainAll(
                  instance.getNetworkInterfaces( ).stream( )
                      .map( NINetworkInterface::getDeviceIndex ).collect( Collectors.toSet( ) ) );
              instance.getNetworkInterfaces( ).stream( )
                  .filter( ni -> ni.getDeviceIndex( ) != 0 )
                  .collect( Collectors.toMap(
                      NINetworkInterface::getDeviceIndex,
                      ni -> new VmInterface(
                          ni.getName( ),
                          ni.getAttachmentId( ),
                          ni.getDeviceIndex( ),
                          ni.getMacAddress( ),
                          ni.getPrivateIp( ),
                          ni.getPublicIp( ) ),
                      (v1, v2) -> v1,
                      vmInfo::getSecondaryInterfaceAttachments
                  ) );

              continue instances;
            }
          }
        }
      }
    } catch ( Exception e ) {
      logger.error( "Error processing network broadcast", e );
    }

    return request.getReply( );
  }

  @Override
  public ClusterAttachVolumeResponseType attachVolume( ClusterAttachVolumeType request ) {
    final ClusterAttachVolumeResponseType response = request.getReply( );

    out:
    for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
      for ( final VmInfo vmInfo : node.getVms( ) ) {
        if ( vmInfo.getId( ).equals( request.getInstanceId( ) ) ) {
          final VmVolumeAttachment fakeVolumeAttachment = new VmVolumeAttachment(
              FakeClusterState.currentTime( ),
              request.getVolumeId( ),
              request.getDevice( ),
              request.getRemoteDevice( ),
              "attached"
          );
          final VmVolumeAttachment existingFakeVolumeAttachment =
              vmInfo.getVolumeAttachments( ).putIfAbsent( request.getVolumeId( ), fakeVolumeAttachment );
          if ( existingFakeVolumeAttachment == null || existingFakeVolumeAttachment.equals( fakeVolumeAttachment ) ) {
            final VmVolumeAttachment attachment = firstNonNull( existingFakeVolumeAttachment, fakeVolumeAttachment );
            response.getAttachedVolume( ).setVolumeId( attachment.getVolumeId( ) );
            response.getAttachedVolume( ).setInstanceId( vmInfo.getId( ) );
            response.getAttachedVolume( ).setDevice( attachment.getDevice( ) );
            response.getAttachedVolume( ).setRemoteDevice( attachment.getRemoteDevice( ) );
            response.getAttachedVolume( ).setAttachTime( new Date( attachment.getAttachmentTimestamp( ) ) );
            response.getAttachedVolume( ).setStatus( attachment.getState( ) );
          } else {
            response.set_return( false );
          }
          break out;
        }
      }
    }

    return response;
  }

  @Override
  public ClusterDetachVolumeResponseType detachVolume( ClusterDetachVolumeType request ) {
    final ClusterDetachVolumeResponseType response = request.getReply( );

    out:
    for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
      for ( final VmInfo vmInfo : node.getVms( ) ) {
        if ( vmInfo.getId( ).equals( request.getInstanceId( ) ) ) {
          final VmVolumeAttachment existingFakeVolumeAttachment =
              vmInfo.getVolumeAttachments( ).get( request.getVolumeId( ) );
          if ( existingFakeVolumeAttachment != null ) {
            final VmVolumeAttachment newFakeVolumeAttachment = new VmVolumeAttachment(
                existingFakeVolumeAttachment.getAttachmentTimestamp( ),
                existingFakeVolumeAttachment.getVolumeId( ),
                existingFakeVolumeAttachment.getDevice( ),
                existingFakeVolumeAttachment.getRemoteDevice( ),
                "detached"
            );
            vmInfo.getVolumeAttachments( ).replace( request.getVolumeId( ), existingFakeVolumeAttachment, newFakeVolumeAttachment );
            response.getDetachedVolume( ).setVolumeId( newFakeVolumeAttachment.getVolumeId( ) );
            response.getDetachedVolume( ).setInstanceId( vmInfo.getId( ) );
            response.getDetachedVolume( ).setDevice( newFakeVolumeAttachment.getDevice( ) );
            response.getDetachedVolume( ).setRemoteDevice( newFakeVolumeAttachment.getRemoteDevice( ) );
            response.getDetachedVolume( ).setAttachTime( new Date( newFakeVolumeAttachment.getAttachmentTimestamp( ) ) );
            response.getDetachedVolume( ).setStatus( newFakeVolumeAttachment.getState( ) );
          }
          break out;
        }
      }
    }

    return response;
  }

  @Override
  public ClusterGetConsoleOutputResponseType getConsoleOutput( ClusterGetConsoleOutputType request ) {
    final ClusterGetConsoleOutputResponseType response = request.getReply( );
    response.setOutput( BaseEncoding.base64( ).encode( ( "\nConsole output for fake instance " + request.getInstanceId( ) + "\n" ).getBytes( StandardCharsets.UTF_8 ) ) );
    return response;
  }

  @Override
  public ClusterMigrateInstancesResponseType migrateInstancesResponseType( ClusterMigrateInstancesType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterRebootInstancesResponseType rebootInstances( ClusterRebootInstancesType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterTerminateInstancesResponseType terminateInstances( ClusterTerminateInstancesType request ) {
    final ClusterTerminateInstancesResponseType response = request.getReply( );

    boolean terminated = false;
    for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
      for ( final VmInfo vmInfo : node.getVms( ) ) {
        if ( request.getInstancesSet( ).contains( vmInfo.getId( ) ) ) {
          terminated = true;
          vmInfo.setState( "Teardown" );
          vmInfo.setStateTimestamp( FakeClusterState.currentTime( ) );
          vmInfo.getVolumeAttachments( ).clear( );
        }
      }
    }

    response.setTerminated( terminated );
    return response;
  }

  @Override
  public DescribeResourcesResponseType describeResources( DescribeResourcesType request ) {
    final DescribeResourcesResponseType response = request.getReply( );

    for ( final VmTypeInfo info : request.getInstanceTypes( ) ) {
      int available = 0;
      int max = 0;
      for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
        max += info.getCores( ) <= 0 ? 0 : node.getCores( ) / info.getCores( );
        int usedCores = 0;
        for ( final VmInfo vmInfo : node.getVms( ) ) {
          if ( !vmInfo.getState( ).equals( "Teardown" ) ) {
            usedCores += vmInfo.getInstanceTypeCores( );
          } else if ( vmInfo.getStateTimestamp( ) + TimeUnit.MINUTES.toMillis( 5 ) < FakeClusterState.currentTime( ) ) {
            node.getVms( ).remove( vmInfo );
          }
        }
        available += info.getCores( ) <= 0 ? 0 : ( node.getCores( ) - usedCores ) / info.getCores( );
      }
      final ResourceType resourceType = new ResourceType( );
      resourceType.setInstanceType( info );
      resourceType.setAvailableInstances( available );
      resourceType.setMaxInstances( max );
      response.getResources( ).add( resourceType );
    }

    for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
      final NodeType nodeType = new NodeType( );
      nodeType.setServiceTag( node.getServiceTag( ) );
      nodeType.setIqn( node.getIqn( ) );
      nodeType.setHypervisor( "KVM" );
      response.getNodes( ).add( nodeType );
    }

    return response;
  }

  @Override
  public DescribeSensorsResponseType describeSensors( DescribeSensorsType request ) {
    final DescribeSensorsResponseType response = request.getReply( );

    final Iterable<String> instanceIds = request.getInstanceIds( );
    final int history = request.getHistorySize( );
    final long interval = request.getCollectionIntervalTimeMs( ).longValue( );

    final long currentTime = FakeClusterState.currentTime( );
    final long mostRecentTimestamp = currentTime - ( currentTime % interval );

    for ( final String instanceId : instanceIds ) {
      VmInfo vmInfo = null;
      find_fake: for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
        for ( final VmInfo fakeVmInfo : node.getVms( ) ) {
          if ( fakeVmInfo.getId( ).equals( instanceId ) ) {
            vmInfo = fakeVmInfo;
            break find_fake;
          }
        }
      }
      if ( vmInfo == null ) {
        continue;
      }

      final long sequenceNumber = ( mostRecentTimestamp - vmInfo.getLaunchtime( ) ) / interval;
      final SensorsResourceType sensorsResource = new SensorsResourceType( );
      sensorsResource.setResourceName( instanceId );
      sensorsResource.setResourceType( "instance" );
      sensorsResource.setResourceUuid( vmInfo.getUuid( ) );
      for ( final MetricsToFake metric : MetricsToFake.values( ) ) {
        final MetricsResourceType metricsResource = new MetricsResourceType( );
        metricsResource.setMetricName( metric.name( ) );
        final MetricCounterType metricCounter = new MetricCounterType( );
        metricCounter.setCollectionIntervalMs( interval );
        metricCounter.setType( metric.isSummation( ) ? "summation" : "latest" );
        for ( final String dimension : metric.getDimensions( ) ) {
          final MetricDimensionsType metricDimensions = new MetricDimensionsType( );
          metricDimensions.setDimensionName( dimension );
          metricDimensions.setSequenceNum( sequenceNumber );
          for ( long i = Math.max( 0, sequenceNumber - history ); i < sequenceNumber; i++ ) {
            final MetricDimensionsValuesType metricDimensionsValues = new MetricDimensionsValuesType( );
            metricDimensionsValues.setTimestamp( new Date( mostRecentTimestamp - ( ( sequenceNumber - ( i + 1 ) ) * interval ) ) );
            metricDimensionsValues.setValue( ( i * interval ) * metric.getMultipler( ) );
            metricDimensions.getValues( ).add( metricDimensionsValues );
          }
          metricCounter.getDimensions( ).add( metricDimensions );
        }
        metricsResource.getCounters( ).add( metricCounter );
        sensorsResource.getMetrics( ).add( metricsResource );
      }
      response.getSensorsResources( ).add( sensorsResource );
    }

    return response;
  }

  @Override
  public ClusterStartInstanceResponseType startInstance( ClusterStartInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterStopInstanceResponseType stopInstance( ClusterStopInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public VmDescribeResponseType describeVms( VmDescribeType request ) {
    final VmDescribeResponseType response = request.getReply( );
    //noinspection deprecation
    response.markWinning( ).setUserId( "eucalyptus" );

    for ( final ClusterNode node : FakeClusterState.nodes( ) ) {
      for ( final VmInfo fakeVmInfo : node.getVms( ) ) {
        final VmTypeInfo vmTypeInfo = new VmTypeInfo( );
        vmTypeInfo.setName( fakeVmInfo.getInstanceTypeName( ) );
        vmTypeInfo.setCores( fakeVmInfo.getInstanceTypeCores( ) );
        vmTypeInfo.setDisk( fakeVmInfo.getInstanceTypeDisk( ) );
        vmTypeInfo.setMemory( fakeVmInfo.getInstanceTypeMemory( ) );
        //TODO: need VBR here?

        final NetworkConfigType netConfig = new NetworkConfigType( );
        netConfig.setMacAddress( fakeVmInfo.getPrimaryInterface( ).getMac( ) );
        netConfig.setIpAddress( fakeVmInfo.getPrimaryInterface( ).getPrivateAddress( ) );
        netConfig.setIgnoredPublicIp( fakeVmInfo.getPrimaryInterface( ).getPublicAddress( ) );
        netConfig.setVlan( -1 );
        netConfig.setNetworkIndex( -1L );

        final List<NetworkConfigType> secondaryNetConfigList = Lists.newArrayList( );
        if ( fakeVmInfo.getVpcId( ) != null ) {
          netConfig.setInterfaceId( Strings.nullToEmpty( fakeVmInfo.getPrimaryInterface( ).getInterfaceId( ) ) );
          netConfig.setAttachmentId( Strings.nullToEmpty( fakeVmInfo.getPrimaryInterface( ).getAttachmentId( ) ) );

          // secondary interfaces here
          fakeVmInfo.getSecondaryInterfaceAttachments( ).values( ).stream( ).map( fvi -> {
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

        final com.eucalyptus.cluster.common.msgs.VmInfo vmInfo = new com.eucalyptus.cluster.common.msgs.VmInfo( );
        vmInfo.setInstanceId( fakeVmInfo.getId( ) );
        vmInfo.setUuid( fakeVmInfo.getUuid( ) );
        vmInfo.setInstanceType( vmTypeInfo );
        vmInfo.setLaunchTime( new Date( fakeVmInfo.getLaunchtime( ) ) );
        vmInfo.setLaunchIndex( String.valueOf( fakeVmInfo.getLaunchIndex( ) ) );
        vmInfo.setStateName( fakeVmInfo.getState( ) );
        vmInfo.setNetParams( netConfig );
        vmInfo.setSecondaryNetConfigList( secondaryNetConfigList );
        vmInfo.setOwnerId( fakeVmInfo.getOwnerId( ) );
        vmInfo.setAccountId( fakeVmInfo.getAccountId( ) );
        vmInfo.setReservationId( fakeVmInfo.getReservationId( ) );
        vmInfo.setServiceTag( fakeVmInfo.getServiceTag( ) );
        //TODO: user data
        vmInfo.setNetworkBytes( 0L );
        vmInfo.setBlockBytes( 0L );
        vmInfo.setPlatform( fakeVmInfo.getPlatform( ) );
        vmInfo.setBundleTaskStateName( "none" );
        vmInfo.setBundleTaskProgress( 0d );
        vmInfo.setGuestStateName( "poweredOn" );
        vmInfo.setMigrationStateName( "none" );
        //TODO: security group names?

        for ( final VmVolumeAttachment volumeAttachment : fakeVmInfo.getVolumeAttachments( ).values( ) ) {
          final AttachedVolume volume = new AttachedVolume(  );
          volume.setVolumeId( volumeAttachment.getVolumeId( ) );
          volume.setInstanceId( fakeVmInfo.getId( ) );
          volume.setDevice( volumeAttachment.getDevice( ) );
          volume.setRemoteDevice( volumeAttachment.getRemoteDevice( ) );
          volume.setAttachTime( new Date( volumeAttachment.getAttachmentTimestamp( ) ) );
          volume.setStatus( volumeAttachment.getState( ) );
          vmInfo.getVolumes().add( volume );
        }

        response.getVms( ).add( vmInfo );
      }
    }

    return response;
  }

  @Override
  public VmRunResponseType runVm( VmRunType request ) {
    final VmRunResponseType response = request.getReply( );

    final ClusterNode node = FakeClusterState.nodes( ).get( 0 );
    final long currentTime = FakeClusterState.currentTime( );

    final VmInfo fakeVmInfo = new VmInfo(
        request.getInstanceId( ),
        request.getUuid( ),
        request.getReservationId( ),
        request.getLaunchIndex( ),
        request.getVmTypeInfo( ).getName( ),
        request.getVmTypeInfo( ).getCores( ),
        request.getVmTypeInfo( ).getDisk( ),
        request.getVmTypeInfo( ).getMemory( ),
        request.getPlatform( ),
        request.getKeyInfo( ).getValue( ),
        currentTime,
        "Extant",
        currentTime,
        null,
        request.getPrimaryEniAttachmentId( ),
        request.getMacAddress( ),
        request.getPrivateAddress( ),
        null,
        request.getOwnerId( ),
        request.getAccountId( ),
        node.getServiceTag( ),
        null
    );
    //TODO: process VBR here
    //request.getVmTypeInfo( ).getVirtualBootRecord().get( 0 ).
    node.getVms( ).add( fakeVmInfo );

    final VmTypeInfo vmTypeInfo = new VmTypeInfo( );
    vmTypeInfo.setName( fakeVmInfo.getInstanceTypeName( ) );
    vmTypeInfo.setCores( fakeVmInfo.getInstanceTypeCores( ) );
    vmTypeInfo.setDisk( fakeVmInfo.getInstanceTypeDisk( ) );
    vmTypeInfo.setMemory( fakeVmInfo.getInstanceTypeMemory( ) );

    final NetworkConfigType netConfig = new NetworkConfigType( );
    netConfig.setMacAddress( fakeVmInfo.getPrimaryInterface( ).getMac( ) );
    netConfig.setIpAddress( fakeVmInfo.getPrimaryInterface( ).getPrivateAddress( ) );
    netConfig.setIgnoredPublicIp( "0.0.0.0" );
    netConfig.setVlan( -1 );
    netConfig.setNetworkIndex( -1L );
    netConfig.setInterfaceId( Strings.nullToEmpty( fakeVmInfo.getPrimaryInterface( ).getInterfaceId( ) ) );
    netConfig.setAttachmentId( Strings.nullToEmpty( fakeVmInfo.getPrimaryInterface( ).getAttachmentId( ) ) );

    final com.eucalyptus.cluster.common.msgs.VmInfo vmInfo = new com.eucalyptus.cluster.common.msgs.VmInfo( );
    vmInfo.setInstanceId( fakeVmInfo.getId( ) );
    vmInfo.setUuid( fakeVmInfo.getUuid( ) );
    vmInfo.setInstanceType( vmTypeInfo );
    vmInfo.setKeyValue( fakeVmInfo.getSshKeyValue( ) );
    vmInfo.setLaunchTime( new Date( fakeVmInfo.getLaunchtime( ) ) );
    vmInfo.setLaunchIndex( String.valueOf( fakeVmInfo.getLaunchIndex( ) ) );
    vmInfo.setStateName( fakeVmInfo.getState( ) );
    vmInfo.setNetParams( netConfig );
    vmInfo.setOwnerId( fakeVmInfo.getOwnerId( ) );
    vmInfo.setAccountId( fakeVmInfo.getAccountId( ) );
    vmInfo.setReservationId( fakeVmInfo.getReservationId( ) );
    vmInfo.setServiceTag( fakeVmInfo.getServiceTag( ) );
    vmInfo.setNetworkBytes( 0L );
    vmInfo.setBlockBytes( 0L );
    vmInfo.setPlatform( fakeVmInfo.getPlatform( ) );
    vmInfo.setBundleTaskProgress( 0d );
    vmInfo.setMigrationStateName( "none" );
    response.getVms( ).add( vmInfo );

    return response;
  }

  @Override
  public ClusterBundleInstanceResponseType bundleInstance( ClusterBundleInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterBundleRestartInstanceResponseType bundleRestartInstance( ClusterBundleRestartInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterCancelBundleTaskResponseType cancelBundleTask( ClusterCancelBundleTaskType request ) {
    return request.getReply( );
  }

  @Override
  public ClusterDescribeBundleTasksResponseType describeBundleTasks( ClusterDescribeBundleTasksType request ) {
    return request.getReply( );
  }

  @Override
  public ModifyNodeResponseType modifyNode( ModifyNodeType request ) {
    return request.getReply( );
  }

  private enum MetricsToFake {
    CPUUtilization( 0.25, "default" ),
    DiskReadBytes( 1, "ephemeral0", "vda" ),
    DiskReadOps( 0.2, "ephemeral0", "vda" ),
    DiskWriteBytes( 0.5, "ephemeral0", "vda" ),
    DiskWriteOps( 0.1, "ephemeral0", "vda" ),
    NetworkIn( 2, "total" ),
    NetworkInExternal( 1, "default" ),
    NetworkOut( 4, "total" ),
    NetworkOutExternal( 3, "default" ),
    NetworkPacketsIn( 1, "total" ),
    NetworkPacketsOut( 2, "total" ),
    VolumeQueueLength( 0, false, "ephemeral0", "vda" ),
    VolumeTotalReadTime( 0.0001, "ephemeral0", "vda" ),
    VolumeTotalWriteTime( 0.0002, "ephemeral0", "vda" ),
    ;

    private final double multipler;
    private final boolean summation;
    private final List<String> dimensions;

    MetricsToFake( double multiplier, boolean summation, String... dimensions ) {
      this.multipler = multiplier;
      this.summation = summation;
      this.dimensions = ImmutableList.copyOf( dimensions );
    }

    MetricsToFake( double multiplier, String... dimensions ) {
      this( multiplier, true, dimensions );
    }

    public double getMultipler( ) {
      return multipler;
    }

    public boolean isSummation( ) {
      return summation;
    }

    public List<String> getDimensions( ) {
      return dimensions;
    }
  }
}
