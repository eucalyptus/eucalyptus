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
package com.eucalyptus.cluster.service.fake;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.MetricCounterType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType;
import com.eucalyptus.cluster.common.msgs.MetricDimensionsValuesType;
import com.eucalyptus.cluster.common.msgs.MetricsResourceType;
import com.eucalyptus.cluster.common.msgs.NcAssignAddressResponseType;
import com.eucalyptus.cluster.common.msgs.NcAssignAddressType;
import com.eucalyptus.cluster.common.msgs.NcAttachNetworkInterfaceResponseType;
import com.eucalyptus.cluster.common.msgs.NcAttachNetworkInterfaceType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.NcAttachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoResponseType;
import com.eucalyptus.cluster.common.msgs.NcBroadcastNetworkInfoType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.NcBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.NcCancelBundleTaskType;
import com.eucalyptus.cluster.common.msgs.NcCreateImageResponseType;
import com.eucalyptus.cluster.common.msgs.NcCreateImageType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeInstancesType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeResourceType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsResponseType;
import com.eucalyptus.cluster.common.msgs.NcDescribeSensorsType;
import com.eucalyptus.cluster.common.msgs.NcDetachNetworkInterfaceResponseType;
import com.eucalyptus.cluster.common.msgs.NcDetachNetworkInterfaceType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeResponseType;
import com.eucalyptus.cluster.common.msgs.NcDetachVolumeType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputResponseType;
import com.eucalyptus.cluster.common.msgs.NcGetConsoleOutputType;
import com.eucalyptus.cluster.common.msgs.NcMigrateInstancesResponseType;
import com.eucalyptus.cluster.common.msgs.NcMigrateInstancesType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeResponseType;
import com.eucalyptus.cluster.common.msgs.NcModifyNodeType;
import com.eucalyptus.cluster.common.msgs.NcPowerDownResponseType;
import com.eucalyptus.cluster.common.msgs.NcPowerDownType;
import com.eucalyptus.cluster.common.msgs.NcRebootInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcRebootInstanceType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcRunInstanceType;
import com.eucalyptus.cluster.common.msgs.NcStartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcStartInstanceType;
import com.eucalyptus.cluster.common.msgs.NcStartNetworkResponseType;
import com.eucalyptus.cluster.common.msgs.NcStartNetworkType;
import com.eucalyptus.cluster.common.msgs.NcStopInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcStopInstanceType;
import com.eucalyptus.cluster.common.msgs.NcTerminateInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.NcTerminateInstanceType;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.eucalyptus.cluster.common.msgs.SensorsResourceType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.common.msgs.VolumeType;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.eucalyptus.cluster.service.vm.ClusterVmBootRecord;
import com.eucalyptus.cluster.service.vm.ClusterVmInterface;
import com.eucalyptus.cluster.service.vm.ClusterVmType;
import com.eucalyptus.cluster.service.vm.ClusterVmVolume;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
public class FakeNodeService implements NodeService {

  private final ClusterNode node;
  private final FakeNodeState state;
  private final Clock clock;

  @SuppressWarnings( "WeakerAccess" )
  public FakeNodeService( final ClusterNode node, final Clock clock, final boolean allowReload ) {
    this.node = node;
    this.state = new FakeNodeState( );
    this.clock = clock;
    if ( allowReload && node.getVms( ).isEmpty( ) ) {
      reload( );
    }
    final long now = clock.millis( );
    node.getVms( ).forEach( vm -> state.extant( now, vm ) );
  }

  @Override
  public NcAssignAddressResponseType assignAddress( final NcAssignAddressType request ) {
    state.assignAddress( request.getInstanceId( ), request.getPublicIp( ) );
    return request.getReply( );
  }

  @Override
  public NcAttachNetworkInterfaceResponseType attachNetworkInterface( final NcAttachNetworkInterfaceType request ) {
    return request.getReply( );
  }

  @Override
  public NcAttachVolumeResponseType attachVolume( final NcAttachVolumeType request ) {
    final NcAttachVolumeResponseType reply = request.getReply( );
    state.vm( request.getInstanceId( ) ).forEach( vm -> {
      final ClusterVmVolume fakeVolumeAttachment = ClusterVmVolume.of(
          clock.millis( ),
          request.getVolumeId( ),
          request.getLocalDev( ),
          request.getRemoteDev( ),
          "attached"
      );
      vm.getVolumeAttachments( ).put( request.getVolumeId( ), fakeVolumeAttachment );
    } );
    return reply;
  }

  @Override
  public NcBroadcastNetworkInfoResponseType broadcastNetworkInfo( final NcBroadcastNetworkInfoType request ) {
    return request.getReply( );
  }

  @Override
  public NcBundleInstanceResponseType bundleInstance( final NcBundleInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public NcBundleRestartInstanceResponseType bundleRestartInstance( final NcBundleRestartInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public NcCancelBundleTaskResponseType cancelBundleTask( final NcCancelBundleTaskType request ) {
    return request.getReply( );
  }

  @Override
  public NcCreateImageResponseType createImage( final NcCreateImageType request ) {
    return request.getReply( );
  }

  @Override
  public NcDescribeInstancesResponseType describeInstances( final NcDescribeInstancesType request ) {
    final NcDescribeInstancesResponseType reply = request.getReply( );
    final ArrayList<InstanceType> instances = Lists.newArrayList( );
    state.cleanup( clock.millis( ) );
    node.getVms( ).forEach( vm -> {
      final Option<FakeNodeVmInfo> nodeVm = state.vm( vm.getId( ) );
      if ( nodeVm.isEmpty( ) ) return;

      final VirtualMachineType vmType = new VirtualMachineType( );
      vmType.setName( vm.getVmType( ).getName( ) );
      vmType.setCores( vm.getVmType( ).getCores( ) );
      vmType.setDisk( vm.getVmType( ).getDisk( ) );
      vmType.setMemory( vm.getVmType( ).getMemory( ) );
      nodeVm.get( ).getVolumeAttachments( ).values( ).forEach( attachment -> {
        final VirtualBootRecordType virtualBootRecord = new VirtualBootRecordType( );
        virtualBootRecord.setType( "machine" );
        virtualBootRecord.setFormat( "none" );
        virtualBootRecord.setGuestDeviceName( attachment.getDevice( ) );
        virtualBootRecord.setSize( 41126400L );
        virtualBootRecord.setId( attachment.getVolumeId( ) );
        virtualBootRecord.setResourceLocation( attachment.getRemoteDevice( ) );
        vmType.getVirtualBootRecord( ).add( virtualBootRecord );
      } );

      final NetConfigType netConfig = new NetConfigType( );
      netConfig.setInterfaceId( vm.getId( ) );
      netConfig.setDevice( 0 );
      netConfig.setPrivateIp( vm.getPrimaryInterface( ).getPrivateAddress( ) );
      netConfig.setPublicIp(
          nodeVm.map( FakeNodeVmInfo::getPublicIp ).getOrElse( vm.getPrimaryInterface( ).getPublicAddress( ) )
      );
      netConfig.setVlan( -1 );
      netConfig.setNetworkIndex( -1 );

      final InstanceType instance = new InstanceType( );
      instance.setReservationId( vm.getReservationId( ) );
      instance.setInstanceId( vm.getId( ) );
      instance.setUuid( vm.getUuid( ) );
      instance.setUserId( vm.getOwnerId( ) );
      instance.setOwnerId( vm.getOwnerId() );
      instance.setAccountId( vm.getAccountId() );
      instance.setKeyName( vm.getSshKeyValue( ) );
      instance.setInstanceType( vmType );
      instance.setNetParams( netConfig );
      instance.setStateName( nodeVm.map( FakeNodeVmInfo::getStateName ).getOrElse( vm.getState( ) ) );
      instance.setBundleTaskStateName( "none" );
      instance.setBundleTaskProgress( 0D );
//      instance.setExpiryTime(  );
      instance.setLaunchTime( new Date( vm.getLaunchtime( ) ) );
      instance.setLaunchIndex( String.valueOf( vm.getLaunchIndex( ) ) );
      instance.setBlkbytes( 0 );
      instance.setNetbytes( 0 );
      instance.setGuestStateName( "poweredOn" );
      instance.setMigrationStateName( "none" );
//      instance.setMigrationSource(  );
//      instance.setMigrationDestination(  );
//      instance.setUserData(  );
      instance.setPlatform( vm.getPlatform( ) );
//      instance.setGroupNames(  );
//      instance.setGroupIds(  );
      instance.setHasFloopy( 0 );
      for ( ClusterVmVolume attachment : nodeVm.get( ).getVolumeAttachments( ).values( ) ) {
        final VolumeType volume = new VolumeType( );
        volume.setVolumeId( attachment.getVolumeId( ) );
        volume.setLocalDev( attachment.getDevice( ) );
        volume.setRemoteDev( attachment.getRemoteDevice( ) );
        volume.setState( attachment.getState( ) );
        instance.getVolumes( ).add( volume );
      }

      instances.add( instance );
    } );
    reply.setInstances( instances );
    return reply;
  }

  @Override
  public NcDescribeResourceResponseType describeResource( final NcDescribeResourceType request ) {
    final NcDescribeResourceResponseType reply = request.getReply( );
    reply.setIqn( "iqn.1994-05.com.redhat:c7ec6fad289" );
    reply.setHypervisor( "KVM" );
    reply.setNumberOfCoresMax( 100_000 );
    reply.setNumberOfCoresAvailable( 100_000 - ( node.getVms( ).size( ) ) );
    reply.setDiskSizeMax( 500_000 );
    reply.setDiskSizeAvailable( 500_000 - ( node.getVms( ).size( ) ) * 5 );
    reply.setMemorySizeMax( 25_600_000 );
    reply.setMemorySizeAvailable( 25_600_000 - ( node.getVms( ).size( ) ) );
    reply.setNodeStatus( "enabled" );
    return reply;
  }

  @Override
  public NcDescribeSensorsResponseType describeSensors( final NcDescribeSensorsType request ) {
    final NcDescribeSensorsResponseType reply = request.getReply( );
    final Iterable<String> instanceIds = request.getInstanceIds( );
    final int history = request.getHistorySize( );
    final long interval = request.getCollectionIntervalTimeMs( ).longValue( );

    final long currentTime = clock.millis( );
    final long mostRecentTimestamp = currentTime - ( currentTime % interval );

    for ( final String instanceId : instanceIds ) {
      ClusterVm vmInfo = null;
      for ( final ClusterVm fakeVmInfo : node.getVms( ) ) {
        if ( fakeVmInfo.getId( ).equals( instanceId ) ) {
          vmInfo = fakeVmInfo;
          break;
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
      reply.getSensorsResources( ).add( sensorsResource );
    }
    return reply;
  }

  @Override
  public NcDetachNetworkInterfaceResponseType detachNetworkInterface( final NcDetachNetworkInterfaceType request ) {
    return request.getReply( );
  }

  @Override
  public NcDetachVolumeResponseType detachVolume( final NcDetachVolumeType request ) {
    final NcDetachVolumeResponseType reply = request.getReply( );
    state.vm( request.getInstanceId( ) ).forEach( vm -> {
      final ClusterVmVolume existingFakeVolumeAttachment = vm.getVolumeAttachments( ).get( request.getVolumeId( ) );
      if ( existingFakeVolumeAttachment != null ) {
        final ClusterVmVolume newFakeVolumeAttachment = ClusterVmVolume.of(
            existingFakeVolumeAttachment.getAttachmentTimestamp( ),
            existingFakeVolumeAttachment.getVolumeId( ),
            existingFakeVolumeAttachment.getDevice( ),
            existingFakeVolumeAttachment.getRemoteDevice( ),
            "detached"
        );
        vm.getVolumeAttachments( ).replace( request.getVolumeId( ), existingFakeVolumeAttachment, newFakeVolumeAttachment );
      }
    } );
    return reply;
  }

  @Override
  public NcGetConsoleOutputResponseType getConsoleOutput( final NcGetConsoleOutputType request ) {
    final NcGetConsoleOutputResponseType reply = request.getReply( );
    final Option<ClusterVm> vm = vm( request.getInstanceId( ) );
    final Option<FakeNodeVmInfo> fakeVm = state.vm( request.getInstanceId( ) );
    reply.setConsoleOutput( BaseEncoding.base64( ).encode(
        (
            "\nConsole output for fake instance " + request.getInstanceId( ) + "\n" +
            vm.map( Object::toString ).getOrElse( "" ) + "\n" +
            fakeVm.map( Object::toString ).getOrElse( "" ) + "\n"
        ).getBytes( StandardCharsets.UTF_8 )
    ) );
    return reply;
  }

  @Override
  public NcMigrateInstancesResponseType migrateInstances( final NcMigrateInstancesType request ) {
    return request.getReply( );
  }

  @Override
  public NcModifyNodeResponseType modifyNode( final NcModifyNodeType request ) {
    return request.getReply( );
  }

  @Override
  public NcPowerDownResponseType powerDown( final NcPowerDownType request ) {
    return request.getReply( );
  }

  @Override
  public NcRebootInstanceResponseType rebootInstance( final NcRebootInstanceType request ) {
    final NcRebootInstanceResponseType reply = request.getReply( );
    reply.setStatus( true );
    return reply;
  }

  @Override
  public NcRunInstanceResponseType runInstance( final NcRunInstanceType request ) {
    final NcRunInstanceResponseType reply = request.getReply( );
    vm( request.getInstanceId( ) ).forEach( vm -> state.extant( clock.millis( ), vm ) );
    final InstanceType instanceType = new InstanceType( );
    instanceType.setReservationId( request.getReservationId( ) );
    instanceType.setInstanceId( request.getInstanceId( ) );
    instanceType.setUuid( request.getUuid( ) );
    instanceType.setImageId( request.getImageId( ) );
    reply.setInstance( instanceType );
    return reply;
  }

  @Override
  public NcStartInstanceResponseType startInstance( final NcStartInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public NcStartNetworkResponseType startNetwork( final NcStartNetworkType request ) {
    return request.getReply( );
  }

  @Override
  public NcStopInstanceResponseType stopInstance( final NcStopInstanceType request ) {
    return request.getReply( );
  }

  @Override
  public NcTerminateInstanceResponseType terminateInstance( final NcTerminateInstanceType request ) {
    final NcTerminateInstanceResponseType reply = request.getReply( );
    state.terminate( clock.millis( ), request.getInstanceId( ) );
    reply.setInstanceId( request.getInstanceId( ) );
    return reply;
  }

  @Override
  public CheckedListenableFuture<NcAssignAddressResponseType> assignAddressAsync( final NcAssignAddressType request ) {
    return Futures.predestinedFuture( assignAddress( request ) );
  }

  @Override
  public CheckedListenableFuture<NcBroadcastNetworkInfoResponseType> broadcastNetworkInfoAsync( final NcBroadcastNetworkInfoType request ) {
    return Futures.predestinedFuture( broadcastNetworkInfo( request ) );
  }

  @Override
  public CheckedListenableFuture<NcDescribeInstancesResponseType> describeInstancesAsync( final NcDescribeInstancesType request ) {
    return Futures.predestinedFuture( describeInstances( request ) );
  }

  @Override
  public CheckedListenableFuture<NcDescribeResourceResponseType> describeResourceAsync( final NcDescribeResourceType request ) {
    return Futures.predestinedFuture( describeResource( request ) );
  }

  @Override
  public CheckedListenableFuture<NcDescribeSensorsResponseType> describeSensorsAsync( final NcDescribeSensorsType request ) {
    return Futures.predestinedFuture( describeSensors( request ) );
  }

  @Override
  public CheckedListenableFuture<NcMigrateInstancesResponseType> migrateInstancesAsync( final NcMigrateInstancesType request ) {
    return Futures.predestinedFuture( migrateInstances( request ) );
  }

  private Option<ClusterVm> vm( final String id ) {
    return Stream.ofAll( node.getVms( ) ).find( vm -> vm.getId( ).equals( id ) );
  }

  private void reload( ) {
    try ( final TransactionResource ignored = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
      for ( final VmInstance instance : VmInstances.list(
          null,
          Restrictions.and(
              VmInstance.criterion( VmInstance.VmStateSet.RUN.array( ) ),
              VmInstance.serviceTagCriterion( node.getServiceTag( ) )
          ),
          Collections.emptyMap( ),
          VmInstance.VmStateSet.RUN,
          false ) ) {

          final ClusterVm fakeVmInfo = new ClusterVm(
              instance.getInstanceId( ),
              instance.getInstanceUuid( ),
              instance.getReservationId( ),
              instance.getLaunchIndex( ),
              instance.getPlatform( ),
              instance.getKeyPair( ).getPublicKey( ),
              instance.getCreationTimestamp( ).getTime( ),
              "Extant",
              instance.getLastUpdateTimestamp( ).getTime( ),
              instance.getNetworkInterfaces( ).stream( ).findFirst( )
                  .map( NetworkInterface::getDisplayName ).<String>orElse( null ),
              instance.getNetworkInterfaces( ).stream( ).findFirst( )
                  .map( ni -> ni.getAttachment( ).getAttachmentId( ) ).orElse( null ),
              instance.getMacAddress( ),
              instance.getPrivateAddress( ),
              null,
              instance.getOwnerUserId( ),
              instance.getOwnerAccountNumber( ),
              ClusterVmBootRecord.none( ),
              ClusterVmType.from( instance.getVmType( ) )
          );

        fakeVmInfo.getSecondaryInterfaceAttachments().putAll( instance.getNetworkInterfaces( ).stream( ).skip( 1 ).collect( Collectors.toMap(
              FUtils.chain( NetworkInterface::getAttachment, NetworkInterfaceAttachment::getDeviceIndex ),
              ni -> ClusterVmInterface.of(
                  ni.getDisplayName( ),
                  ni.getAttachment( ).getAttachmentId( ),
                  ni.getAttachment( ).getDeviceIndex( ),
                  ni.getMacAddress( ),
                  ni.getPrivateIpAddress( ),
                  Optional.ofNullable( ni.getAssociation( ) )
                      .map( NetworkInterfaceAssociation::getPublicIp ).<String>orElse( null ) )
          ) ) );

          for ( com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment attachment : Iterables.concat(
              instance.getBootRecord( ).getPersistentVolumes( ),
              instance.getTransientVolumeState( ).getAttachments( ) ) ) {
            fakeVmInfo.getVolumeAttachments( ).put( attachment.getVolumeId( ), ClusterVmVolume.of(
                attachment.getAttachTime( ).getTime( ),
                attachment.getVolumeId( ),
                attachment.getDevice( ),
                attachment.getRemoteDevice( ),
                "attached"
            ) );
          }
          node.vm( fakeVmInfo );
      }
    }
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
