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
import com.eucalyptus.cluster.Cluster
import com.eucalyptus.cluster.ClusterConfiguration
import com.eucalyptus.cluster.Clusters
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResult
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType
import com.eucalyptus.compute.common.network.NetworkResource
import com.eucalyptus.compute.common.network.NetworkResourceReportType
import com.eucalyptus.compute.common.network.NetworkingFeature
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType
import com.eucalyptus.compute.common.network.PrivateIPResource
import com.eucalyptus.compute.common.network.PrivateNetworkIndexResource
import com.eucalyptus.compute.common.network.PublicIPResource
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType
import com.eucalyptus.compute.common.network.SecurityGroupResource
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesResponseType
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesResponseType
import com.eucalyptus.compute.common.network.UpdateNetworkResourcesType
import com.eucalyptus.entities.Entities
import com.eucalyptus.util.EucalyptusCloudException
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import javax.persistence.EntityTransaction

import static com.eucalyptus.compute.common.network.NetworkingFeature.*

/**
 * NetworkingService implementation supporting STATIC, SYSTEM, and MANAGED[-NOVLAN] modes
 */
@CompileStatic
class GenericNetworkingService extends NetworkingServiceSupport {

  private static final Logger logger = Logger.getLogger( GenericNetworkingService );

  GenericNetworkingService( ) {
    super( logger )
  }

  @Override
  protected PrepareNetworkResourcesResponseType prepareWithRollback( final PrepareNetworkResourcesType request,
                                                                     final List<NetworkResource> resources ) {
    if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
      request.getResources( ).each { NetworkResource networkResource ->
        switch( networkResource ) {
          case PublicIPResource:
            resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) )
            break
          case SecurityGroupResource:
            if ( !resources.find{ NetworkResource resource -> resource instanceof PrivateNetworkIndex } ) {
              resources.addAll( prepareSecurityGroup( request, (SecurityGroupResource) networkResource ) )
            }
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

  @Override
  UpdateNetworkResourcesResponseType update(final UpdateNetworkResourcesType request) {
    try {
      Cluster cluster = Clusters.instance.lookup( request.cluster )
      updateClusterConfiguration( cluster, request.resources )
      NetworkGroups.updateNetworkRangeConfiguration( );
      NetworkGroups.updateExtantNetworks( cluster.configuration, request.resources.activeNetworks );
    } catch ( NoSuchElementException e ) {
      logger.debug( "Not updating network resource availability, cluster not found ${request.cluster}.", e )
    } catch ( e ) {
      logger.error( "Error updating network resource availability.", e )
    }
    UpdateNetworkResourcesResponseType.cast( request.reply( new UpdateNetworkResourcesResponseType( ) ) )
  }

  @Override
  UpdateInstanceResourcesResponseType update(final UpdateInstanceResourcesType request) {
    UpdateInstanceResourcesResponseType.cast( request.reply( new UpdateInstanceResourcesResponseType( ) ) )
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
            } else if ( networkGroup.hasExtantNetwork( ) && networkGroup.extantNetwork( ).getTag( ) != restoreVlan ) {
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

  private void updateClusterConfiguration( final Cluster cluster,
                                           final NetworkResourceReportType reply ) {
    Entities.transaction( ClusterConfiguration ) { EntityTransaction db ->
      final ClusterConfiguration config = Entities.uniqueResult( cluster.getConfiguration( ) );
      config.setNetworkMode( reply.getMode( ) );
      config.setUseNetworkTags( reply.getUseVlans( ) == 1 );
      config.setMinNetworkTag( reply.getVlanMin( ) );
      config.setMaxNetworkTag( reply.getVlanMax( ) );
      config.setMinNetworkIndex( ( long ) reply.getAddrIndexMin( ) );
      config.setMaxNetworkIndex( ( long ) reply.getAddrIndexMax( ) );
      config.setAddressesPerNetwork( reply.getAddrsPerNet( ) );
      config.setVnetNetmask( reply.getVnetNetmask( ) );
      config.setVnetSubnet( reply.getVnetSubnet( ) );
      db.commit( );
    }
  }
}
