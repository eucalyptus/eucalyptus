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
import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.util.EucalyptusCloudException
import com.google.common.base.Optional
import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import javax.persistence.EntityTransaction

import static com.eucalyptus.compute.common.network.NetworkingFeature.*

/**
 * NetworkingService implementation supporting MANAGED[-NOVLAN] modes
 */
@CompileStatic
class ManagedNetworkingService extends NetworkingServiceSupport {
  private static final Logger logger = Logger.getLogger( ManagedNetworkingService );

  /**
   * class Constructor
   */
  ManagedNetworkingService( ) {
    super( logger )
  }

  /**
   * Allocates network resources for both MANAGED modes. In this case, we will prepare
   * the public IP resource requests as well as the security group resource requests. When
   * a security group requests is present, the resulting resource list will include PrivateIpResources
   *
   * @param request the request information containing the list of resource to prepare
   * @param resources the list of NetworkResource to fill
   *
   * @return the resulting PrepareNetworkResourcesResponseType instance
   */
  @Override
  protected PrepareNetworkResourcesResponseType prepareWithRollback( final PrepareNetworkResourcesType request,
                                                                     final List<NetworkResource> resources ) {
    // Only if we have a valid networking configuration
    if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
      request.getResources( ).each { NetworkResource networkResource ->
        switch( networkResource ) {
          case PublicIPResource:
            if ( networkResource.ownerId.startsWith( 'i-' ) ) {
              resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) )
            }
            break
          case SecurityGroupResource:
            // Only serve the request if we do not have any PrivateIndexResources in the list
            if ( !resources.find{ NetworkResource resource -> resource instanceof PrivateNetworkIndex } ) {
              resources.addAll( prepareSecurityGroup( request, (SecurityGroupResource) networkResource ) )
            }
            break
        }
      }
    }

    // Setup the response
    PrepareNetworkResourcesResponseType.cast( request.reply( new PrepareNetworkResourcesResponseType(
          prepareNetworkResourcesResultType: new PrepareNetworkResourcesResultType(
            resources: resources as ArrayList<NetworkResource>
          ),
    ) ) )
  }

  /**
   * Release the previously allocated resources.
   *
   * @param request the request containing the list of resources to release
   *
   * @return the resulting ReleaseNetworkResourcesResponseType instance
   */
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
        case PrivateIPResource:
          try {
            PrivateAddresses.release( request.vpc, networkResource.value, networkResource.ownerId )
          } catch ( e ) {
            logger.error( "Error releasing private IP address: ${networkResource.value}", e )
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
    DescribeNetworkingFeaturesResponseType.cast( request.reply( new DescribeNetworkingFeaturesResponseType(
          describeNetworkingFeaturesResult : new DescribeNetworkingFeaturesResult(
            networkingFeatures: NetworkGroups.networkingConfiguration( ).hasNetworking( ) ?
              [ Classic, ElasticIPs ] as ArrayList<NetworkingFeature>:
              [ Classic ] as ArrayList<NetworkingFeature>
          )
    ) ) )
  }

  @Override
  UpdateNetworkResourcesResponseType update( final UpdateNetworkResourcesType request ) {
    try {
      Cluster cluster = Clusters.instance.lookup( request.cluster )
      NetworkGroups.updateNetworkRangeConfiguration( );
      NetworkGroups.updateExtantNetworks( cluster.configuration, request.resources.activeNetworks );
    } catch ( NoSuchElementException e ) {
      logger.debug( "Not updating network resource availability, cluster not found ${request.cluster}.", e )
    } catch ( e ) {
      logger.error( "Error updating network resource availability.", e )
    }
    PrivateAddresses.releasing( request.resources.privateIps, request.cluster )
    UpdateNetworkResourcesResponseType.cast( request.reply( new UpdateNetworkResourcesResponseType( ) ) )
  }

  @Override
  UpdateInstanceResourcesResponseType update( final UpdateInstanceResourcesType request ) {
    PublicAddresses.clearDirty( request.resources.publicIps, request.partition )
    UpdateInstanceResourcesResponseType.cast( request.reply( new UpdateInstanceResourcesResponseType( ) ) )
  }

  /**
   * Retrieves a private IP address from a given network segment index and an IP
   * address index from within the segment.
   *
   * @param segmentId the network segment index to select
   * @param ipIndex the IP address index from within the network segment
   *
   * @return the matching IP address in a String format
   */
  private static int getPrivateIpFromSegment( Integer segmentId, Long ipIndex ) {
    int privateIpInt = -1;

    // Do we have a valid configuration?
    Optional<NetworkConfiguration> configuration = NetworkConfigurations.networkConfiguration
    if ( configuration.present ) {
      // Retrieve our managed subnet configuration
      NetworkConfiguration config = configuration.get()
      ManagedSubnet managedSubnet = config.getManagedSubnet()
      if ( managedSubnet ) {
        // Convert our strings to integers so we can do some  calculation
        int subnetInt = InetAddresses.coerceToInteger( InetAddresses.forString( managedSubnet.getSubnet( ) ) )
        int segmentSizeInt = managedSubnet.segmentSize ?: ManagedSubnet.DEF_SEGMENT_SIZE

        // Compute the private IP and set the return value
        privateIpInt = subnetInt + ( segmentSizeInt * segmentId.intValue( ) ) + ipIndex.intValue( )
      }
    }

    privateIpInt
  }

  private Collection<NetworkResource> prepareSecurityGroup( final PrepareNetworkResourcesType request,
                                                            final SecurityGroupResource securityGroupResource ) {
    final List<NetworkResource> resources = [ ]
    final List<NetworkResource> networkIndexResources = [ ]
    Entities.transaction( NetworkGroup ) { EntityTransaction db ->
      final NetworkGroup networkGroup = Entities.uniqueResult( NetworkGroup.withGroupId( null, securityGroupResource.value ) );
      final Set<String> identifiers = [ ]
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

            networkIndexResources.add( new PrivateNetworkIndexResource(
                tag: String.valueOf( restoreVlan ),
                value: String.valueOf( restoreNetworkIndex ),
                ownerId: privateNetworkIndexResource.ownerId
            ) )
          }
      }

      // regular prepare case
      request.getResources( ).findAll{ it instanceof PrivateIPResource }.each { PrivateIPResource privateIPResource ->
        if ( identifiers.add( privateIPResource.ownerId ) ) {
          networkIndexResources.add( new PrivateNetworkIndexResource(
                  tag: String.valueOf( networkGroup.extantNetwork( ).getTag( ) ),
                  value: String.valueOf( networkGroup.extantNetwork( ).allocateNetworkIndex( ).index ),
                  ownerId: privateIPResource.ownerId
          ) )
        }
      }
      db.commit( );
    }

    // Add the private IPs now
    networkIndexResources.findAll{ it instanceof PrivateNetworkIndexResource }.each { PrivateNetworkIndexResource privateNetworkIndexResource ->
      int privateIpInt = getPrivateIpFromSegment( Integer.parseInt( privateNetworkIndexResource.getTag( ) ), Long.parseLong( privateNetworkIndexResource.getValue( ) ) )
      String privateAllocate = PrivateAddresses.allocate( request.vpc, "0.0.0.0", [ privateIpInt ] )
      PrivateIPResource privateResource = new PrivateIPResource( value: privateAllocate, ownerId: privateNetworkIndexResource.ownerId )
      resources.add( privateResource )
    }

    resources
  }
}
