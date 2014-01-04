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
import com.eucalyptus.entities.Entities
import com.eucalyptus.records.Logs
import com.eucalyptus.util.EucalyptusCloudException
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
    final List<NetworkResource> resources = [ ]

    if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
      request.getResources( ).each { NetworkResource networkResource ->
        switch( networkResource ) {
          case PublicIPResource:
            resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) )
            break
          case SecurityGroupResource:
            resources.addAll( prepareSecurityGroup( request, (SecurityGroupResource) networkResource ) )
            break
        }
      }
    }

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

  private Collection<NetworkResource> preparePublicIp( final PrepareNetworkResourcesType request,
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

  private Collection<NetworkResource> prepareSecurityGroup( final PrepareNetworkResourcesType request,
                                                            final SecurityGroupResource securityGroupResource ) {
    final List<NetworkResource> resources = [ ]
    Entities.transaction( NetworkGroup ) { EntityTransaction db ->
      final NetworkGroup networkGroup =
          Entities.uniqueResult( NetworkGroup.withGroupId( null, securityGroupResource.value ) );
      final Set<String> identifiers = []
      // specific values requested, restore case
      request.getResources( ).findAll{ it instanceof PrivateNetworkIndexResource }.each {
        PrivateNetworkIndexResource privateNetworkIndexResource ->
          if ( identifiers.add( privateNetworkIndexResource.ownerId ) ) {
            Integer restoreVlan = Integer.valueOf( privateNetworkIndexResource.tag )
            Long restoreNetworkIndex = Long.valueOf( privateNetworkIndexResource.value )
            if ( networkGroup.hasExtantNetwork( ) && networkGroup.extantNetwork( ).getTag( ) == restoreVlan ) {
              logger.info( "Found matching extant network for ${privateNetworkIndexResource.ownerId}: ${networkGroup.extantNetwork( )}" );
              networkGroup.extantNetwork( ).reclaimNetworkIndex( restoreNetworkIndex );
            } else if ( networkGroup.hasExtantNetwork( ) && !networkGroup.extantNetwork( ).getTag( ) != restoreVlan ) {
              throw new EucalyptusCloudException( "Found conflicting extant network for ${privateNetworkIndexResource.ownerId}: ${networkGroup.extantNetwork( )}" )
            } else {
              logger.info( "Restoring extant network for ${privateNetworkIndexResource.ownerId}: ${restoreVlan}" );
              ExtantNetwork exNet = networkGroup.reclaim( restoreVlan );
              logger.debug( "Restored extant network for ${privateNetworkIndexResource.ownerId}: ${networkGroup.extantNetwork( )}" );
              logger.info( "Restoring private network index for ${privateNetworkIndexResource.ownerId}: ${restoreNetworkIndex}" );
              exNet.reclaimNetworkIndex( restoreNetworkIndex );
            }
            resources.add( new PrivateNetworkIndexResource(
                tag: String.valueOf( restoreVlan ),
                value: String.valueOf( restoreNetworkIndex ),
                ownerId: privateNetworkIndexResource.ownerId
            ) )
          }
      }

      // regular prepare case
      request.getResources( ).findAll{ it instanceof PrivateIPResource }.each { PrivateIPResource privateIPResource ->
        if ( identifiers.add( privateIPResource.ownerId ) ) {
          resources.add( new PrivateNetworkIndexResource(
              tag: String.valueOf( networkGroup.extantNetwork( ).getTag( ) ),
              value: String.valueOf( networkGroup.extantNetwork( ).allocateNetworkIndex( ).index ),
              ownerId: privateIPResource.ownerId
          ) )
        }
      }

      db.commit( );
    }
    resources
  }

}
