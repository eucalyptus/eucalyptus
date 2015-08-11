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
package com.eucalyptus.vm.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import com.eucalyptus.address.AddressRegistry;
import com.eucalyptus.cluster.ClusterConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Subnets;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;

/**
 * Resolve the network (cidr) for an instance (i.e. a VPC or EC2-Classic)
 */
public class InstanceNetworkResolver {

  public static Optional<Cidr> tryResolve( final InetAddress address ) {
    try ( TransactionResource db = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
      final VmInstance instance = VmInstances.lookupByPublicIp( address.getHostAddress( ) );
      if ( instance.getVpcId( ) != null ) {
        return FUtils.flatten( Optional.fromNullable( instance.getBootRecord( ).getVpc( ) )
            .transform( Functions.compose( Cidr.parse( ), Vpcs.FilterStringFunctions.CIDR ) ) );
      } else {
        final InetAddress privateAddress = AddressRegistry.getInstance( ).contains( address.getHostAddress( ) ) ?
            InetAddresses.forString( VmInstances.lookupByPublicIp( address.getHostAddress( ) ).getPrivateAddress( ) ) :
            address;
        for ( final ServiceConfiguration clusterService : ServiceConfigurations.list( ClusterController.class ) ) {
          final ClusterConfiguration cluster = ( ClusterConfiguration ) clusterService;
          try {
            final Cidr cidr = Subnets.cidr( cluster.getVnetSubnet( ), cluster.getVnetNetmask( ) );
            if ( cidr.contains( privateAddress.getHostAddress( ) ) ) {
              return Optional.of( cidr );
            }
          } catch ( final UnknownHostException ex ) {
            // try next configuration
          }
        }
      }
    } catch ( NoSuchElementException e ) {
      // instance not found, no network to resolve
    }
    return Optional.absent( );
  }

}
