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
import com.eucalyptus.entities.Entities
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import javax.persistence.EntityTransaction

import static com.eucalyptus.network.NetworkingFeature.*

/**
 * NetworkingService implementation supporting STATIC, SYSTEM, and MANAGED[-NOVLAN] modes
 */
@CompileStatic
class GenericNetworkingService implements NetworkingService {

  private static final Logger logger = Logger.getLogger( GenericNetworkingService );

  @Override
  PrepareNetworkResourcesResponseType prepare( final PrepareNetworkResourcesType request ) {
    final String zone = request.availabilityZone
    final List<NetworkResource> resources = [ ]

    if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
      request.getResources( ).each { NetworkResource networkResource ->
        switch( networkResource ) {
          case PublicIPResource:
            resources.add( new PublicIPResource(
                value: Addresses.allocateSystemAddress( Partitions.lookupByName( zone ) ).displayName,
                ownerId: networkResource.ownerId ) )
            break
          case SecurityGroupResource:
            Entities.transaction( NetworkGroup ) { EntityTransaction db ->
              final NetworkGroup networkGroup =
                  Entities.uniqueResult( NetworkGroup.withGroupId( null, networkResource.value ) );
              final ExtantNetwork extantNetwork = networkGroup.extantNetwork( );
              request.getResources( ).findAll{ it instanceof PrivateIPResource }.each { PrivateIPResource privateIPResource ->
                resources.add( new PrivateNetworkIndexResource(
                    tag: String.valueOf( extantNetwork.getTag( ) ),
                    value: String.valueOf( extantNetwork.allocateNetworkIndex( ).index ),
                    ownerId: privateIPResource.ownerId
                ) )
              }
              db.commit( );
            }
            break
        }
      }
    }

    //TODO:STEVE: implement restore
//    request.getResources( ).each { NetworkResource networkResource ->
//      switch( networkResource ) {
//        case PrivateNetworkIndexResource:
//          Entities.transaction( NetworkGroup ) { EntityTransaction db ->
//            final NetworkGroup net = Entities.uniqueResult( NetworkGroup.withGroupId( null, networkResource.value ) );
//            net.extantNetwork( );
//            db.commit( );
//          }
//          break
//      }
//    }

    PrepareNetworkResourcesResponseType.cast( request.reply( new PrepareNetworkResourcesResponseType(
        prepareNetworkResourcesResultType: new PrepareNetworkResourcesResultType(
          resources: resources as ArrayList<NetworkResource>
        ),
    ) ) )
  }

  @Override
  ReleaseNetworkResourcesResponseType release( final ReleaseNetworkResourcesType request ) {
    request.getResources( ).each { NetworkResource networkResource ->
      switch( networkResource ) {
        case PublicIPResource:
          try {
            Addresses.getInstance( ).lookup( networkResource.value ).release( )
          } catch ( NoSuchElementException e ) {
            logger.info( "IP address not found for release: ${networkResource.value}" )
          } catch ( e ) {
            logger.error( "Error releasing IP address: ${networkResource.value}", e )
          }
          break
        case PrivateNetworkIndexResource:
          PrivateNetworkIndexResource pniResource = ( PrivateNetworkIndexResource ) networkResource
          try {
            Entities.transaction( NetworkGroup ) { EntityTransaction db ->
              pniResource.with{
                Entities.uniqueResult(
                    PrivateNetworkIndex.named( Integer.valueOf( tag ), Long.valueOf( value ) )
                ).with{
                  release( )
                  teardown( )
                }
              }
              db.commit( );
            }
          } catch ( NoSuchElementException e ) {
            logger.info( "Private network index not found for release: ${pniResource.tag}/${pniResource.value}" )
          } catch ( e ) {
            logger.error( "Error releasing private network index: ${pniResource.tag}/${pniResource.value}", e )
          }
          break
      }
    }

    ReleaseNetworkResourcesResponseType.cast( request.reply( new ReleaseNetworkResourcesResponseType( ) ) )
  }

  @Override
  DescribeNetworkingFeaturesResponseType describeFeatures( final DescribeNetworkingFeaturesType request ) {
    DescribeNetworkingFeaturesResponseType.cast( request.reply(new DescribeNetworkingFeaturesResponseType(
        describeNetworkingFeaturesResult : new DescribeNetworkingFeaturesResult(
            networkingFeatures: NetworkGroups.networkingConfiguration( ).hasNetworking( ) ?
                [ ElasticIPs ] as ArrayList<NetworkingFeature>:
                [ ] as ArrayList<NetworkingFeature>
        )
    ) ) )
  }
}
