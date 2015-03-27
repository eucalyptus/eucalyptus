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
import com.eucalyptus.compute.common.internal.network.ExtantNetwork
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.compute.common.internal.network.PrivateNetworkIndex
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException
import com.eucalyptus.compute.common.internal.util.Reference as EReference
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResponseType
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesResult
import com.eucalyptus.compute.common.network.DescribeNetworkingFeaturesType
import com.eucalyptus.compute.common.network.NetworkResource
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
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.TransactionException
import com.eucalyptus.entities.TransactionExecutionException
import com.eucalyptus.entities.TransactionResource
import com.eucalyptus.entities.TransientEntityException
import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.records.EventRecord
import com.eucalyptus.records.EventType
import com.eucalyptus.records.Logs
import com.eucalyptus.util.EucalyptusCloudException
import com.eucalyptus.util.Exceptions
import com.eucalyptus.util.Numbers
import com.eucalyptus.util.Pair
import com.google.common.base.Optional
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger
import org.hibernate.exception.ConstraintViolationException

import javax.persistence.EntityTransaction
import javax.persistence.OptimisticLockException
import javax.transaction.Synchronization
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

import static com.eucalyptus.compute.common.network.NetworkingFeature.*

/**
 * NetworkingService implementation supporting MANAGED[-NOVLAN] modes
 */
@CompileStatic(TypeCheckingMode.SKIP)
class ManagedNetworkingService extends NetworkingServiceSupport {
  private static final Logger logger = Logger.getLogger( ManagedNetworkingService );

  private static final ConcurrentMap<NetworkIndexKey,Long> inFlightNetworkIndexes = Maps.newConcurrentMap();

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
            networkingFeatures: [ Classic ] as ArrayList<NetworkingFeature>
          )
    ) ) )
  }

  @Override
  UpdateInstanceResourcesResponseType update( final UpdateInstanceResourcesType request ) {
    PublicAddresses.clearDirty( request.resources.publicIps, request.partition )
    timeoutPrivateNetworkIndexes( request.resources.privateIps, request.partition )
    UpdateInstanceResourcesResponseType.cast( request.reply( new UpdateInstanceResourcesResponseType( ) ) )
  }

  private Collection<NetworkResource> prepareSecurityGroup( final PrepareNetworkResourcesType request,
                                                            final SecurityGroupResource securityGroupResource ) {
    int retries = 5;
    while ( true ) {
      retries--;
      try {
        return Entities.transaction(NetworkGroup) { EntityTransaction db ->
          final List<NetworkResource> resources = [ ]
          final NetworkGroup networkGroup =
              Entities.uniqueResult( NetworkGroup.withGroupId( null, securityGroupResource.value ) );
          if ( !networkGroup.hasExtantNetwork( ) ) {
            extantNetwork( networkGroup )
            try {
              Entities.flush( NetworkGroup ) // will fail on conflict
            } catch ( Exception e ) {
              if ( retries > 0 && (
                  Exceptions.isCausedBy( e, ConstraintViolationException ) ||
                  Exceptions.isCausedBy( e, OptimisticLockException.class )
              ) ) {
                throw new RetryTransactionException( )
              }
              throw e;
            }
          }
          final Set<String> identifiers = []
          // specific values requested, restore case
          request.getResources( ).findAll{ it instanceof PrivateNetworkIndexResource }.each {
            PrivateNetworkIndexResource privateNetworkIndexResource ->
              if ( identifiers.add( privateNetworkIndexResource.ownerId ) ) {
                Integer restoreVlan = privateNetworkIndexResource.tag
                Long restoreNetworkIndex = Long.valueOf( privateNetworkIndexResource.value )
                if ( networkGroup.hasExtantNetwork( ) && extantNetwork( networkGroup ).getTag( ) == restoreVlan ) {
                  logger.info( "Found matching extant network for ${privateNetworkIndexResource.ownerId}: ${extantNetwork(networkGroup)}" );
                  extantNetwork( networkGroup ).reclaimNetworkIndex( restoreNetworkIndex );
                } else if ( networkGroup.hasExtantNetwork( ) && extantNetwork( networkGroup ).getTag( ) != restoreVlan ) {
                  throw new EucalyptusCloudException( "Found conflicting extant network for ${privateNetworkIndexResource.ownerId}: ${extantNetwork(networkGroup)}" )
                } else {
                  logger.info( "Restoring extant network for ${privateNetworkIndexResource.ownerId}: ${restoreVlan}" );
                  ExtantNetwork exNet = reclaim( networkGroup, restoreVlan );
                  logger.debug( "Restored extant network for ${privateNetworkIndexResource.ownerId}: ${extantNetwork( networkGroup )}" );
                  logger.info( "Restoring private network index for ${privateNetworkIndexResource.ownerId}: ${restoreNetworkIndex}" );
                  exNet.reclaimNetworkIndex( restoreNetworkIndex );
                }
                resources.add( new PrivateNetworkIndexResource(
                    mac: mac( privateNetworkIndexResource.ownerId ),
                    privateIp: privateIp( restoreVlan, restoreNetworkIndex ),
                    tag: restoreVlan,
                    value: String.valueOf( restoreNetworkIndex ),
                    ownerId: privateNetworkIndexResource.ownerId
                ) )
              }
          }

          // regular prepare case
          request.getResources( ).findAll{ it instanceof PrivateIPResource }.each { PrivateIPResource privateIPResource ->
            if ( identifiers.add( privateIPResource.ownerId ) ) {
              final Long index = allocateNetworkIndex( extantNetwork( networkGroup ) ).index;
              resources.add( new PrivateNetworkIndexResource(
                  mac: mac( privateIPResource.ownerId ),
                  privateIp: privateIp( extantNetwork( networkGroup ).tag, index ),
                  tag: extantNetwork( networkGroup ).tag,
                  value: String.valueOf( index ),
                  ownerId: privateIPResource.ownerId
              ) )
            }
          }

          db.commit( )
          resources
        }
      } catch ( RetryTransactionException retry ) {
        //
      }
    }
  }

  private static String privateIp( final Integer tag, final Long index ) {
    final Optional<ManagedSubnet> subnetOptional =
        NetworkConfigurations.networkConfiguration.transform( ManagedSubnets.managedSubnet( ) )
    if ( !subnetOptional.isPresent( ) ) {
      throw new ResourceAllocationException( "Private address allocation failure" );
    }
    ManagedSubnets.indexToAddress( subnetOptional.get(), tag, index )
  }

  private static void timeoutPrivateNetworkIndexes( final Iterable<String> addresses, final String addressPartition ) {
    // Time out pending network indexes that are not reported
    final Optional<ManagedSubnet> subnetOptional =
        NetworkConfigurations.networkConfiguration.transform( ManagedSubnets.managedSubnet( ) )
    if ( subnetOptional.isPresent( ) ) {
      final String partition = addressPartition
      final Set<Pair<Integer,Long>> activeTaggedIndexes =
          Sets.newHashSet( Iterables.transform( addresses, ManagedSubnets.addressToIndex( subnetOptional.get( ) ) ) );
      Entities.transaction( PrivateNetworkIndex.class  ) { final TransactionResource transactionResource ->
        Entities.query( PrivateNetworkIndex.inState( EReference.State.PENDING) ).each { final PrivateNetworkIndex index ->
          if ( isTimedOut(index.lastUpdateMillis(), NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
            if ( !activeTaggedIndexes.contains( Pair.pair( index.extantNetwork.tag, index.index ) ) &&
                ( index.instance == null || index.instance.partition == partition ) ) {
              logger.warn( "Pending network index (${index.displayName}) timed out, tearing down" );
              index.release( )
              index.teardown( )
            }
          }
        }
        transactionResource.commit( )
      }
    }
  }

  private static ExtantNetwork reclaim( NetworkGroup group, Integer i ) throws NotEnoughResourcesException, TransientEntityException {
    if ( !Entities.isPersistent( group ) ) {
      throw new TransientEntityException( group.toString( ) );
    } else {
      ExtantNetwork exNet = Entities.persist( ExtantNetwork.create( group, i ) );
      group.setExtantNetwork( exNet );
      return group.getExtantNetwork( );
    }
  }

  private static ExtantNetwork extantNetwork( NetworkGroup group ) throws NotEnoughResourcesException, TransientEntityException {
    if ( !Entities.isPersistent( group ) ) {
      throw new TransientEntityException( group.toString( ) );
    } else {
      ExtantNetwork exNet = group.getExtantNetwork( );
      if ( exNet == null ) {
        for ( Integer i : Numbers.shuffled( NetworkGroups.networkTagInterval( ) ) ) {
          try {
            Entities.uniqueResult( ExtantNetwork.named( i ) );
          } catch ( Exception ex ) {
            exNet = ExtantNetwork.create( group, i );
            Entities.persist( exNet );
            group.setExtantNetwork( exNet );
            EventRecord.here( NetworkGroup.class, EventType.VLAN_ALLOCATED, "VLAN allocated: " + exNet.getTag( ) ).info();
            return group.getExtantNetwork( );
          }
        }
        throw new NotEnoughResourcesException( "Failed to allocate network tag for network: " + group.getFullName( ) + ": no network tags are free." );
      } else {
        if ( !exNet.inUse( ) ) {
          exNet.updateTimeStamps( );
        }
        return exNet;
      }
    }
  }

  private static boolean isTimedOut( Long timeSinceUpdateMillis, Integer timeoutMinutes ) {
    timeSinceUpdateMillis != null &&
        timeoutMinutes != null &&
        ( timeSinceUpdateMillis > TimeUnit.MINUTES.toMillis( timeoutMinutes )  );
  }

  private static PrivateNetworkIndex allocateNetworkIndex( ExtantNetwork extantNetwork ) throws TransactionException {
    if ( !Entities.isPersistent( extantNetwork ) ) {
      throw new TransientEntityException( extantNetwork.toString( ) );
    } else {
      final Integer tag = extantNetwork.tag
      try {
        Entities.transaction( PrivateNetworkIndex.class ) { final TransactionResource db ->
          final List<Long> networkIndexHolder = Lists.newArrayList();
          Entities.registerSynchronization(ExtantNetwork.class, new Synchronization() {
            @Override
            public void beforeCompletion() {}

            @Override
            public void afterCompletion(final int status) {
              clearInFlight(tag, networkIndexHolder);
            }
          });
          for (final Long i : Numbers.shuffled(NetworkGroups.networkIndexInterval())) {
            try {
              Entities.uniqueResult(PrivateNetworkIndex.named(extantNetwork, i));
            } catch (final NoSuchElementException ex) {
              if (ifNotInFlight(tag, i)) try {
                networkIndexHolder.add(i);
                PrivateNetworkIndex netIdx = Entities.persist(PrivateNetworkIndex.create(extantNetwork, i));
                PrivateNetworkIndex ref = netIdx.allocate();
                db.commit();
                return ref;
              } catch (final Exception ex1) {
                Logs.exhaust().debug(ex1);
              }
            }
          }
          throw new NoSuchElementException();
        }
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        throw new TransactionExecutionException(
            "Too many instances running in security group '"+extantNetwork.displayName+"', unable to allocate a private network " +
            "index. Please either reduce the number of instances in the security group or use another security group.",
            ex );
      }
    }
  }

  private static void clearInFlight( final Integer tag, final Collection<Long> indexes ) {
    for ( final Long index: indexes ) {
      final NetworkIndexKey key = new NetworkIndexKey( tag, index );
      inFlightNetworkIndexes.remove( key );
    }
  }

  private static boolean ifNotInFlight( final Integer tag, final Long index ) {
    final NetworkIndexKey key = new NetworkIndexKey( tag, index );
    final Long reservedTime = inFlightNetworkIndexes.putIfAbsent( key, System.currentTimeMillis() );
    reservedTime == null ||
        ( reservedTime + TimeUnit.MINUTES.toMillis( 1 ) < System.currentTimeMillis() &&
            inFlightNetworkIndexes.replace( key, reservedTime, System.currentTimeMillis() ) );
  }


  private static class RetryTransactionException extends RuntimeException {
  }

  private static final class NetworkIndexKey extends Pair<Integer,Long> {
    private NetworkIndexKey( final Integer tag,
                             final Long index ) {
      super( tag, index )
    }
  }
}
