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
package com.eucalyptus.network

import com.eucalyptus.address.Address
import com.eucalyptus.address.Addresses
import com.eucalyptus.component.Partitions
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.NetworkingService
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.records.Logs
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Logger

/**
 *
 */
@CompileStatic
@PackageScope
abstract class NetworkingServiceSupport implements NetworkingService {

  private final Logger logger;

  NetworkingServiceSupport( final Logger logger ) {
    this.logger = logger
  }

  @Override
  PrepareNetworkResourcesResponseType prepare( final PrepareNetworkResourcesType request ) {
    final ArrayList<NetworkResource> resources = Lists.newArrayList( )
    try {
      prepareWithRollback( request, resources )
    } catch ( Exception e ) {
      resources.each{ NetworkResource resource -> resource.ownerId = null }
      release( new ReleaseNetworkResourcesType( resources: resources ) );
      throw e
    }
  }

  protected abstract PrepareNetworkResourcesResponseType prepareWithRollback( final PrepareNetworkResourcesType request,
                                                                              final List<NetworkResource> resources );

  protected Collection<NetworkResource> preparePublicIp( final PrepareNetworkResourcesType request,
                                                         final PublicIPResource publicIPResource ) {
    final String zone = request.availabilityZone

    String address = null
    if ( publicIPResource.value ) { // handle restore
      String restoreQualifier = ''
      try {
        try {
          final Address addr = Addresses.getInstance( ).lookup( publicIPResource.value )
          if ( addr.reallyAssigned && addr.instanceId == publicIPResource.ownerId ) {
            address = publicIPResource.value
          }
        } catch ( NoSuchElementException ignored ) { // Address disabled
          restoreQualifier = "(from disabled) "
          final Address addr = Addresses.getInstance( ).lookupDisabled( publicIPResource.value );
          addr.pendingAssignment( );
          address = publicIPResource.value
        }
      } catch ( final Exception e ) {
        logger.error( "Failed to restore address state ${restoreQualifier}${publicIPResource.value}" +
            " for instance ${publicIPResource.ownerId} because of: ${e.message}" );
        Logs.extreme( ).error( e, e );
      }
    } else {
      address = Addresses.allocateSystemAddress( Partitions.lookupByName( zone ) ).displayName
    }

    address ?
        [ new PublicIPResource(  value: address, ownerId: publicIPResource.ownerId ) ] :
        [ ]
  }



}
