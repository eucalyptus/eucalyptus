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

import com.eucalyptus.address.Addresses
import com.eucalyptus.component.Partitions
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResult
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.NetworkingService
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesType
import com.eucalyptus.network.config.NetworkConfigurations
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import static com.eucalyptus.compute.common.network.NetworkingFeature.ElasticIPs

/**
 * NetworkingService implementation for EDGE mode
 */
@CompileStatic
class EdgeNetworkingService implements NetworkingService {

  private static final Logger logger = Logger.getLogger( EdgeNetworkingService );

  @Override
  PrepareNetworkResourcesResponseType prepare(final PrepareNetworkResourcesType request) {
    final String zone = request.availabilityZone
    final List<NetworkResource> resources = [ ]

    request.getResources( ).each { NetworkResource networkResource ->
      switch( networkResource ) {
        case PublicIPResource:
          //TODO:STEVE: Restore for public IP in EDGE mode?
          resources.add( new PublicIPResource(
              value: Addresses.allocateSystemAddress( Partitions.lookupByName( zone ) ).displayName,
              ownerId: networkResource.ownerId ) )
          break
        case PrivateIPResource:
          Collection<String> addresses = NetworkConfigurations.getPrivateAddresses( zone )
          resources.add( new PrivateIPResource(
              value: PrivateAddresses.allocate( addresses ).displayName,
              ownerId: networkResource.ownerId ) )
          break
      }
    }

    PrepareNetworkResourcesResponseType.cast( request.reply( new PrepareNetworkResourcesResponseType(
        prepareNetworkResourcesResultType: new PrepareNetworkResourcesResultType(
            resources: resources as ArrayList<NetworkResource>
        ),
    ) ) )
  }

  @Override
  ReleaseNetworkResourcesResponseType release(final ReleaseNetworkResourcesType request) {
    request.getResources( ).each { NetworkResource networkResource ->
      switch( networkResource ) {
        case PublicIPResource:
          try {
            Addresses.getInstance( ).lookup( networkResource.value ).release( )
          } catch ( NoSuchElementException e ) {
            logger.info( "IP address not found for release: ${networkResource.value}" )
          } catch ( e ) {
            logger.error( "Error releasing public IP address: ${networkResource.value}", e )
          }
          break
        case PrivateIPResource:
          try {
            PrivateAddresses.release( networkResource.value )
          } catch ( e ) {
            logger.error( "Error releasing private IP address: ${networkResource.value}", e )
          }
          break
      }
    }

    ReleaseNetworkResourcesResponseType.cast( request.reply( new ReleaseNetworkResourcesResponseType( ) ) )
  }

  @Override
  DescribeNetworkingFeaturesResponseType describeFeatures(final DescribeNetworkingFeaturesType request) {
    DescribeNetworkingFeaturesResponseType.cast( request.reply( new DescribeNetworkingFeaturesResponseType(
        describeNetworkingFeaturesResult : new DescribeNetworkingFeaturesResult(
            networkingFeatures: Lists.newArrayList( ElasticIPs )
        )
    ) ) )
  }

  @Override
  UpdateNetworkResourcesResponseType update(final UpdateNetworkResourcesType request) {
    UpdateNetworkResourcesResponseType.cast( request.reply( new UpdateNetworkResourcesResponseType( ) ) )
  }
}
