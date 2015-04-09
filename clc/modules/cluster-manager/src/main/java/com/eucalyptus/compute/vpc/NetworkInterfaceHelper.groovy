/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.vpc

import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.Networking
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface as VpcNetworkInterface
import com.eucalyptus.util.dns.DomainNames
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig
import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import static com.eucalyptus.vm.VmInstances.MAC_PREFIX

/**
 *
 */
@CompileStatic
class NetworkInterfaceHelper {
  private static final Logger logger = Logger.getLogger( NetworkInterfaceHelper )

  static String mac( final String identifier ) {
    String.format( "${MAC_PREFIX}:%s:%s:%s:%s",
        identifier.substring( 4, 6 ),
        identifier.substring( 6, 8 ),
        identifier.substring( 8, 10 ),
        identifier.substring( 10, 12 ) ).toLowerCase( );
  }

  static String allocate( final String vpcId,
                          final String subnetId,
                          final String networkInterfaceId,
                          final String mac,
                          final String privateIp ) throws ResourceAllocationException {
    PrepareNetworkResourcesResultType result = Networking.instance.prepare( new PrepareNetworkResourcesType(
        vpc: vpcId,
        subnet: subnetId,
        resources: [
            new VpcNetworkInterfaceResource(
                ownerId: networkInterfaceId,
                value: networkInterfaceId,
                mac: mac,
                privateIp: privateIp
            )
        ] as ArrayList<NetworkResource>
    ) )

    VpcNetworkInterfaceResource resource = result?.resources?.getAt( 0 ) as VpcNetworkInterfaceResource
    String allocatedIp = resource?.privateIp
    if ( !allocatedIp ) {
      throw new ResourceAllocationException( "Address (${privateIp}) not available" )
    }
    allocatedIp
  }

  /**
   * Caller must have open transaction on ENI
   */
  static void associate( final Address address,
                         final VpcNetworkInterface networkInterface ) {
    associate( address, networkInterface, Optional.fromNullable( networkInterface.instance ) )
  }

  /**
   * Caller must have open transaction on ENI / instance
   */
  static void associate( final Address address,
                         final VpcNetworkInterface networkInterface,
                         final Optional<VmInstance> instanceOption ) {
    address.assign( networkInterface )
    if ( instanceOption.present && VmInstance.VmStateSet.RUN.apply( instanceOption.get( ) ) ) {
      address.start( instanceOption.get( ) )
    }
    networkInterface.associate( NetworkInterfaceAssociation.create(
        address.associationId,
        address.allocationId,
        address.ownerAccountNumber,
        address.displayName,
        networkInterface.vpc.dnsHostnames ?
            VmInstances.dnsName( address.displayName, DomainNames.externalSubdomain( ) ) :
            null as String
    ) )
    if ( instanceOption.present ) {
      VmInstances.updatePublicAddress( instanceOption.get( ), address.displayName )
    }
  }

  static void releasePublic( final VpcNetworkInterface networkInterface ) {
    try {
      List<NetworkResource> resources = Lists.newArrayList( );

      if ( networkInterface.associated ) try {
        Address address = Addresses.getInstance( ).lookup( networkInterface.association.publicIp )
        try {
          if ( address.started ) {
            address.stop( );
          }
        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}' clean up.", e1 )
        }
        try {
          address.unassign( networkInterface )
        } catch ( final Exception e2 ) {
          logger.error( "Error unassiging address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}'.", e2 )
        }
        if ( address.systemOwned ) {
          resources << new PublicIPResource(
              value: networkInterface.association.publicIp
          )
        }
      } catch ( NoSuchElementException e ) {
        logger.warn( "Address '${networkInterface.association.publicIp}' not found when releasing '${networkInterface.displayName}'" )
      }

      if ( resources ) {
        Networking.instance.release( new ReleaseNetworkResourcesType(
            vpc: networkInterface.vpc.displayName,
            resources: resources as ArrayList<NetworkResource> )
        )
      }
    } catch ( final Exception ex ) {
      logger.error( "Error releasing public address '${networkInterface.privateIpAddress}' for interface '${networkInterface.displayName}'.", ex )
    }
  }

  static void start( final VpcNetworkInterface networkInterface, final VmInstance instance ) {
    if ( networkInterface.associated ) try {
      Address address = Addresses.getInstance( ).lookup( networkInterface.association.publicIp )
      try {
        address.start( instance )
      } catch ( final Exception e ) {
        logger.error( "Error starting address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}, instance ${instance.displayName}'.", e )
      }
    } catch ( NoSuchElementException e ) {
      logger.warn( "Address '${networkInterface.association.publicIp}' not found when stopping '${networkInterface.displayName}, instance ${instance.displayName}'" )
    }
  }


  static void stop( final VpcNetworkInterface networkInterface ) {
    if ( networkInterface.associated ) try {
      Address address = Addresses.getInstance( ).lookup( networkInterface.association.publicIp )
      try {
        if ( address.started ) {
          address.stop( );
        }
      } catch ( final Exception e ) {
        logger.error( "Error stopping address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}'.", e )
      }
    } catch ( NoSuchElementException e ) {
      logger.warn( "Address '${networkInterface.association.publicIp}' not found when stopping '${networkInterface.displayName}'" )
    }
  }

  static void release( final VpcNetworkInterface networkInterface ) {
    try {
      List<NetworkResource> resources = Lists.newArrayList( );

      if ( !Strings.isNullOrEmpty( networkInterface.privateIpAddress ) &&
          !VmNetworkConfig.DEFAULT_IP.equals( networkInterface.privateIpAddress ) ) {
        resources << new VpcNetworkInterfaceResource(
            value: networkInterface.displayName,
            mac: networkInterface.macAddress,
            privateIp: networkInterface.privateIpAddress
        )
      }

      if ( networkInterface.associated ) try {
        Address address = Addresses.getInstance( ).lookup( networkInterface.association.publicIp )
        try {
          if ( address.started ) {
            address.stop( )
          }
        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}' clean up.", e1 )
        }
        try {
          address.unassign( networkInterface )
        } catch ( final Exception e2 ) {
          logger.error( "Error unassiging address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}' clean up.", e2 )
        }
        if ( address.systemOwned ) {
          resources << new PublicIPResource(
              value: networkInterface.association.publicIp
          )
        }
      } catch ( NoSuchElementException e ) {
        logger.warn( "Address '${networkInterface.association.publicIp}' not found when releasing '${networkInterface.displayName}'" )
      }

      if ( resources ) {
        Networking.instance.release( new ReleaseNetworkResourcesType(
            vpc: networkInterface.vpc.displayName,
            resources: resources as ArrayList<NetworkResource> )
        )
      }
    } catch ( final Exception ex ) {
      logger.error( "Error releasing private address '${networkInterface.privateIpAddress}' for interface '${networkInterface.displayName}' clean up.", ex )
    }
  }
}
