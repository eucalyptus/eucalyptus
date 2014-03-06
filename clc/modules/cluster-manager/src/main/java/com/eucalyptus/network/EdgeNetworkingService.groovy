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
import com.eucalyptus.cloud.util.NotEnoughResourcesException
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResult
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesResponseType
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesType
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.records.Logs
import com.google.common.collect.Iterators
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import static com.eucalyptus.compute.common.network.NetworkingFeature.ElasticIPs

/**
 * NetworkingService implementation for EDGE mode
 */
@CompileStatic
class EdgeNetworkingService extends NetworkingServiceSupport {

  private static final Logger logger = Logger.getLogger( EdgeNetworkingService );

  EdgeNetworkingService( ) {
    super( logger )
  }

  @Override
  protected PrepareNetworkResourcesResponseType prepareWithRollback( final PrepareNetworkResourcesType request,
                                                                     final List<NetworkResource> resources ) {
    request.getResources( ).each { NetworkResource networkResource ->
      switch( networkResource ) {
        case PublicIPResource:
          resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) )
          break
        case PrivateIPResource:
          resources.addAll( preparePrivateIp( request, (PrivateIPResource) networkResource ) )
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
            PrivateAddresses.release( networkResource.value, networkResource.ownerId )
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
  UpdateNetworkResourcesResponseType update( final UpdateNetworkResourcesType request ) {
    PrivateAddresses.releasing( request.resources.privateIps, request.cluster )
    UpdateNetworkResourcesResponseType.cast( request.reply( new UpdateNetworkResourcesResponseType( ) ) )
  }

  @Override
  UpdateInstanceResourcesResponseType update(final UpdateInstanceResourcesType request) {
    PublicAddresses.clearDirty( request.resources.publicIps, request.partition )
    UpdateInstanceResourcesResponseType.cast( request.reply( new UpdateInstanceResourcesResponseType( ) ) )
  }

  private Collection<NetworkResource> preparePrivateIp( final PrepareNetworkResourcesType request,
                                                        final PrivateIPResource privateIPResource ) {
    PrivateIPResource resource = null
    final String zone = request.availabilityZone
    final Iterable<Integer> addresses = NetworkConfigurations.getPrivateAddresses( zone )
    if ( privateIPResource.value ) { // handle restore
      if ( Iterators.contains( addresses.iterator( ), privateIPResource.value ) ) {
        try {
          resource = new PrivateIPResource(
              value: PrivateAddresses.allocate( [ PrivateAddresses.asInteger( privateIPResource.value ) ] ),
              ownerId: privateIPResource.ownerId )
        } catch ( NotEnoughResourcesException e ) {
          if ( PrivateAddresses.verify( privateIPResource.value, privateIPResource.ownerId ) ) {
            resource = new PrivateIPResource(
                value: privateIPResource.value,
                ownerId: privateIPResource.ownerId )
          } else {
            logger.error( "Failed to restore private address ${privateIPResource.value}" +
                " for instance ${privateIPResource.ownerId} because of  ${e.message}" );
            Logs.extreme( ).error( e, e );
          }
        }
      } else {
        logger.error( "Failed to restore private address ${privateIPResource.value}" +
            " for instance ${privateIPResource.ownerId} because address is not valid for zone ${zone}" );
      }
    } else {
      resource = new PrivateIPResource(
          value: PrivateAddresses.allocate( addresses ),
          ownerId: privateIPResource.ownerId )
    }

    resource ?
      [ resource ] :
      [ ]
  }
}
