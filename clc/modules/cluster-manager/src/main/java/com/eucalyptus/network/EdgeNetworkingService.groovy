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
package com.eucalyptus.network

import com.eucalyptus.address.Addresses
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException
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
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource
import com.eucalyptus.compute.common.internal.vpc.Subnet
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.Transactions
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.records.Logs
import com.eucalyptus.util.Cidr
import com.eucalyptus.util.Pair
import com.eucalyptus.util.Strings
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.collect.Iterables
import com.google.common.collect.Iterators
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import static com.eucalyptus.compute.common.network.NetworkingFeature.*

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
    boolean vpc = request.vpc != null
    request.resources.each { NetworkResource networkResource ->
      switch( networkResource ) {
        case PublicIPResource:
          resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) )
          break
        case PrivateIPResource:
          if ( !vpc ) {
            resources.addAll( preparePrivateIp( request.availabilityZone, null, null, (PrivateIPResource) networkResource ) )
          }
          break
        case VpcNetworkInterfaceResource:
          resources.addAll( prepareNetworkInterface( request, (VpcNetworkInterfaceResource) networkResource ) )
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
            final Addresses addresses = Addresses.getInstance( );
            addresses.release( addresses.lookupActiveAddress( networkResource.value ), null )
          } catch ( NoSuchElementException e ) {
            logger.info( "IP address not found for release: ${networkResource.value}" )
          } catch ( e ) {
            logger.error( "Error releasing public IP address: ${networkResource.value}", e )
          }
          break
        case PrivateIPResource:
          releasePrivateIp( request.vpc, networkResource.value, networkResource.ownerId, networkResource.ownerId )
          break
        case VpcNetworkInterfaceResource:
          String ip = ((VpcNetworkInterfaceResource)networkResource).privateIp
          releasePrivateIp( request.vpc, ip, networkResource.ownerId, networkResource.value )
          break
      }
    }

    ReleaseNetworkResourcesResponseType.cast( request.reply( new ReleaseNetworkResourcesResponseType( ) ) )
  }

  @Override
  DescribeNetworkingFeaturesResponseType describeFeatures(final DescribeNetworkingFeaturesType request) {
    final Optional<NetworkConfiguration> configurationOptional = NetworkConfigurations.networkConfiguration
    final boolean vpc = configurationOptional.isPresent( ) &&
        NetworkMode.VPCMIDO.toString( ) == configurationOptional.get( ).mode;
    DescribeNetworkingFeaturesResponseType.cast( request.reply( new DescribeNetworkingFeaturesResponseType(
        describeNetworkingFeaturesResult : new DescribeNetworkingFeaturesResult(
            networkingFeatures: vpc ?
                Lists.newArrayList( Vpc, SiteLocalManaged ) :
                Lists.newArrayList( Classic )
        )
    ) ) )
  }

  @Override
  UpdateInstanceResourcesResponseType update(final UpdateInstanceResourcesType request) {
    boolean updated = PublicAddresses.clearDirty( request.resources.publicIps, request.partition )
    PrivateAddresses.releasing( request.resources.privateIps, request.partition )
    UpdateInstanceResourcesResponseType.cast( request.reply( new UpdateInstanceResourcesResponseType(
      updated: updated
    ) ) )
  }

  private Collection<NetworkResource> prepareNetworkInterface(
      final PrepareNetworkResourcesType request,
      final VpcNetworkInterfaceResource vpcNetworkInterfaceResource
  ) {
    VpcNetworkInterfaceResource resource

    if ( vpcNetworkInterfaceResource.mac == null ) { // using existing network interface, nothing to do
      resource = vpcNetworkInterfaceResource
    } else {
      resource = preparePrivateIp(
          null,
          vpcNetworkInterfaceResource.vpc ?: request.vpc,
          vpcNetworkInterfaceResource.subnet ?: request.subnet,
          new PrivateIPResource(
            ownerId: vpcNetworkInterfaceResource.ownerId,
            value: vpcNetworkInterfaceResource.privateIp
          )
      ).getAt( 0 )?.with{
        vpcNetworkInterfaceResource.privateIp = value
        vpcNetworkInterfaceResource
      }
    }

    resource ?
        [ resource ] :
        [ ]
  }

  /**
   * either zone or both vpcId and subnetId must be specified
   */
  private Collection<NetworkResource> preparePrivateIp( final String zone,
                                                        final String vpcId,
                                                        final String subnetId,
                                                        final PrivateIPResource privateIPResource ) {
    PrivateIPResource resource = null
    final Iterable<Integer> addresses
    final int addressCount
    final int allocatedCount
    if ( subnetId != null ) {
      final Pair<Cidr,Integer> cidrAndAvailable = cidrForSubnet( subnetId )
      final IPRange range = IPRange.fromCidr( cidrAndAvailable.getLeft( ) )
      addresses = Iterables.skip( range, 3 )
      addressCount = ((int)range.size( )) - 3
      allocatedCount = addressCount - cidrAndAvailable.right
    } else {
      final Pair<Iterable<Integer>,Integer> addressPair = NetworkConfigurations.getPrivateAddresses( zone )
      addresses = addressPair.left
      addressCount = addressPair.right
      allocatedCount = -1 // unknown
    }
    final String scopeDescription = subnetId != null ? "subnet ${subnetId}" :  "zone ${zone}"
    if ( privateIPResource.value ) { // handle restore
      if ( Iterators.contains( addresses.iterator( ), PrivateAddresses.asInteger( privateIPResource.value ) ) ) {
        try {
          resource = new PrivateIPResource(
              mac: mac( privateIPResource.ownerId  ),
              value: PrivateAddresses.allocate(
                  vpcId,
                  subnetId,
                  [ PrivateAddresses.asInteger( privateIPResource.value ) ],
                  1,
                  -1 ),
              ownerId: privateIPResource.ownerId )
        } catch ( NotEnoughResourcesException e ) {
          if ( PrivateAddresses.verify( vpcId, privateIPResource.value, privateIPResource.ownerId ) ) {
            resource = new PrivateIPResource(
                mac: mac( privateIPResource.ownerId  ),
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
            " for instance ${privateIPResource.ownerId} because address is not valid for ${scopeDescription}" );
      }
    } else {
      resource = new PrivateIPResource(
          mac: mac( privateIPResource.ownerId  ),
          value: PrivateAddresses.allocate( vpcId, subnetId, addresses, addressCount, allocatedCount ),
          ownerId: privateIPResource.ownerId )
    }

    if ( resource && subnetId ) {
      updateFreeAddressesForSubnet( subnetId )
    }

    resource ?
      [ resource ] :
      [ ]
  }

  private void releasePrivateIp( final String vpcId,
                                 final String ip,
                                 final String ownerId,
                                 final String relatedResource ) {
    try {
      String tag = PrivateAddresses.release( vpcId, ip, ownerId )
      if ( Strings.startsWith( 'subnet-' ).apply( tag ) ) {
        updateFreeAddressesForSubnet( tag )
      }
    } catch ( e ) {
      logger.error( "Error releasing private IP address ${ip} for ${relatedResource}", e )
    }
  }

  protected Pair<Cidr,Integer> cidrForSubnet( final String subnetId ) {
    Transactions.one( Subnet.exampleWithName( null, subnetId ), new Function<Subnet, Pair<Cidr,Integer>>( ){
      @Override
      Pair<Cidr,Integer> apply( final Subnet subnet ) {
        return subnet != null ? Pair.pair( Cidr.parse( subnet.getCidr( ) ), subnet.getAvailableIpAddressCount( ) ) : null;
      }
    } )
  }

  protected void updateFreeAddressesForSubnet( final String subnetIdForUpdate ) {
    Entities.asDistinctTransaction( Subnet, new Function<String, Void>( ){
      @Override
      Void apply( final String subnetId ) {
        final Subnet subnet = Entities.uniqueResult( Subnet.exampleWithName( null, subnetId ) )
        subnet.setAvailableIpAddressCount(
            Subnet.usableAddressesForSubnet( subnet.getCidr( ) ) - (int) Entities.count( PrivateAddress.tagged( subnetId ) )
        )
        null
      }
    } ).apply( subnetIdForUpdate );
  }
}
