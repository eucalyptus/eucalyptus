/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.util.Exceptions
import com.eucalyptus.util.dns.DomainNames
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig
import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

/**
 *
 */
@CompileStatic
class NetworkInterfaceHelper {
  private static final Logger logger = Logger.getLogger( NetworkInterfaceHelper )

  static String mac( final String identifier ) {
    String.format( "${NetworkConfigurations.macPrefix}:%s:%s:%s:%s",
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
    try {
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
    } catch ( final Exception e ) {
      Exceptions.findAndRethrow( e, ResourceAllocationException.class )
      throw Exceptions.toUndeclared( e )
    }
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
    Addresses.getInstance( ).assign( address, networkInterface )
    if ( instanceOption.present && VmInstance.VmStateSet.RUN.apply( instanceOption.get( ) ) ) {
      Addresses.getInstance( ).start( address, instanceOption.get( ) )
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
    if ( instanceOption.present &&
        networkInterface.isAttached( ) && networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
      VmInstances.updatePublicAddress( instanceOption.get( ), address.displayName )
    }
  }

  static void releasePublic( final VpcNetworkInterface networkInterface ) {
    try {
      List<NetworkResource> resources = Lists.newArrayList( );

      if ( networkInterface.associated ) try {
        Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.association.publicIp )
        try {
          if ( address.started ) {
            Addresses.getInstance( ).stop( address );
          }
        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}' clean up.", e1 )
        }
        try {
          Addresses.getInstance( ).unassign( address, null )
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
      Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.association.publicIp )
      try {
        Addresses.getInstance( ).start( address, instance )
      } catch ( final Exception e ) {
        logger.error( "Error starting address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}, instance ${instance.displayName}'.", e )
      }
    } catch ( NoSuchElementException e ) {
      logger.warn( "Address '${networkInterface.association.publicIp}' not found when stopping '${networkInterface.displayName}, instance ${instance.displayName}'" )
    }
  }


  static void stop( final VpcNetworkInterface networkInterface ) {
    if ( networkInterface.associated ) try {
      Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.association.publicIp )
      try {
        if ( address.started ) {
          Addresses.getInstance( ).stop( address )
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
        Address address = Addresses.getInstance( ).lookupActiveAddress( networkInterface.association.publicIp )
        try {
          if ( address.started ) {
            Addresses.getInstance( ).stop( address )
          }
        } catch ( final Exception e1 ) {
          logger.error( "Error stopping address '${networkInterface.association.publicIp}' for interface '${networkInterface.displayName}' clean up.", e1 )
        }
        try {
          Addresses.getInstance( ).unassign( address, null )
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
