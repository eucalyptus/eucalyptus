/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network;

import static com.eucalyptus.compute.common.network.NetworkingFeature.Classic;
import static com.eucalyptus.compute.common.network.NetworkingFeature.SiteLocalManaged;
import static com.eucalyptus.compute.common.network.NetworkingFeature.Vpc;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType;
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResult;
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType;
import com.eucalyptus.compute.common.network.NetworkResource;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResponseType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesResultType;
import com.eucalyptus.compute.common.network.PrepareNetworkResourcesType;
import com.eucalyptus.compute.common.network.PrivateIPResource;
import com.eucalyptus.compute.common.network.PublicIPResource;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesResponseType;
import com.eucalyptus.compute.common.network.ReleaseNetworkResourcesType;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesResponseType;
import com.eucalyptus.compute.common.network.UpdateInstanceResourcesType;
import com.eucalyptus.compute.common.network.VpcNetworkInterfaceResource;
import com.eucalyptus.compute.vpc.NotEnoughPrivateAddressResourcesException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.config.NetworkConfigurationApi.NetworkConfiguration;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.google.common.base.Function;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.vavr.control.Option;

/**
 * NetworkingService implementation for EDGE and VPC modes
 */
public class EdgeNetworkingService extends NetworkingServiceSupport {

  private static final Logger logger = Logger.getLogger( EdgeNetworkingService.class );
  private static final CompatSupplier<Boolean> isVpc = CompatSupplier.of( Suppliers.memoizeWithExpiration( ( ) -> {
    @SuppressWarnings( "Guava" )
    final Option<NetworkConfiguration> configurationOptional = NetworkConfigurations.getNetworkConfiguration( );
    return configurationOptional.isDefined( ) &&
        Objects.equals( NetworkMode.VPCMIDO.toString( ), configurationOptional.get( ).mode( ).getOrNull( ) );
  }, 5, TimeUnit.SECONDS ) );

  @SuppressWarnings( "WeakerAccess" )
  public EdgeNetworkingService( ) {
    super( logger );
  }

  @Override
  protected PrepareNetworkResourcesResponseType prepareWithRollback(
      final PrepareNetworkResourcesType request,
      final List<NetworkResource> resources
  ) throws NotEnoughResourcesException {
    boolean vpc = request.getVpc( ) != null;
    for ( NetworkResource networkResource : request.getResources( ) ) {
      if ( PublicIPResource.class.isInstance( networkResource ) ) {
        resources.addAll( preparePublicIp( request, (PublicIPResource) networkResource ) );
      } else if ( PrivateIPResource.class.isInstance( networkResource ) ) {
        if ( !vpc ) {
          resources.addAll( preparePrivateIp(
              request.getAvailabilityZone( ),
              null,
              null,
              (PrivateIPResource) networkResource ) );
        }
      } else if ( VpcNetworkInterfaceResource.class.isInstance( networkResource ) ) {
        resources.addAll( prepareNetworkInterface( request, (VpcNetworkInterfaceResource) networkResource ) );
      }
    }
    return request.reply( new PrepareNetworkResourcesResponseType(
        new PrepareNetworkResourcesResultType( Lists.newArrayList( resources ) )
    ) );
  }

  @Override
  public ReleaseNetworkResourcesResponseType release( final ReleaseNetworkResourcesType request ) {
    for ( NetworkResource networkResource : request.getResources( ) ) {
      if ( PublicIPResource.class.isInstance( networkResource ) ) {
        try {
          final Addresses addresses = Addresses.getInstance( );
          addresses.release( addresses.lookupActiveAddress( networkResource.getValue( ) ), null );
        } catch ( NoSuchElementException e ) {
          logger.info( "IP address not found for release: " + networkResource.getValue( ) );
        } catch ( Exception e ) {
          logger.error( "Error releasing public IP address: " + networkResource.getValue( ), e );
        }
      } else if ( PrivateIPResource.class.isInstance( networkResource ) ) {
        releasePrivateIp(
            request.getVpc( ),
            networkResource.getValue( ),
            networkResource.getOwnerId( ) );
      } else if ( VpcNetworkInterfaceResource.class.isInstance( networkResource ) ) {
        String ip = ( (VpcNetworkInterfaceResource) networkResource ).getPrivateIp( );
        releasePrivateIp(
            request.getVpc( ),
            ip,
            networkResource.getValue( ) );
      }
    }
    return request.reply( new ReleaseNetworkResourcesResponseType( ) );
  }

  @Override
  public DescribeNetworkingFeaturesResponseType describeFeatures( final DescribeNetworkingFeaturesType request ) {
    return request.reply( new DescribeNetworkingFeaturesResponseType(
        new DescribeNetworkingFeaturesResult( isVpc.get( ) ?
            Lists.newArrayList( Vpc, SiteLocalManaged ) :
            Lists.newArrayList( Classic ) ) ) );
  }

  @Override
  public UpdateInstanceResourcesResponseType update( final UpdateInstanceResourcesType request ) {
    boolean updated = false;
    if ( !isVpc.get( ) ) {
      updated = PublicAddresses.clearDirty( request.getResources( ).getPublicIps( ), request.getPartition( ) );
    }
    updated = PrivateAddresses.releasing( request.getResources( ).getPrivateIps( ), request.getPartition( ) ) || updated;
    return request.reply( new UpdateInstanceResourcesResponseType( updated ) );
  }

  private Collection<NetworkResource> prepareNetworkInterface(
      final PrepareNetworkResourcesType request,
      final VpcNetworkInterfaceResource vpcNetworkInterfaceResource
  ) throws NotEnoughResourcesException {
    VpcNetworkInterfaceResource resource = null;

    if ( vpcNetworkInterfaceResource.getMac( ) == null ) { // using existing network interface, nothing to do
      resource = vpcNetworkInterfaceResource;
    } else {
      NetworkResource privateIpResource = Iterables.getFirst( preparePrivateIp(
          null,
          vpcNetworkInterfaceResource.getVpc( ) != null ? vpcNetworkInterfaceResource.getVpc( ) : request.getVpc( ),
          vpcNetworkInterfaceResource.getSubnet( ) != null ? vpcNetworkInterfaceResource.getSubnet( ) : request.getSubnet( ),
          new PrivateIPResource(
              vpcNetworkInterfaceResource.getValue( ),
              vpcNetworkInterfaceResource.getPrivateIp( ),
              null ) ), null );

      if ( privateIpResource != null ) {
        vpcNetworkInterfaceResource.setPrivateIp( privateIpResource.getValue( ) );
        resource = vpcNetworkInterfaceResource;
      }
    }

    return resource != null ?
        Lists.newArrayList( resource ) :
        Lists.newArrayList( );
  }

  /**
   * either zone or both vpcId and subnetId must be specified
   */
  private Collection<NetworkResource> preparePrivateIp(
      final String zone,
      final String vpcId,
      final String subnetId,
      final PrivateIPResource privateIPResource
  ) throws NotEnoughResourcesException {
    PrivateIPResource resource = null;
    final Iterable<Integer> addresses;
    final int addressCount;
    final int allocatedCount;
    if ( subnetId != null ) {
      final Pair<Cidr, Integer> cidrAndAvailable = cidrForSubnet( subnetId );
      final IPRange range = IPRange.fromCidr( cidrAndAvailable.getLeft( ) );
      addresses = Iterables.skip( range, 3 );
      addressCount = range.size( ) - 3;
      allocatedCount = addressCount - cidrAndAvailable.getRight( );
    } else {
      final Pair<Iterable<Integer>, Integer> addressPair = NetworkConfigurations.getPrivateAddresses( zone );
      addresses = addressPair.getLeft( );
      addressCount = addressPair.getRight( );
      allocatedCount = -1;// unknown
    }

    final String scopeDescription = subnetId != null ? "subnet " + subnetId : "zone " + zone;
    if ( privateIPResource.getValue( ) != null ) {// handle restore
      if ( Iterators.contains( addresses.iterator( ), PrivateAddresses.asInteger( privateIPResource.getValue( ) ) ) ) {
        try {
          resource = new PrivateIPResource(
              privateIPResource.getOwnerId( ),
              PrivateAddresses.allocate(
                  vpcId,
                  subnetId,
                  Lists.newArrayList( PrivateAddresses.asInteger( privateIPResource.getValue( ) ) ),
                  1,
                  -1 ),
              NetworkingServiceSupport.mac( privateIPResource.getOwnerId( ) ) );
        } catch ( NotEnoughResourcesException e ) {
          if ( PrivateAddresses.verify(
              vpcId,
              privateIPResource.getValue( ),
              privateIPResource.getOwnerId( ) ) ) {
            resource = new PrivateIPResource(
                privateIPResource.getOwnerId( ),
                privateIPResource.getValue( ),
                NetworkingServiceSupport.mac( privateIPResource.getOwnerId( ) ) );
          } else {
            logger.error( "Failed to restore private address " + privateIPResource.getValue( ) + " for instance " +
                privateIPResource.getOwnerId( ) + " because of  " + e.getMessage( ) );
            Logs.extreme( ).error( e, e );
          }
        }
      } else {
        logger.error( "Failed to restore private address " + privateIPResource.getValue( ) + " for instance " +
            privateIPResource.getOwnerId( ) + " because address is not valid for " + scopeDescription );
      }
    } else {
      try {
        resource = new PrivateIPResource(
            privateIPResource.getOwnerId( ),
            PrivateAddresses.allocate(
                vpcId,
                subnetId,
                addresses,
                addressCount,
                allocatedCount ),
            NetworkingServiceSupport.mac( privateIPResource.getOwnerId( ) ) );
      } catch ( NotEnoughResourcesException e ) {
        throw subnetId == null ? e : new NotEnoughPrivateAddressResourcesException( e.getMessage( ) );
      }
    }

    if ( resource != null && subnetId != null ) {
      updateFreeAddressesForSubnet( subnetId );
    }

    return resource != null ?
        Lists.newArrayList( resource ) :
        Lists.newArrayList( );
  }

  private void releasePrivateIp( final String vpcId, final String ip, final String ownerId ) {
    try {
      String tag = PrivateAddresses.release( vpcId, ip, ownerId );
      if ( Strings.startsWith( "subnet-" ).apply( tag ) ) {
        updateFreeAddressesForSubnet( tag );
      }

    } catch ( Exception e ) {
      logger.error( "Error releasing private IP address " + ip + " for " + ownerId, e );
    }
  }

  @SuppressWarnings( "deprecation" )
  private Pair<Cidr, Integer> cidrForSubnet( final String subnetId ) {
    try {
      //noinspection Convert2Lambda
      return Transactions.one( Subnet.exampleWithName( null, subnetId ), new Function<Subnet, Pair<Cidr, Integer>>( ) {
        @Override
        public Pair<Cidr, Integer> apply( final Subnet subnet ) {
          return subnet != null ? Pair.pair( Cidr.parse( subnet.getCidr( ) ), subnet.getAvailableIpAddressCount( ) ) : null;
        }
      } );
    } catch ( TransactionException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  private void updateFreeAddressesForSubnet( final String subnetIdForUpdate ) {
    try {
      //noinspection Convert2Lambda
      Entities.asDistinctTransaction( Subnet.class, new Function<String, Void>( ) {
        @SuppressWarnings( "deprecation" )
        @Override
        public Void apply( final String subnetId ) {
          final Subnet subnet;
          try {
            subnet = Entities.uniqueResult( Subnet.exampleWithName( null, subnetId ) );
          } catch ( TransactionException e ) {
            throw Exceptions.toUndeclared( e );
          }
          subnet.setAvailableIpAddressCount(
              Subnet.usableAddressesForSubnet( subnet.getCidr( ) ) -
                  (int) Entities.count( PrivateAddress.tagged( subnetId ) ) );
          return null;
        }
      } ).apply( subnetIdForUpdate );
    } catch ( Exception e ) {
      if ( PersistenceExceptions.isStaleUpdate( e ) ) {
        logger.warn( "Unable to update free addresses for subnet " + subnetIdForUpdate );
      } else {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
