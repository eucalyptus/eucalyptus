/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.address;

import static com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity.FilterFunctions.*;
import static com.eucalyptus.reporting.event.ResourceAvailabilityEvent.ResourceType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.address.Address.AddressInfo;
import com.eucalyptus.address.Address.AddressStateTransition;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.internal.address.AddressDomain;
import com.eucalyptus.compute.common.internal.address.AddressI;
import com.eucalyptus.compute.common.internal.address.AddressState;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.NetworkInfoBroadcaster;
import com.eucalyptus.reporting.event.AddressEvent;
import com.eucalyptus.reporting.event.EventActionInfo;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Predicates;
import com.google.common.base.Suppliers;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.reporting.event.ResourceAvailabilityEvent;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import groovy.lang.Closure;

public class Addresses {

  private static final String ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY = "InsufficientAddressCapacity";

  private static final Logger LOG = Logger.getLogger( Addresses.class );

  private static final AtomicReference<Iterable<String>> configuredAddresses = new AtomicReference<>( );

  private static final Addresses instance = new Addresses( new AllocatedAddressPersistenceImpl( ) );

  private static ThreadLocal<AddressingBatch> batchThreadLocal = new ThreadLocal<>( );

  private final Supplier<Void> storedAddressLoadingSupplier = Suppliers.memoizeWithExpiration( new Supplier<Void>(){
    @Override
    public Void get( ) {
      loadStoredAddresses( );
      return null;
    }
  }, 1, TimeUnit.MINUTES );

  private final AllocatedAddressPersistence allocatedAddressPersistence;
  private final AddressRegistry addressRegistry = AddressRegistry.getInstance( );

  public static Addresses getInstance( ) {
    return instance;
  }

  public Addresses( final AllocatedAddressPersistence allocatedAddressPersistence ) {
    this.allocatedAddressPersistence = allocatedAddressPersistence;
  }

  public List<Address> listActiveAddresses( ) {
    return addressRegistry.listValues( );
  }

  public List<Address> listInactiveAddresses( ) {
    return addressRegistry.listDisabledValues( );
  }

  public Address lookupActiveAddress( final String ip ) {
    return addressRegistry.lookup( ip );
  }

  public Allocator allocator( final AddressDomain domain ) {
    return new Allocator( this, domain );
  }

  public Address allocateNext( final UserFullName userId, final AddressDomain domain ) throws NotEnoughResourcesException {
    final Predicate<Address> predicate = RestrictedTypes.filterPrivileged( );
    final Address address;
    try {
      address = addressRegistry.enableFirst( predicate );
    } catch ( final NoSuchElementException e ) {
      throw new NotEnoughResourcesException( ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }

    final Optional<AddressStateTransition> transition = address.allocate( userId, domain );
    if ( !transition.isPresent( ) ) {
      throw new NotEnoughResourcesException( ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }

    try {
      call( new Callable<Void>( ) {
        @Override
        public Void call( ) throws Exception {
          allocatedAddressPersistence.save( createEntity( transition.get( ).newAddressInfo( ) ) );
          return null;
        }
      } );
    } catch ( final Exception e ) {
      LOG.error( "Persistence error for allocated address, attempting rollback " + address.getDisplayName( ), e );
      if ( transition.get( ).rollback( ) ) {
        disable( address.getAddress( ) );
      }
      throw new NotEnoughResourcesException( ERR_SYS_INSUFFICIENT_ADDRESS_CAPACITY );
    }

    LOG.debug( "Allocated address for public addressing: " + String.valueOf( address ) );
    fireUsageEvent( userId, address.getDisplayName( ), Suppliers.ofInstance( AddressEvent.forAllocate( ) ) );
    return address;
  }

  public Address allocateSystemAddress( ) throws NotEnoughResourcesException {
    return doAllocateSystemAddress( Optional.<String>absent( ) );
  }

  public Address allocateSystemAddress( final String address ) throws NotEnoughResourcesException {
    return doAllocateSystemAddress( Optional.of( address ) );
  }

  public void assignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    doAssignSystemAddress( vm );
  }

  public boolean assign( final Address address, final VmInstance vm ) {
    try ( final AddressingBatch batch = batch( ) ) {
      boolean assigned = false;
      if ( address.getOwnerAccountNumber( ) == null ||
          vm.getOwnerAccountNumber( ).equals( address.getOwnerAccountNumber( ) ) ) {
        boolean disableAddressOnFailure = false;
        if ( !address.isAllocated( ) ) {
          final Optional<Address> enabledAddress = addressRegistry.tryEnable( address.getAddress( ) );
          disableAddressOnFailure = enabledAddress.isPresent( );
        }

        final Optional<AddressStateTransition> assignTransition = address.assign( vm );
        if ( assignTransition.isPresent( ) ) {
          assigned = store( address, assignTransition.get( ).newAddressInfo( ) ) || !assignTransition.get( ).rollback( );
        }
        if ( assigned && assignTransition.get( ).newAddressInfo( ).getOwnerUserId( ) != null ) {
          fireAssociateUsageEvent( assignTransition.get( ) );
        }
        if ( !assigned && disableAddressOnFailure ) {
          disable( address.getAddress( ) );
        }
        if ( assigned ) {
          addressFlushRequired( );
        }
      }
      return assigned;
    }
  }

  /**
   * EC2 classic unassign
   */
  public boolean unassign( final String address ) {
    boolean unassign = false;
    try {
       unassign = unassign( lookupActiveAddress( address ) );
    } catch ( NoSuchElementException e ) {
      // not unassigned
    }
    return unassign;
  }

  /**
   * EC2 classic unassign
   */
  public boolean unassign( final Address address ) {
    try ( final AddressingBatch batch = batch( ) ) {
      boolean unassign = false;
      final Optional<AddressStateTransition> unassignTransition = address.unassign( );
      if ( unassignTransition.isPresent( ) ) {
        unassign = store( address, unassignTransition.get( ).newAddressInfo( ) ) || !unassignTransition.get( ).rollback( );
      }
      if ( unassign && unassignTransition.get( ).newAddressInfo( ).getOwnerUserId( ) != null ) {
        fireDisassociateUsageEvent( unassignTransition.get( ) );
      }
      if ( unassign && !unassignTransition.get( ).newAddressInfo( ).isAllocated( ) ) {
        disable( address.getAddress( ) );
      }
      if ( unassign ) {
        addressFlushRequired( );
      }
      return unassign;
    }
  }

  public boolean assign( final Address address, final NetworkInterface eni ) {
    boolean assigned = false;
    boolean disableAddressOnFailure = false;
    if ( !address.isAllocated( ) ) {
      final Optional<Address> enabledAddress = addressRegistry.tryEnable( address.getAddress( ) );
      disableAddressOnFailure = enabledAddress.isPresent( );
    }

    final Optional<AddressStateTransition> assignTransition = address.assign( eni );
    if ( assignTransition.isPresent( ) ) {
      assigned = store( address, assignTransition.get( ).newAddressInfo( ) ) || !assignTransition.get( ).rollback( );
    }
    if ( !assigned && disableAddressOnFailure ) {
      disable( address.getAddress( ) );
    }
    return assigned;
  }

  /**
   * EC2 vpc unassign
   */
  public boolean unassign( final Address address, final String associationId ) {
    boolean unassign = false;
    final Optional<AddressStateTransition> unassignTransition = address.unassign( associationId );
    if ( unassignTransition.isPresent( ) ) {
      unassign = store( address, unassignTransition.get( ).newAddressInfo( ) ) || !unassignTransition.get( ).rollback( );
    }
    if ( unassign &&
        unassignTransition.get( ).oldAddressInfo( ).getOwnerUserId( ) != null &&
        unassignTransition.get( ).oldAddressInfo( ).getInstanceId( ) != null ) {
      fireDisassociateUsageEvent( unassignTransition.get( ) );
    }
    if ( unassign && !unassignTransition.get( ).newAddressInfo( ).isAllocated( ) ) {
      disable( address.getAddress( ) );
    }
    return unassign;
  }

  public boolean start( final Address address, final VmInstance vm ) {
    try ( final AddressingBatch batch = batch( ) ) {
      boolean start = false;
      final Optional<AddressStateTransition> startTransition = address.start( vm );
      if ( startTransition.isPresent( ) ) {
        start = store( address, startTransition.get( ).newAddressInfo( ) ) || !startTransition.get( ).rollback( );
      }
      if ( start && startTransition.get( ).newAddressInfo( ).getOwnerUserId( ) != null ) {
        final AddressStateTransition transition = startTransition.get( );
        fireAssociateUsageEvent( transition );
      }
      if ( start ) {
        addressFlushRequired( );
      }
      return start;
    }
  }

  public boolean stop( final Address address ) {
    try ( final AddressingBatch batch = batch( ) ) {
      boolean stop = false;
      final Optional<AddressStateTransition> stopTransition = address.stop( );
      if ( stopTransition.isPresent( ) ) {
        stop = store( address, stopTransition.get( ).newAddressInfo( ) ) || !stopTransition.get( ).rollback( );
      }
      if ( stop && stopTransition.get( ).oldAddressInfo( ).getOwnerUserId( ) != null ) {
        fireDisassociateUsageEvent( stopTransition.get( ) );
      }
      if ( stop ) {
        addressFlushRequired( );
      }
      return stop;
    }
  }

  public void system( final VmInstance vm ) {
    try {
      if ( !vm.isUsePrivateAddressing() &&
          (VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) ) ) {
        assignSystemAddress( vm );
      }
    } catch ( final NotEnoughResourcesException e ) {
      LOG.warn( "No addresses are available to provide a system address for: " + LogUtil.dumpObject( vm ) );
      LOG.debug( e, e );
    }
  }

  public boolean release( final Address address, final String allocationId ) {
    try ( final AddressingBatch batch = batch( ) ) {
      boolean release = false;
      Optional<AddressStateTransition> releaseTransition = Optional.absent( );
      while ( address.isAllocated( ) &&
          !releaseTransition.isPresent( ) &&
          ( allocationId==null || allocationId.equals( address.getAllocationId( ) ) ) ) {
        releaseTransition = address.release( allocationId );
      }

      if ( releaseTransition.isPresent( ) ) {
        if ( releaseTransition.get( ).oldAddressInfo( ).isReallyAssigned( ) &&
            releaseTransition.get( ).oldAddressInfo( ).getInstanceId( ) != null ) {
          if ( releaseTransition.get( ).oldAddressInfo( ).getOwnerUserId( ) != null ) {
            fireDisassociateUsageEvent( releaseTransition.get( ) );
          }
          try {
            final VmInstance instance = VmInstances.lookup( releaseTransition.get( ).oldAddressInfo( ).getInstanceId( ) );
            if ( address.getAddress( ).equals( instance.getPublicAddress( ) ) ) {
              Addresses.updatePublicIpByInstanceId( instance.getDisplayName( ), null );
            }
            try {
              system( instance );
            } catch ( NoSuchElementException e ) {
              LOG.debug( e, e );
            } catch ( Exception e ) {
              LOG.error( "Error assigning system address for instance " + instance.getDisplayName( ), e );
            }
          } catch ( NoSuchElementException ex ) {
            Logs.extreme( ).error( ex );
          }
        }

        try {
          if ( allocatedAddressPersistence.delete(
              createEntity( releaseTransition.get( ).oldAddressInfo( ) ),
              new Predicate<AllocatedAddressEntity>( ) {
                @Override
                public boolean apply( final AllocatedAddressEntity addressEntity ) {
                  return allocationId == null || allocationId.equals( addressEntity.getAllocationId( ) );
                }
              }
          ) ) {
            release = true;
            disable( address.getAddress( ) );

            LOG.debug( "Released address: " + String.valueOf( address ) );
            final String oldAccountNumber = releaseTransition.get( ).oldAddressInfo( ).getOwnerAccountNumber( );
            final String oldUserId = releaseTransition.get( ).oldAddressInfo( ).getOwnerUserId( );
            if ( oldAccountNumber != null && oldUserId != null ) {
              fireUsageEvent(
                  UserFullName.getInstanceForAccount( oldAccountNumber, oldUserId ),
                  address.getDisplayName( ),
                  Suppliers.ofInstance( AddressEvent.forRelease( ) ) );
            }
          } else {
            releaseTransition.get( ).rollback( );
          }
        } catch ( final Exception e ) {
          LOG.error( "Persistence error for allocated address, attempting rollback " + address.getDisplayName( ), e );
          releaseTransition.get( ).rollback( );
        }
      }
      return release;
    }
  }

  public static <R> R withBatch( final Closure<R> closure ) {
    //noinspection unused
    try ( final AddressingBatch batch = Addresses.getInstance( ).batch( ) ) {
      return closure.call( );
    }
  }

  public AddressingBatch batch( ) {
    if ( batchThreadLocal.get( ) != null ) {
      return new AddressingBatch( ) {
        @Override
        public void close( ){
        }
      };
    } else {
      batchThreadLocal.set( new AddressingBatch( ) );
      return batchThreadLocal.get( );
    }
  }

  private void addressFlushRequired( ) {
    AddressingBatch.flush( );
  }

  private Address doAllocateSystemAddress( Optional<String> requestedAddress ) throws NotEnoughResourcesException {
    final Optional<Address> address = requestedAddress.isPresent( ) ?
        addressRegistry.tryEnable( requestedAddress.get( ) ) :
        addressRegistry.tryEnable( );
    if ( !address.isPresent( ) ) {
      throw new NotEnoughAddressResourcesException( );
    }
    final Optional<AddressStateTransition> pendingTransition = address.get( ).pendingAssignment( );
    if ( pendingTransition.isPresent( ) ) {
      try {
        call( new Callable<Void>( ) {
           @Override
           public Void call( ) throws Exception {
             allocatedAddressPersistence.save( createEntity( pendingTransition.get( ).newAddressInfo( ) ) );
             return null;
           }
         } );
      } catch ( final Exception e ) {
        LOG.error( "Persistence error for system address, attempting rollback " + address.get( ).getDisplayName( ), e );
        if ( pendingTransition.get( ).rollback( ) ) {
          disable( address.get( ).getName( ) );
        }
        throw new NotEnoughAddressResourcesException( );
      }
    } else {
      LOG.error( "Error transitioning enabled address: " + address );
      throw new NotEnoughAddressResourcesException( );
    }
    return address.get( );
  }

  static AllocatedAddressEntity createEntity( final Address.AddressInfo addressInfo ) {
    final AllocatedAddressEntity entity = AllocatedAddressEntity.create( );
    updateEntity( addressInfo, entity );
    return entity;
  }

  static void updateEntity( final Address.AddressInfo addressInfo, final AllocatedAddressEntity entity ) {
    if ( entity.getDisplayName( ) != null && !entity.getDisplayName( ).equals( addressInfo.getAddress( ) ) ) {
      throw new IllegalArgumentException( "Attempt to update with invalid address" );
    }
    entity.setDisplayName( addressInfo.getAddress( ) );
    entity.setDomain( addressInfo.getDomain( ) );
    entity.setState( addressInfo.getState( ) );
    if ( addressInfo.getOwnerUserId( ) != null ) {
      entity.setOwnerAccountNumber( addressInfo.getOwnerAccountNumber( ) );
      entity.setOwnerUserId( addressInfo.getOwnerUserId( ) );
      entity.setOwnerUserName( addressInfo.getOwnerUserName( ) );
    } else { // system owned
      entity.setOwner( Principals.systemFullName( ) );
    }
    entity.setAllocationId( addressInfo.getAllocationId( ) );
    entity.setAssociationId( addressInfo.getAssociationId( ) );
    entity.setNetworkInterfaceId( addressInfo.getNetworkInterfaceId( ) );
    entity.setNetworkInterfaceOwnerId( addressInfo.getNetworkInterfaceOwnerId( ) );
    entity.setPrivateAddress( addressInfo.getPrivateAddress( ) );
    entity.setInstanceId( addressInfo.getInstanceId( ) );
    entity.setInstanceUuid( addressInfo.getInstanceUuid( ) );
  }

  private boolean store( final Address address, final AddressInfo addressInfo ) {
    try { // Ensure no enclosing transaction.
      return call( new Callable<Boolean>( ) {
        @Override
        public Boolean call() throws Exception {
          AllocatedAddressPersistenceException lastException = null;
          for ( int i=0; i<Entities.CONCURRENT_UPDATE_RETRIES; i++ ) try {
            allocatedAddressPersistence.updateByExample(
                AllocatedAddressEntity.exampleWithAddress( addressInfo.getAddress( ) ),
                null,
                addressInfo.getAddress( ),
                new Callback<AllocatedAddressEntity>( ) {
                  @Override
                  public void fire( final AllocatedAddressEntity addressEntity ) {
                    if ( !addressInfo.isAllocated( ) ) {
                      Entities.delete( addressEntity );
                    } else {
                      Addresses.updateEntity( addressInfo, addressEntity );
                    }
                  }
                }
            );
            return true;
          } catch ( AllocatedAddressPersistenceException e ) {
            lastException = e;
            if ( address.getStateVersion( ) > addressInfo.getStateVersion( ) || // our update is stale, drop it
                ( !PersistenceExceptions.isLockError( e ) && !PersistenceExceptions.isStaleUpdate( e ) ) ) {
              break;
            }
          }
          if ( lastException != null ) {
            LOG.error( "Error storing address " + addressInfo.getAddress( ), lastException );
          }
          return false;
        }
      } );
    } catch ( ExecutionException e ) {
      LOG.error( "Error storing address " + addressInfo.getAddress( ), e );
    }
    return false;
  }

  private <T> T call( final Callable<T> callable ) throws ExecutionException {
    try {
      return Threads.enqueue( Compute.class, Addresses.class, callable ).get( );
    } catch ( InterruptedException e ) {
      Thread.currentThread( ).interrupt( );
      throw new ExecutionException( e );
    }
  }

  private void doAssignSystemAddress( final VmInstance vm ) throws NotEnoughResourcesException {
    final String instanceId = vm.getInstanceId();
    final Address addr = this.allocateSystemAddress( );
    if ( assign( addr, vm ) ) {
      updatePublicIpByInstanceId( instanceId, addr.getName( ) );
    }
  }

  private void disable( final String name ) {
    try {
      addressRegistry.disable( name );
    } catch ( NoSuchElementException e ) {
      // so nothing to disable
    }
    checkRemove( name );
  }

  private void checkRemove( String address ) {
    final Iterable<String> configuredAddresses = Addresses.configuredAddresses.get( );
    if ( configuredAddresses != null &&
        !Iterables.isEmpty( configuredAddresses ) &&
        !Iterables.contains( configuredAddresses, address ) ) {
      addressRegistry.deregisterDisabled( address );
    }
  }

  /**
   * Update addresses from the list assign (system) to instances if necessary.
   */
  public void update( final Iterable<String> addressIterable ) {
    final Collection<String> addresses = Collections.unmodifiableCollection( Sets.newLinkedHashSet( addressIterable ) );
    Addresses.configuredAddresses.set( addresses );
    storedAddressLoadingSupplier.get( );
    for ( final String address : addresses ) {
      lookupOrCreate( address );
    }
    for ( final Address address : addressRegistry.listDisabledValues( ) ) {
      checkRemove( address.getName( ) );
    }
  }

  private Address lookupOrCreate( final String address ) {
    Address addr = null;
    try {
      addr = addressRegistry.lookupDisabled( address );
      LOG.trace( "Found address in the inactive set cache: " + addr );
    } catch ( final NoSuchElementException e1 ) {
      try {
        addr = addressRegistry.lookup( address );
        LOG.trace( "Found address in the active set cache: " + addr );
      } catch ( final NoSuchElementException e ) {}
    }
    if ( addr == null ) try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
      VmInstance vm = maybeFindVm( null, address, null );
      addr = new Address( address );
      if ( vm != null ) {
        addr.pendingAssignment( );
        final Optional<AddressStateTransition> transition;
        if ( vm.getVpcId( ) == null) {
          transition = addr.assign( vm );
        } else {
          transition = addr.assign( Iterables.getOnlyElement( vm.getNetworkInterfaces( ) ) );
        }
        if ( transition.isPresent( ) ) {
          store( addr, transition.get( ).newAddressInfo( ) );
        }
        addressRegistry.register( addr );
      } else {
        addressRegistry.registerDisabled( addr );
      }
    }
    return addr;
  }

  private VmInstance maybeFindVm( final String instanceId, final String publicIp, final String privateIp ) {
    VmInstance vm = null;
    if ( instanceId != null ) {
      try {
        vm = VmInstances.lookup( instanceId );
      } catch ( NoSuchElementException ex ) {
        Logs.extreme( ).error( ex );
      }
    }
    if ( vm == null && privateIp != null ) {
      try {
        vm = VmInstances.lookupByPrivateIp( privateIp );
      } catch ( NoSuchElementException ex ) {
        Logs.extreme( ).error( ex );
      }
    }
    if ( vm == null && publicIp != null ) {
      try {
        vm = VmInstances.lookupByPublicIp( publicIp );
      } catch ( NoSuchElementException ex ) {
        Logs.extreme( ).error( ex );
      }
    }
    if ( vm != null && VmState.RUNNING.equals( vm.getState( ) ) && publicIp.equals( vm.getPublicAddress( ) ) ) {
      Logs.extreme( ).debug( "Candidate vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
      if ( publicIp.equals( vm.getPublicAddress( ) ) ) {
        Logs.extreme( ).debug( "Found vm which claims this address: " + vm.getInstanceId( ) + " " + vm.getState( ) + " " + publicIp );
      }
      return vm;
    } else {
      return null;
    }
  }

  private void loadStoredAddresses( ) {
    try {
      for ( AllocatedAddressEntity addr : allocatedAddressPersistence.list( null ) ) {
        if ( !addressRegistry.contains( addr.getName( ) ) ) {
          try {
            final Address address = new Address( addr.getName( ) );
            if ( addr.getState( ) == null ) {
              continue; // state may be null if DB not upgraded
            }
            if ( addr.getState( ).ordinal( ) < AddressState.allocated.ordinal( ) ) {
              allocatedAddressPersistence.delete( addr, Predicates.alwaysTrue( ) );
              continue; // do not register
            } else if ( addr.getState( ) == AddressState.allocated ) {
              address.allocate( addr.getOwnerAccountNumber( ), addr.getOwnerUserId( ), addr.getOwnerUserName( ), addr.domainWithDefault( ), addr.getAllocationId( ) );
            } else if ( addr.getState( ) == AddressState.impending ) {
              address.pendingAssignment( );
            } else if ( addr.getState( ) == AddressState.assigned || addr.getState( ) == AddressState.started ) {
              if ( addr.getOwnerAccountNumber( ) == null ||
                  addr.getOwnerAccountNumber( ).equals( Principals.systemAccount( ).getAccountNumber( ) ) ) {
                address.pendingAssignment( );
              } else {
                address.allocate( addr.getOwnerAccountNumber( ), addr.getOwnerUserId( ), addr.getOwnerUserName( ), addr.domainWithDefault( ), addr.getAllocationId( )  );
              }
              if ( addr.getNetworkInterfaceId( ) == null ) {
                address.assignClassic( addr.getInstanceId( ), addr.getInstanceUuid( ), addr.getPrivateAddress( ) );
              } else {
                address.assignVpc( addr.getNetworkInterfaceId( ), addr.getNetworkInterfaceOwnerId( ), addr.getPrivateAddress( ), addr.getAssociationId( ) );
                if ( addr.getState( ) == AddressState.started ) {
                  address.start( addr.getInstanceId( ), addr.getInstanceUuid( ) );
                }
              }
            }
            addressRegistry.register( address );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      }
    } catch ( final Exception e ) {
      LOG.debug( e, e );
    }
  }

  private void fireAssociateUsageEvent( final AddressStateTransition transition ) {
    final String oldAccountNumber = transition.oldAddressInfo( ).getOwnerAccountNumber( );
    final String oldUserId = transition.oldAddressInfo( ).getOwnerUserId( );
    if ( oldAccountNumber != null && oldUserId != null ) {
      fireUsageEvent(
          UserFullName.getInstanceForAccount( oldAccountNumber, oldUserId ),
          transition.newAddressInfo( ).getAddress( ),
          new Supplier<EventActionInfo<AddressEvent.AddressAction>>( ) {
            @Override
            public EventActionInfo<AddressEvent.AddressAction> get( ) {
              return AddressEvent.forAssociate(
                  transition.newAddressInfo( ).getInstanceUuid( ),
                  transition.newAddressInfo( ).getInstanceId( )
              );
            }
          }
      );
    }
  }

  private void fireDisassociateUsageEvent( final AddressStateTransition transition ) {
    final String oldAccountNumber = transition.oldAddressInfo( ).getOwnerAccountNumber( );
    final String oldUserId = transition.oldAddressInfo( ).getOwnerUserId( );
    if ( oldAccountNumber != null && oldUserId != null ) {
      fireUsageEvent(
          UserFullName.getInstanceForAccount( oldAccountNumber, oldUserId ),
          transition.oldAddressInfo( ).getAddress( ),
          new Supplier<EventActionInfo<AddressEvent.AddressAction>>( ) {
            @Override
            public EventActionInfo<AddressEvent.AddressAction> get( ) {
              return AddressEvent.forDisassociate(
                  transition.oldAddressInfo( ).getInstanceUuid( ),
                  transition.oldAddressInfo( ).getInstanceId( )
              );
            }
          }
      );
    }
  }

  private void fireUsageEvent( final OwnerFullName ownerFullName,
                               final String address,
                               final Supplier<EventActionInfo<AddressEvent.AddressAction>> actionInfoSupplier ) {
    if ( !Principals.isFakeIdentityAccountNumber( ownerFullName.getAccountNumber() ) ) {
      try {
        ListenerRegistry.getInstance( ).fireEvent(
            AddressEvent.with(
                address,
                ownerFullName,
                Accounts.lookupAccountAliasById( ownerFullName.getAccountNumber( ) ),
                actionInfoSupplier.get() ) );
      } catch ( final Throwable e ) {
        LOG.error( e, e );
      }
    }
  }

  public static class AddressingBatch implements AutoCloseable {
    private boolean flushRequested;

    @Override
    public void close( ) {
      if ( flushRequested ) {
        flushNow( );
      }
      reset( );
    }

    public static void reset( ) {
      final AddressingBatch batch = batchThreadLocal.get( );
      if ( batch != null ) {
        batchThreadLocal.set( null );
        batch.flushRequested = false;
      }
    }

    static void flush( ) {
      if ( batchThreadLocal.get( ) == null ) {
        flushNow( );
      } else {
        batchThreadLocal.get( ).flushRequested = true;
      }
    }

    static void flushNow( ) {
      NetworkInfoBroadcaster.requestNetworkInfoBroadcast( );
    }
  }

  @Resolver( Address.class )
  public enum Lookup implements Function<String, Address> {
    INSTANCE;
    
    @Override
    public Address apply( final String input ) {
      Optional<Address> addressOptional = Optional.absent( );
      final Function<AddressI,String> addressExtractor =
          Strings.startsWith( Address.ID_PREFIX_ALLOC ).apply( input ) ?
              allocation( ) :
              Strings.startsWith( Address.ID_PREFIX_ASSOC ).apply( input ) ?
                association( ) :
                null;

      if ( addressExtractor != null ) {
        addressOptional = Iterables.tryFind(
            AddressRegistry.getInstance( ).listValues( ),
            CollectionUtils.propertyPredicate( input, addressExtractor )
        );
      } else try {
        addressOptional = Optional.of( AddressRegistry.getInstance( ).lookup( input ) );
      } catch ( NoSuchElementException e ) { /* throw appropriate exception below */ }
      if ( !addressOptional.isPresent( ) ) {
        throw new NoSuchElementException( "Address not found (" + input +")");
      }
      return addressOptional.get( );
    }
  }
  
  @QuantityMetricFunction( AddressMetadata.class )
  public enum CountAddresses implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @SuppressWarnings( "unchecked" )
    @Override
    public Long apply( final OwnerFullName input ) {
      return (long) Iterables.size( Iterables.filter(
          AddressRegistry.getInstance( ).listValues( ),
          Predicates.and(
              RestrictedTypes.filterByOwner( input ),
              Predicates.compose( Predicates.equalTo( Boolean.TRUE ), BooleanFilterFunctions.IS_ALLOCATED )
      ) ) );
    }
  }

  public static Function<AddressI,String> allocation( ) {
    return AllocatedAddressEntity.FilterFunctions.ALLOCATION_ID;
  }

  public static Function<AddressI,String> association( ) {
    return AllocatedAddressEntity.FilterFunctions.ASSOCIATION_ID;
  }

  public static class Allocator implements Supplier<Address>, Predicate<Address> {
    private final Addresses addresses;
    private final AddressDomain domain;

    private Allocator( final Addresses addresses, final AddressDomain domain ) {
      this.addresses = addresses;
      this.domain = domain;
    }

    @Override
    public Address get( ) {
      final Context ctx = Contexts.lookup( );
      try {
        return addresses.allocateNext( ctx.getUserFullName( ), domain );
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    @Override
    public boolean apply( final Address input ) {
      try {
        addresses.release( input, input.getAllocationId( ) );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
      return true;
    }
  }

  public static void updatePublicIpByInstanceId( final String instanceId,
                                                 final String publicIp ) {
    Entities.asTransaction( VmInstance.class, new Predicate<String>() {
      @Override
      public boolean apply( final String publicAddress ) {
        final VmInstance vm = VmInstances.lookup( instanceId );
        VmInstances.updatePublicAddress( vm, Objects.firstNonNull( publicAddress, VmNetworkConfig.DEFAULT_IP ) );
        return true;
      }
    } ).apply( publicIp );
  }
  
  @SuppressWarnings( "UnusedDeclaration" )
  public static class AddressAvailabilityEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register(ClockTick.class, new AddressAvailabilityEventListener());
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational() && Hosts.isCoordinator() ) {
        final List<Address> addresses = AddressRegistry.getInstance( ).listValues( );
        final List<Address> disabledAddresses = AddressRegistry.getInstance( ).listDisabledValues( );
        final long total = addresses.size() + disabledAddresses.size();
        final long available = Iterators.size( Iterators.filter( Iterators.concat( addresses.iterator(), disabledAddresses.iterator()), new Predicate<com.eucalyptus.address.Address>() {
          @Override
          public boolean apply( final Address address ) {
            return !address.isAllocated();
          }
        } ) );

        try {
          ListenerRegistry.getInstance( ).fireEvent( new ResourceAvailabilityEvent( ResourceType.Address, new ResourceAvailabilityEvent.Availability( total, available ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
  }

  @TypeMapper
  public enum AddressIToAddressInfoTypeTransform implements Function<AddressI,AddressInfoType> {
    INSTANCE;

    @Override
    public AddressInfoType apply( final AddressI address ) {
      final AddressInfoType addressInfoType = new AddressInfoType( );

      // allocation info
      addressInfoType.setPublicIp( address.getAddress( ) );
      addressInfoType.setDomain( Objects.firstNonNull( address.getDomain( ), AddressDomain.standard ).toString( ) );
      addressInfoType.setAllocationId( address.getAllocationId( ) );

      // association info
      if ( Strings.startsWith( "i-" ).apply( address.getInstanceId( ) ) ) {
        addressInfoType.setInstanceId( address.getInstanceId() );
      }
      addressInfoType.setAssociationId( address.getAssociationId( ) );
      addressInfoType.setNetworkInterfaceId( address.getNetworkInterfaceId( ) );
      addressInfoType.setNetworkInterfaceOwnerId( address.getNetworkInterfaceOwnerId( ) );
      addressInfoType.setPrivateIpAddress( address.getPrivateAddress( ) );

      return addressInfoType;
    }
  }

  private static abstract class AddressIFilterSupport<T extends AddressI> extends FilterSupport<T> {
    protected AddressIFilterSupport( @Nonnull final Builder<T> builder ) {
      super( properties( builder ) );
    }

    private static <TI extends AddressI> Builder<TI> properties( final Builder<TI> builder ) {
      return builder
          .withStringProperty( "allocation-id", ALLOCATION_ID )
          .withStringProperty( "association-id", ASSOCIATION_ID )
          .withStringProperty( "domain", DOMAIN )
          .withStringProperty( "instance-id", AllocatedAddressEntity.FilterFunctions.INSTANCE_ID )
          .withStringProperty( "network-interface-id", AllocatedAddressEntity.FilterFunctions.NETWORK_INTERFACE_ID )
          .withStringProperty( "network-interface-owner-id", AllocatedAddressEntity.FilterFunctions.NETWORK_INTERFACE_OWNER_ID )
          .withStringProperty( "private-ip-address", AllocatedAddressEntity.FilterFunctions.PRIVATE_IP_ADDRESS )
          .withStringProperty( "public-ip", AllocatedAddressEntity.FilterFunctions.PUBLIC_IP );
    }
  }

  public static class AddressFilterSupport extends AddressIFilterSupport<Address> {
    public AddressFilterSupport( ) {
      super( builderFor( Address.class ) );
    }
  }

  public static class AllocatedAddressEntityFilterSupport extends AddressIFilterSupport<AllocatedAddressEntity> {
    public AllocatedAddressEntityFilterSupport ( ) {
      super( builderFor( AllocatedAddressEntity.class )
          .withPersistenceFilter( "allocation-id", "allocationId" )
          .withPersistenceFilter( "association-id", "associationId" )
          .withPersistenceFilter( "domain", "domain", FUtils.valueOfFunction( AddressDomain.class ) )
          .withPersistenceFilter( "instance-id", "instanceId" )
          .withPersistenceFilter( "network-interface-id", "networkInterfaceId" )
          .withPersistenceFilter( "network-interface-owner-id", "networkInterfaceOwnerId" )
          .withPersistenceFilter( "private-ip-address", "privateAddress" )
          .withPersistenceFilter( "public-ip", "displayName" )
      );
    }
  }

  private enum BooleanFilterFunctions implements Function<Address,Boolean> {
    IS_ALLOCATED {
      @Nullable
      @Override
      public Boolean apply( @Nullable final Address address ) {
        return address != null && address.isAllocated( );
      }
    }
  }
}
