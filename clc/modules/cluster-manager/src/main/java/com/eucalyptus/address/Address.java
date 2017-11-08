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

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.type.RestrictedType.AccountRestrictedType;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.internal.address.AddressDomain;
import com.eucalyptus.compute.common.internal.address.AddressI;
import com.eucalyptus.compute.common.internal.address.AddressState;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class Address implements AccountRestrictedType, AddressI, AddressMetadata, HasFullName<AddressMetadata> {

  public static final String ID_PREFIX_ALLOC = "eipalloc";
  public static final String ID_PREFIX_ASSOC = "eipassoc";

  public interface AddressStateMetadataWithInstanceInfo {
    String getInstanceId( );
    String getInstanceUuid( );
    String getPrivateAddress( );
  }

  public interface AddressStateMetadataWithNetworkInterfaceInfo {
    String getNetworkInterfaceId( );
    String getNetworkInterfaceOwnerId( );
    String getPrivateAddress( );
  }

  public interface AddressStateMetadataWithAllocationId {
    String getOwnerAccountNumber( );
    String getOwnerUserId( );
    String getOwnerUserName( );
    String getAllocationId( );
  }

  public interface AddressStateMetadataWithAssociationId extends AddressStateMetadataWithAllocationId {
    String getAssociationId( );
  }

  public interface AddressStateMetadataWithDomain {
    AddressDomain getDomain( );
  }

  public static abstract class AddressStateMetadataSupport {
    private final long version;
    private final AddressState state;

    protected AddressStateMetadataSupport(
        final AddressState state,
        final long version
    ) {
      this.state = state;
      this.version = version;
    }
  }

  public static abstract class OwnedAddressStateMetadataSupport extends AddressStateMetadataSupport {
    private final String ownerAccountNumber;
    private final String ownerUserId;
    private final String ownerUserName;

    protected OwnedAddressStateMetadataSupport(
        final AddressState state,
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName
    ) {
      super( state, version );
      this.ownerAccountNumber = ownerAccountNumber;
      this.ownerUserId = ownerUserId;
      this.ownerUserName = ownerUserName;
    }

    public String getOwnerAccountNumber( ) {
      return ownerAccountNumber;
    }

    public String getOwnerUserId( ) {
      return ownerUserId;
    }

    public String getOwnerUserName( ) {
      return ownerUserName;
    }
  }

  public static final class AvailableAddressMetadata extends AddressStateMetadataSupport {
    public AvailableAddressMetadata( final long version ) {
      super( AddressState.unallocated, version );
    }
  }

  public static final class ImpendingSystemAddressMetadata extends AddressStateMetadataSupport {
    public ImpendingSystemAddressMetadata( final long version ) {
      super( AddressState.impending, version );
    }
  }

  public static final class AssignedClassicSystemAddressMetadata extends AddressStateMetadataSupport implements AddressStateMetadataWithInstanceInfo {
    private final String instanceId;
    private final String instanceUuid;
    private final String privateAddress;

    public AssignedClassicSystemAddressMetadata(
        final long version,
        final String instanceId,
        final String instanceUuid,
        final String privateAddress
    ) {
      super( AddressState.assigned, version );
      this.instanceId = instanceId;
      this.instanceUuid = instanceUuid;
      this.privateAddress = privateAddress;
    }

    @Override
    public String getInstanceId( ) {
      return instanceId;
    }

    @Override
    public String getInstanceUuid( ) {
      return instanceUuid;
    }

    @Override
    public String getPrivateAddress( ) {
      return privateAddress;
    }
  }

  public static final class AssignedVpcSystemAddressMetadata extends AddressStateMetadataSupport implements AddressStateMetadataWithNetworkInterfaceInfo {
    private final String networkInterfaceId;
    private final String networkInterfaceOwnerId;
    private final String privateAddress;

    public AssignedVpcSystemAddressMetadata(
        final long version,
        final String networkInterfaceId,
        final String networkInterfaceOwnerId,
        final String privateAddress
    ) {
      super( AddressState.assigned, version );
      this.networkInterfaceId = networkInterfaceId;
      this.networkInterfaceOwnerId = networkInterfaceOwnerId;
      this.privateAddress = privateAddress;
    }

    @Override
    public String getNetworkInterfaceId( ) {
      return networkInterfaceId;
    }

    @Override
    public String getNetworkInterfaceOwnerId( ) {
      return networkInterfaceOwnerId;
    }

    @Override
    public String getPrivateAddress( ) {
      return privateAddress;
    }
  }

  public static final class AllocatedClassicAddressMetadata extends OwnedAddressStateMetadataSupport implements AddressStateMetadataWithDomain {
    protected AllocatedClassicAddressMetadata(
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName
    ) {
      super( AddressState.allocated, version, ownerAccountNumber, ownerUserId, ownerUserName );
    }

    @Override
    public AddressDomain getDomain() {
      return AddressDomain.standard;
    }
  }

  public static final class AllocatedVpcAddressMetadata extends OwnedAddressStateMetadataSupport implements AddressStateMetadataWithAllocationId, AddressStateMetadataWithDomain {
    private final String allocationId;

    protected AllocatedVpcAddressMetadata(
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName,
        final String allocationId
    ) {
      super( AddressState.allocated, version, ownerAccountNumber, ownerUserId, ownerUserName );
      this.allocationId = allocationId;
    }

    @Override
    public AddressDomain getDomain() {
      return AddressDomain.vpc;
    }

    @Override
    public String getAllocationId( ) {
      return allocationId;
    }
  }

  public static final class AssociatedClassicAddressMetadata extends OwnedAddressStateMetadataSupport implements AddressStateMetadataWithInstanceInfo, AddressStateMetadataWithDomain {
    private final String instanceId;
    private final String instanceUuid;
    private final String privateAddress;

    public AssociatedClassicAddressMetadata(
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName,
        final String instanceId,
        final String instanceUuid,
        final String privateAddress
    ) {
      super( AddressState.assigned, version, ownerAccountNumber, ownerUserId, ownerUserName );
      this.instanceId = instanceId;
      this.instanceUuid = instanceUuid;
      this.privateAddress = privateAddress;
    }

    @Override
    public AddressDomain getDomain() {
      return AddressDomain.standard;
    }

    @Override
    public String getInstanceId( ) {
      return instanceId;
    }

    @Override
    public String getInstanceUuid( ) {
      return instanceUuid;
    }

    @Override
    public String getPrivateAddress( ) {
      return privateAddress;
    }
  }

  public static final class AssociatedVpcAddressMetadata extends OwnedAddressStateMetadataSupport implements AddressStateMetadataWithAssociationId, AddressStateMetadataWithDomain, AddressStateMetadataWithNetworkInterfaceInfo {
    private final String allocationId;
    private final String associationId;
    private final String networkInterfaceId;
    private final String networkInterfaceOwnerId;
    private final String privateAddress;

    public AssociatedVpcAddressMetadata(
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName,
        final String allocationId,
        final String associationId,
        final String networkInterfaceId,
        final String networkInterfaceOwnerId,
        final String privateAddress
    ) {
      super( AddressState.assigned, version, ownerAccountNumber, ownerUserId, ownerUserName );
      this.allocationId = allocationId;
      this.associationId = associationId;
      this.networkInterfaceId = networkInterfaceId;
      this.networkInterfaceOwnerId = networkInterfaceOwnerId;
      this.privateAddress = privateAddress;
    }

    @Override
    public AddressDomain getDomain() {
      return AddressDomain.vpc;
    }

    @Override
    public String getAllocationId( ) {
      return allocationId;
    }

    @Override
    public String getAssociationId( ) {
      return associationId;
    }

    @Override
    public String getNetworkInterfaceId( ) {
      return networkInterfaceId;
    }

    @Override
    public String getNetworkInterfaceOwnerId( ) {
      return networkInterfaceOwnerId;
    }

    @Override
    public String getPrivateAddress( ) {
      return privateAddress;
    }
  }

  public static final class ActiveVpcAddressMetadata extends OwnedAddressStateMetadataSupport implements AddressStateMetadataWithAssociationId, AddressStateMetadataWithDomain, AddressStateMetadataWithInstanceInfo, AddressStateMetadataWithNetworkInterfaceInfo {
    private final String allocationId;
    private final String associationId;
    private final String networkInterfaceId;
    private final String networkInterfaceOwnerId;
    private final String privateAddress;
    private final String instanceId;
    private final String instanceUuid;

    public ActiveVpcAddressMetadata(
        final long version,
        final String ownerAccountNumber,
        final String ownerUserId,
        final String ownerUserName,
        final String allocationId,
        final String associationId,
        final String networkInterfaceId,
        final String networkInterfaceOwnerId,
        final String privateAddress,
        final String instanceId,
        final String instanceUuid
    ) {
      super( AddressState.started, version, ownerAccountNumber, ownerUserId, ownerUserName );
      this.allocationId = allocationId;
      this.associationId = associationId;
      this.networkInterfaceId = networkInterfaceId;
      this.networkInterfaceOwnerId = networkInterfaceOwnerId;
      this.privateAddress = privateAddress;
      this.instanceId = instanceId;
      this.instanceUuid = instanceUuid;
    }

    @Override
    public AddressDomain getDomain() {
      return AddressDomain.vpc;
    }

    @Override
    public String getAllocationId( ) {
      return allocationId;
    }

    @Override
    public String getAssociationId( ) {
      return associationId;
    }

    @Override
    public String getInstanceId( ) {
      return instanceId;
    }

    @Override
    public String getInstanceUuid( ) {
      return instanceUuid;
    }


    @Override
    public String getNetworkInterfaceId( ) {
      return networkInterfaceId;
    }

    @Override
    public String getNetworkInterfaceOwnerId( ) {
      return networkInterfaceOwnerId;
    }

    @Override
    public String getPrivateAddress() {
      return privateAddress;
    }
  }

  public interface AddressInfo {
    String getAddress( );
    long getStateVersion( );
    AddressState getState( );
    boolean isAllocated( );
    boolean isAssigned( );
    boolean isReallyAssigned( );
    boolean isStarted( );
    @Nullable String getOwnerAccountNumber( );
    @Nullable String getOwnerUserId( );
    @Nullable String getOwnerUserName( );
    @Nullable String getInstanceId( );
    @Nullable String getInstanceUuid( );
    @Nullable String getInstanceAddress( );
    @Nullable AddressDomain getDomain( );
    @Nullable String getAllocationId( );
    @Nullable String getAssociationId( );
    @Nullable String getNetworkInterfaceId( );
    @Nullable String getNetworkInterfaceOwnerId( );
    @Nullable String getPrivateAddress( );
  }

  private static final class StateAddressInfo implements AddressInfo {
    private final String address;
    private final Supplier<AddressStateMetadataSupport> stateSupplier;

    private StateAddressInfo(
        final String address,
        final Supplier<AddressStateMetadataSupport> stateSupplier
    ) {
      this.address = address;
      this.stateSupplier = stateSupplier;
    }

    @Override
    public String getAddress( ) {
      return address;
    }

    @Override
    public long getStateVersion( ) {
      return stateSupplier.get( ).version;
    }

    @Override
    public AddressState getState( ) {
      return stateSupplier.get( ).state;
    }

    @Override
    public boolean isAllocated( ) {
      return stateSupplier.get( ).state.ordinal( ) > AddressState.unallocated.ordinal( );
    }

    @Override
    public boolean isAssigned( ) {
      return stateSupplier.get( ).state.ordinal( ) > AddressState.allocated.ordinal( );
    }

    @Override
    public boolean isReallyAssigned( ) {
      return stateSupplier.get( ).state.ordinal( ) > AddressState.impending.ordinal( );
    }

    @Override
    public boolean isStarted( ) {
      return stateSupplier.get( ).state.ordinal( ) > AddressState.assigned.ordinal( );
    }

    @Override
    public String getOwnerAccountNumber( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof OwnedAddressStateMetadataSupport ) {
        return ( (OwnedAddressStateMetadataSupport) stateMetadata ).ownerAccountNumber;
      }
      return null;
    }

    @Override
    public String getOwnerUserId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof OwnedAddressStateMetadataSupport ) {
        return ( (OwnedAddressStateMetadataSupport) stateMetadata ).ownerUserId;
      }
      return null;
    }

    @Override
    public String getOwnerUserName() {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof OwnedAddressStateMetadataSupport ) {
        return ( (OwnedAddressStateMetadataSupport) stateMetadata ).ownerUserName;
      }
      return null;
    }

    @Override
    public String getInstanceId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithInstanceInfo ) {
        return ( (AddressStateMetadataWithInstanceInfo) stateMetadata ).getInstanceId( );
      }
      return null;
    }

    @Override
    public String getInstanceUuid( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithInstanceInfo ) {
        return ( (AddressStateMetadataWithInstanceInfo) stateMetadata ).getInstanceUuid( );
      }
      return null;
    }

    @Override
    public String getInstanceAddress( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithInstanceInfo ) {
        return ( (AddressStateMetadataWithInstanceInfo) stateMetadata ).getPrivateAddress( );
      }
      return null;
    }

    @Override
    public AddressDomain getDomain( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithDomain ) {
        return ( (AddressStateMetadataWithDomain) stateMetadata ).getDomain( );
      }
      return null;
    }

    @Override
    public String getAllocationId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithAllocationId ) {
        return ( (AddressStateMetadataWithAllocationId) stateMetadata ).getAllocationId( );
      }
      return null;
    }

    @Override
    public String getAssociationId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithAssociationId ) {
        return ( (AddressStateMetadataWithAssociationId) stateMetadata ).getAssociationId( );
      }
      return null;
    }

    @Override
    public String getNetworkInterfaceId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithNetworkInterfaceInfo ) {
        return ( (AddressStateMetadataWithNetworkInterfaceInfo) stateMetadata ).getNetworkInterfaceId( );
      }
      return null;
    }

    @Override
    public String getNetworkInterfaceOwnerId( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithNetworkInterfaceInfo ) {
        return ( (AddressStateMetadataWithNetworkInterfaceInfo) stateMetadata ).getNetworkInterfaceOwnerId( );
      }
      return null;
    }

    @Override
    public String getPrivateAddress( ) {
      final AddressStateMetadataSupport stateMetadata = stateSupplier.get( );
      if ( stateMetadata instanceof AddressStateMetadataWithNetworkInterfaceInfo ) {
        return ( (AddressStateMetadataWithNetworkInterfaceInfo) stateMetadata ).getPrivateAddress( );
      }
      return null;
    }
  }

  private final String address;
  private final AtomicReference<AddressStateMetadataSupport> stateMetadata;
  private final AtomicReference<UserFullName> cachedUser = new AtomicReference<>( );
  private final AddressInfo addressInfo;

  public Address( final String address ) {
    this.address = address;
    this.stateMetadata =
        new AtomicReference<AddressStateMetadataSupport>( new AvailableAddressMetadata( 0 ) );
    this.addressInfo = new StateAddressInfo( address, new Supplier<AddressStateMetadataSupport>(){
      @Override
      public AddressStateMetadataSupport get() {
        return stateMetadata.get( );
      }
    } );
  }

  public String getAddress( ) {
    return address;
  }

  @Override
  public String getName( ) {
    return getAddress( );
  }

  @Override
  public String getDisplayName( ) {
    return getAddress( );
  }

  @Override
  public OwnerFullName getOwner( ) {
    final AddressStateMetadataSupport metadata = stateMetadata.get( );
    final UserFullName owner = cachedUser.get( );
    if ( owner != null &&
        metadata instanceof OwnedAddressStateMetadataSupport &&
        ((OwnedAddressStateMetadataSupport)metadata).ownerUserId.equals( owner.getUserId( ) ) ) {
      return owner;
    } else if ( metadata instanceof OwnedAddressStateMetadataSupport ) {
      final UserFullName newOwner = UserFullName.getInstanceForAccount(
          ((OwnedAddressStateMetadataSupport)metadata).ownerAccountNumber,
          ((OwnedAddressStateMetadataSupport)metadata).ownerUserId
      );
      cachedUser.set( newOwner );
      return newOwner;
    } else {
      return metadata.state.ordinal( ) > AddressState.unallocated .ordinal( ) ?
          Principals.systemFullName( ) :
          Principals.nobodyFullName( );
    }
  }

  public static final class AddressStateTransition {
    private final String address;
    private final AtomicReference<AddressStateMetadataSupport> stateMetadata;
    private final AddressStateMetadataSupport oldState;
    private final AddressStateMetadataSupport newState;

    public AddressStateTransition(
        final String address,
        final AtomicReference<AddressStateMetadataSupport> stateMetadata,
        final AddressStateMetadataSupport oldState,
        final AddressStateMetadataSupport newState
    ) {
      this.address = address;
      this.stateMetadata = stateMetadata;
      this.oldState = oldState;
      this.newState = newState;
    }

    public AddressInfo oldAddressInfo( ) {
      return new StateAddressInfo( address, Suppliers.ofInstance( oldState ) );
    }

    public AddressInfo newAddressInfo( ) {
      return new StateAddressInfo( address, Suppliers.ofInstance( newState ) );
    }

    /**
     * True if the state change was reverted.
     *
     * If false is returned the state transition is stale and no other
     * changes should be made for rollback.
     */
    public boolean rollback( ) {
      return stateMetadata.compareAndSet( newState, oldState );
    }
  }

  private Optional<AddressStateTransition> transition(
      final AddressStateMetadataSupport oldState,
      final AddressStateMetadataSupport newState
  ) {
    if ( stateMetadata.compareAndSet( oldState, newState ) ) {
      return Optional.of( new AddressStateTransition( getAddress( ), stateMetadata, oldState, newState ) );
    } else {
      return Optional.absent( );
    }
  }

  Optional<AddressStateTransition> allocate( final UserFullName ownerFullName, final AddressDomain domain ) {
    return allocate(
        ownerFullName.getAccountNumber( ),
        ownerFullName.getUserId( ),
        ownerFullName.getUserName( ),
        domain,
        ResourceIdentifiers.generateString( ID_PREFIX_ALLOC )
    );
  }

  Optional<AddressStateTransition> allocate(
      final String ownerAccountNumber,
      final String ownerUserId,
      final String ownerUserName,
      @Nonnull final AddressDomain domain,
      final String allocationId
  ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState.state == AddressState.unallocated ) {
      switch( domain ) {
        case standard:
          return transition( initialState, new AllocatedClassicAddressMetadata(
              initialState.version + 1,
              ownerAccountNumber,
              ownerUserId,
              ownerUserName ) );
        case vpc:
          return transition( initialState, new AllocatedVpcAddressMetadata(
              initialState.version + 1,
              ownerAccountNumber,
              ownerUserId,
              ownerUserName,
              allocationId ) );
      }
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> pendingAssignment( ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState.state == AddressState.unallocated ) {
      return transition( initialState, new ImpendingSystemAddressMetadata( initialState.version + 1 ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> assign( @Nonnull final VmInstance vm ) {
    if ( vm.getVpcId( ) != null ) throw new IllegalArgumentException( "Cannot assign address to VPC instance" );
    return assignClassic(
        vm.getInstanceId( ),
        vm.getInstanceUuid( ),
        vm.getPrivateAddress( )
    );
  }

  Optional<AddressStateTransition> assignClassic(
      @Nonnull final String instanceId,
      @Nonnull final String instanceUuid,
      @Nonnull final String privateAddress
  ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof AllocatedClassicAddressMetadata ) { // Elastic IP assign
      final AllocatedClassicAddressMetadata allocated = (AllocatedClassicAddressMetadata) initialState;
      return transition( initialState, new AssociatedClassicAddressMetadata(
          initialState.version + 1,
          allocated.getOwnerAccountNumber( ),
          allocated.getOwnerUserId( ),
          allocated.getOwnerUserName( ),
          instanceId,
          instanceUuid,
          privateAddress
      ) );
    } else if ( initialState.state.ordinal( ) < AddressState.assigned.ordinal( )  ) { // Public IP assign
      return transition( initialState, new AssignedClassicSystemAddressMetadata(
          initialState.version + 1,
          instanceId,
          instanceUuid,
          privateAddress
      ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> unassign( ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof AssociatedClassicAddressMetadata ) { // Elastic IP unassign
      final AssociatedClassicAddressMetadata associated = (AssociatedClassicAddressMetadata) initialState;
      return transition( initialState, new AllocatedClassicAddressMetadata(
          initialState.version + 1,
          associated.getOwnerAccountNumber( ),
          associated.getOwnerUserId( ),
          associated.getOwnerUserName( )
      ) );
    } else if (initialState instanceof AssignedClassicSystemAddressMetadata  ) { // Public IP unassign
      return transition( initialState, new AvailableAddressMetadata( initialState.version + 1 ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> release( final String allocationId ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState.state.ordinal( ) > AddressState.unallocated.ordinal( ) ) {
      boolean release = false;
      if ( allocationId != null ) {
        if ( initialState.state.ordinal( ) > AddressState.allocated.ordinal( ) ) {
          throw new IllegalStateException( "Not releasing address in state " + initialState.state );
        }
        if ( initialState instanceof AddressStateMetadataWithAllocationId ) {
          final AddressStateMetadataWithAllocationId allocatedState =
              (AddressStateMetadataWithAllocationId) initialState;
          release = allocationId.equals( allocatedState.getAllocationId( ) );
        }
      } else {
        release = true;
      }
      if ( release ) {
        return transition( initialState, new AvailableAddressMetadata( initialState.version + 1 ) );
      }
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> assign( @Nonnull final NetworkInterface networkInterface ) {
    return assignVpc(
        networkInterface.getDisplayName( ),
        networkInterface.getOwnerUserId( ),
        networkInterface.getPrivateIpAddress( ),
        ResourceIdentifiers.generateString( ID_PREFIX_ASSOC )
    );
  }

  Optional<AddressStateTransition> assignVpc(
      @Nonnull String networkInterfaceId,
      @Nonnull String networkInterfaceOwnerUserId,
      @Nonnull String privateAddress,
      @Nonnull String associationId
  ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof AllocatedVpcAddressMetadata ) { // Elastic IP assign
      final AllocatedVpcAddressMetadata allocated = (AllocatedVpcAddressMetadata) initialState;
      return transition( initialState, new AssociatedVpcAddressMetadata(
          initialState.version + 1,
          allocated.getOwnerAccountNumber( ),
          allocated.getOwnerUserId( ),
          allocated.getOwnerUserName( ),
          allocated.getAllocationId( ),
          associationId,
          networkInterfaceId,
          networkInterfaceOwnerUserId,
          privateAddress
      ) );
    } else if ( initialState.state.ordinal( ) < AddressState.assigned.ordinal( )  ) { // Public IP assign
      return transition( initialState, new AssignedVpcSystemAddressMetadata(
          initialState.version + 1,
          networkInterfaceId,
          networkInterfaceOwnerUserId,
          privateAddress
      ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> unassign( @Nullable final String associationId ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof AddressStateMetadataWithAssociationId ) { // Elastic IP unassign
      final AddressStateMetadataWithAssociationId associated = (AddressStateMetadataWithAssociationId) initialState;
      if ( associationId == null || associationId.equals( associated.getAssociationId( ) ) ) {
        return transition( initialState, new AllocatedVpcAddressMetadata(
            initialState.version + 1,
            associated.getOwnerAccountNumber( ),
            associated.getOwnerUserId( ),
            associated.getOwnerUserName( ),
            associated.getAllocationId( )
        ) );
      }
    } else if (initialState instanceof AssignedVpcSystemAddressMetadata  ) { // Public IP unassign
      return transition( initialState, new AvailableAddressMetadata( initialState.version + 1 ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> start( @Nonnull final VmInstance vm  ) {
    return start(
        vm.getInstanceId( ),
        vm.getInstanceUuid( )
    );
  }

  Optional<AddressStateTransition> start(
      @Nonnull final String instanceId,
      @Nonnull final String instanceUuid
  ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof AssociatedVpcAddressMetadata ) {
      final AssociatedVpcAddressMetadata assigned = (AssociatedVpcAddressMetadata) initialState;
      return transition( initialState, new ActiveVpcAddressMetadata(
          initialState.version + 1,
          assigned.getOwnerAccountNumber( ),
          assigned.getOwnerUserId( ),
          assigned.getOwnerUserName( ),
          assigned.getAllocationId( ),
          assigned.getAssociationId( ),
          assigned.getNetworkInterfaceId( ),
          assigned.getNetworkInterfaceOwnerId( ),
          assigned.getPrivateAddress( ),
          instanceId,
          instanceUuid
      ) );
    }
    return Optional.absent( );
  }

  Optional<AddressStateTransition> stop( ) {
    final AddressStateMetadataSupport initialState = stateMetadata.get( );
    if ( initialState instanceof ActiveVpcAddressMetadata ) {
      final ActiveVpcAddressMetadata active = (ActiveVpcAddressMetadata) initialState;
      return transition( initialState, new AssociatedVpcAddressMetadata(
          initialState.version + 1,
          active.getOwnerAccountNumber( ),
          active.getOwnerUserId( ),
          active.getOwnerUserName( ),
          active.getAllocationId( ),
          active.getAssociationId( ),
          active.getNetworkInterfaceId( ),
          active.getNetworkInterfaceOwnerId( ),
          active.getPrivateAddress( )
      ) );
    }
    return Optional.absent( );
  }

  public boolean isAllocated( ) {
    return addressInfo.isAllocated( );
  }
  
  public boolean isSystemOwned( ) {
    return Principals.systemFullName( ).equals( this.getOwner( ) );
  }

  /**
   * Is the instance assigned or with an impending assignment.
   *
   * <P>WARNING! in this state the instance ID may not be a valid instance
   * identifier.</P>
   *
   * @return True if assigned or assignment impending.
   * @see #getInstanceId()
   * @see #getInstanceUuid()
   */
  public boolean isAssigned( ) {
    return addressInfo.isAssigned( );
  }

  /**
   * Is it really assigned? Really?
   *
   * <P>In this state the instance ID and UUID are expected to be valid.</P>
   *
   * @return True if assigned to an instance.
   * @see #getInstanceId()
   * @see #getInstanceUuid()
   */
  public boolean isReallyAssigned( ) {
    return addressInfo.isReallyAssigned( );
  }

  public boolean isStarted( ) {
    return addressInfo.isStarted( );
  }

  @Override
  public String getOwnerAccountNumber( ) {
    return addressInfo.getOwnerAccountNumber( );
  }

  /**
   * Get the instance ID for the instance using this address.
   *
   * <P>The instance ID is only a valid identifier when the address is
   * assigned. In other states the value describes the state of the address
   * (e.g. "available" or "pending") </P>
   *
   * @return The instance ID
   * @see #isReallyAssigned()
   */
  public String getInstanceId( ) {
    return addressInfo.getInstanceId( );
  }

  /**
   * Get the instance UUID for the instance using this address.
   *
   * <P>The instance ID is only a valid identifier when the address is
   * assigned.</P>
   *
   * @return The instance UUID
   * @see #isReallyAssigned()
   */
  public String getInstanceUuid( ) {
    return addressInfo.getInstanceUuid( );
  }

  public String getUserId( ) {
    return this.getOwner( ).getUserId( );
  }
  
  public String getInstanceAddress( ) {
    return addressInfo.getInstanceAddress( );
  }
  
  @Nullable
  public AddressDomain getDomain( ) {
    return addressInfo.getDomain( );
  }

  @Nullable
  public String getAllocationId( ) {
    return addressInfo.getAllocationId( );
  }

  @Nullable
  public String getAssociationId( ) {
    return addressInfo.getAssociationId( );
  }

  @Nullable
  public String getNetworkInterfaceId( ) {
    return addressInfo.getNetworkInterfaceId( );
  }

  @Nullable
  public String getNetworkInterfaceOwnerId( ) {
    return addressInfo.getNetworkInterfaceOwnerId( );
  }

  @Nullable
  public String getPrivateAddress( ) {
    return addressInfo.getPrivateAddress( );
  }

  public long getStateVersion( ) {
    return addressInfo.getStateVersion( );
  }

  public AddressState getState( ) {
    return addressInfo.getState( );
  }

  @Override
  public String toString( ) {
    return "Address " + this.getDisplayName( ) + " " + ( this.isAllocated( )
      ? this.getOwner( ) + " "
      : "" ) + ( this.isAssigned( )
      ? this.getInstanceId() + " " + this.getInstanceAddress() + " "
      : "" );
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;
    Address address = ( Address ) o;
    if ( !this.getDisplayName( ).equals( address.getDisplayName( ) ) ) return false;
    return true;
  }
  
  @Override
  public int hashCode( ) {
    return this.getDisplayName( ).hashCode( );
  }
  
  public AddressInfoType getAdminDescription( ) {
    final AddressInfoType addressInfoType = TypeMappers.transform( this, AddressInfoType.class );
    final String desc = String.format( "%s (%s)", Strings.nullToEmpty( this.getInstanceId( ) ), this.getOwner() );
    addressInfoType.setInstanceId( desc );
    return addressInfoType;
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" ).region( ComponentIds.lookup( ClusterController.class ).name( ) ).namespace( this.getPartition( ) ).relativeId( "public-address",
                                                                                                                                                           this.getName( ) );
  }

  @Override
  public int compareTo( @Nonnull AddressMetadata that ) {
    return this.getDisplayName( ).compareTo( that.getDisplayName( ) );
  }

  /**
   * @see HasFullName#getPartition()
   */
  @Override
  public String getPartition( ) {
    return "eucalyptus";
  }
}
