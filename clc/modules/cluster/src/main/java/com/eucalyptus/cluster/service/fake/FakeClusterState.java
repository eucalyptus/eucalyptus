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
package com.eucalyptus.cluster.service.fake;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.eucalyptus.cluster.service.vm.VmInterface;
import com.eucalyptus.cluster.service.vm.VmVolumeAttachment;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.FUtils;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 *
 */
class FakeClusterState {
  private static final String host = System.getProperty( "euca.version" ) == null ?
      "127.0.0.1" : // test
      Hosts.localHost( ).getBindAddress( ).getHostAddress( );
  private static final List<ClusterNode> nodes = ImmutableList.of(
      new ClusterNode( host, 100_000, 1_000_000, 1_024_000_000 )
  );

  static List<ClusterNode> nodes( ) {
    return nodes;
  }

  static long currentTime( ) {
    return System.currentTimeMillis( );
  }

  static String localZone( ) {
    return ServiceConfigurations.lookupByHost( FakeCluster.class, Hosts.localHost( ).getDisplayName( ) ).getPartition( );
  }

  static void reload( ) {
    for ( final ClusterNode node : nodes ) {
      node.getVms( ).clear( );
    }
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
      instances:
      for ( final VmInstance instance : VmInstances.list(
          null,
          Restrictions.and(
              Restrictions.not( VmInstance.criterion( VmInstance.VmStateSet.DONE.array( ) ) ),
              VmInstance.zoneCriterion( localZone( ) )
          ),
          Collections.emptyMap( ),
          Predicates.and( VmInstance.VmStateSet.DONE.not( ), VmInstance.VmStateSet.RUN ),
          false ) ) {
        for ( final ClusterNode node : nodes ) {
          if ( node.getServiceTag( ).equals( instance.getServiceTag( ) ) ) {
            final VmInfo fakeVmInfo = new VmInfo(
                instance.getInstanceId( ),
                instance.getInstanceUuid( ),
                instance.getReservationId( ),
                instance.getLaunchIndex( ),
                instance.getVmType( ).getName( ),
                instance.getVmType( ).getCpu( ),
                instance.getVmType( ).getDisk( ),
                instance.getVmType( ).getMemory( ),
                instance.getPlatform( ),
                instance.getKeyPair( ).getPublicKey( ),
                instance.getCreationTimestamp( ).getTime( ),
                "Extant",
                currentTime( ),
                instance.getNetworkInterfaces( ).stream( ).findFirst( )
                    .map( NetworkInterface::getDisplayName ).<String>orElse( null ),
                instance.getNetworkInterfaces( ).stream( ).findFirst( )
                    .map( ni -> ni.getAttachment( ).getAttachmentId( ) ).orElse( null ),
                instance.getMacAddress( ),
                instance.getPrivateAddress( ),
                null,
                instance.getOwnerUserId( ),
                instance.getOwnerAccountNumber( ),
                instance.getServiceTag( ),
                instance.getVpcId( )
            );

            instance.getNetworkInterfaces( ).stream( ).skip( 1 ).collect( Collectors.toMap(
                FUtils.chain( NetworkInterface::getAttachment, NetworkInterfaceAttachment::getDeviceIndex ),
                ni -> new VmInterface(
                    ni.getDisplayName( ),
                    ni.getAttachment( ).getAttachmentId( ),
                    ni.getAttachment( ).getDeviceIndex( ),
                    ni.getMacAddress( ),
                    ni.getPrivateIpAddress( ),
                    Optional.ofNullable( ni.getAssociation( ) )
                        .map( NetworkInterfaceAssociation::getPublicIp ).<String>orElse( null ) ),
                ( v1, v2 ) -> v1,
                fakeVmInfo::getSecondaryInterfaceAttachments
            ) );

            for ( com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment attachment : Iterables.concat(
                instance.getBootRecord( ).getPersistentVolumes( ),
                instance.getTransientVolumeState( ).getAttachments( ) ) ) {
              fakeVmInfo.getVolumeAttachments( ).put( attachment.getVolumeId( ), new VmVolumeAttachment(
                  attachment.getAttachTime( ).getTime( ),
                  attachment.getVolumeId( ),
                  attachment.getDevice( ),
                  attachment.getRemoteDevice( ),
                  "attached"
              ) );
            }
            node.getVms( ).add( fakeVmInfo );
            continue instances;
          }
        }
      }
    }
  }
}
