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

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet;
import java.util.Collections;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.address.AddressDomain;
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.vpc.NetworkInterfaceHelper;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.PublicAddresses;
import com.eucalyptus.records.Logs;
import com.eucalyptus.compute.common.internal.tags.Filters;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.eucalyptus.compute.common.backend.AllocateAddressResponseType;
import com.eucalyptus.compute.common.backend.AllocateAddressType;
import com.eucalyptus.compute.common.backend.AssociateAddressResponseType;
import com.eucalyptus.compute.common.backend.AssociateAddressType;
import com.eucalyptus.compute.common.backend.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.backend.DescribeAddressesType;
import com.eucalyptus.compute.common.backend.DisassociateAddressResponseType;
import com.eucalyptus.compute.common.backend.DisassociateAddressType;
import com.eucalyptus.compute.common.backend.ReleaseAddressResponseType;
import com.eucalyptus.compute.common.backend.ReleaseAddressType;

@ComponentNamed("computeAddressManager")
public class AddressManager {

  private static final Logger LOG = Logger.getLogger( AddressManager.class );

  private final InternetGateways internetGateways;
  private final Addresses addresses;

  @Inject
  public AddressManager(
      final AllocatedAddressPersistence allocatedAddressPersistence,
      final InternetGateways internetGateways
  ) {
    this.addresses = new Addresses( allocatedAddressPersistence );
    this.internetGateways = internetGateways;
  }

  public AllocateAddressResponseType allocateAddress( final AllocateAddressType request ) throws Exception {
    final AllocateAddressResponseType reply = request.getReply( );
    try {
      final String defaultVpcId = getDefaultVpcId( );
      final AddressDomain domain = Optional.fromNullable( request.getDomain( ) )
          .transform( FUtils.valueOfFunction( AddressDomain.class ) )
          .or( defaultVpcId != null ? AddressDomain.vpc : AddressDomain.standard );
      final Addresses.Allocator allocator = addresses.allocator( domain );
      final Address address = RestrictedTypes.allocateNamedUnitlessResources( 1, allocator, allocator ).get( 0 );
      reply.setPublicIp( address.getName( ) );
      reply.setAllocationId( address.getAllocationId( ) );
      reply.setDomain( domain.name( ) );
    } catch( final RuntimeException e ) {
      if( e.getCause( ) != null ) {
        throw new EucalyptusCloudException( e.getCause() );
      } else {
        throw new EucalyptusCloudException( "couldn't allocate addresses" );
      }
    }
    return reply;
  }

  public ReleaseAddressResponseType releaseAddress( final ReleaseAddressType request ) throws Exception {
    final ReleaseAddressResponseType reply = request.getReply( ).markFailed( );
    if ( request.getPublicIp( ) == null && request.getAllocationId( ) == null ) {
      throw new ClientComputeException( "MissingParameter", "PublicIp or AllocationId required" );
    }

    final String allocationId = ResourceIdentifiers.tryNormalize().apply( request.getAllocationId( ) );
    final Address address;
    try {
      address = RestrictedTypes.doPrivileged(
          Objects.firstNonNull( request.getPublicIp( ), allocationId ),
          Address.class );
    } catch ( NoSuchElementException e ) {
      if ( request.getAllocationId( ) != null ) {
        throw new ClientComputeException(
            "InvalidAddressID.NotFound",
            "Address not found for allocation ("+request.getAllocationId( )+")" );
      }
      throw e;
    }

    try {
      addresses.release(
          address,
          address.getDomain( ) == AddressDomain.vpc ? allocationId != null ? allocationId : address.getAllocationId( ) : null );
    } catch ( IllegalStateException e ) {
      throw new ClientComputeException(
          "InvalidIPAddress.InUse",
          "Address ("+address.getName( )+") in use ("+address.getNetworkInterfaceId( )+")" );
    }

    reply.set_return( true );
    return reply;
  }

  /**
   * NOTE: ComputeService#describeAddresses is used for non-verbose describe functionality.
   */
  public DescribeAddressesResponseType describeAddresses( final DescribeAddressesType request ) throws EucalyptusCloudException {
    final DescribeAddressesResponseType reply = request.getReply( );
    final Context ctx = Contexts.lookup( );
    final boolean isAdmin = ctx.isAdministrator( );
    final boolean verbose = isAdmin && request.getPublicIpsSet( ).remove( "verbose" ) ;
    final Predicate<? super Address> filter = CloudMetadatas.filteringFor( Address.class )
        .byId( request.getPublicIpsSet( ) )
        .byProperty( request.getAllocationIds( ), Addresses.allocation( ) )
        .byPredicate( Filters.generate( request.getFilterSet( ), Address.class ).asPredicate( ) )
        .byOwningAccount( verbose ?
            Collections.<String>emptyList( ) :
            Collections.singleton( ctx.getAccount( ).getAccountNumber( ) ) )
        .byPrivileges( )
        .buildPredicate( );
    for ( final Address address : Iterables.filter( addresses.listActiveAddresses( ), filter ) ) {
      reply.getAddressesSet( ).add( verbose
          ? address.getAdminDescription( )
          : TypeMappers.transform( address, AddressInfoType.class ) );
    }
    if ( verbose ) {
      for ( Address address : Iterables.filter( addresses.listInactiveAddresses( ), filter ) ) {
        reply.getAddressesSet( ).add( new AddressInfoType( address.getName( ), AddressDomain.standard.toString(), Principals.nobodyFullName( ).getUserName( ) ) );
      }
    }
    return reply;
  }

  public AssociateAddressResponseType associateAddress( final AssociateAddressType request ) throws Exception {
    final AssociateAddressResponseType reply = request.getReply( ).markFailed( );
    final String instanceId = request.getInstanceId( )==null ?
        null :
        normalizeInstanceIdentifier( request.getInstanceId( ) );
    final String networkInterfaceId = request.getNetworkInterfaceId()==null ?
        null :
        normalizeNetworkInterfaceIdentifier( request.getNetworkInterfaceId() );
    final Address address = RestrictedTypes.doPrivileged( Objects.firstNonNull(
            request.getPublicIp( ),
            ResourceIdentifiers.tryNormalize().apply( request.getAllocationId( ) ) ),
        Address.class );
    if ( !address.isAllocated( ) && !Contexts.lookup( ).isAdministrator( ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated: " + request.getPublicIp( ) );
    } else if ( !Contexts.lookup( ).isAdministrator( ) && !Contexts.lookup( ).getUserFullName( ).asAccountFullName( ).getAccountNumber( ).equals( address.getOwner( ).getAccountNumber( ) ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated to your account: " + request.getPublicIp( ) );
    }
    final VmInstance vm = instanceId == null ? null : RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
    try ( final Addresses.AddressingBatch batch = addresses.batch( ) ) {
      if ( address.getDomain( ) != AddressDomain.vpc ) { // EC2-Classic
        if ( vm == null ) {
          throw new ClientComputeException( "InvalidParameterCombination", "InstanceId must be specified when using PublicIp" );
        }
        if ( VmStateSet.NOT_RUNNING.apply( vm ) ) {
          throw new ClientComputeException( "InvalidInstanceID", "The instance '" + vm.getDisplayName( ) + "' is not in a valid state for this operation." );
        }
        final VmInstance oldVm = findCurrentAssignedVm( address );
        final Address oldAddr = findVmExistingAddress( addresses, vm );
        reply.set_return( true );

        if ( oldAddr != null && address.equals( oldAddr ) ) {
          return reply;
        }

        if ( address.isAssigned( ) ) { // clear current assignment for address
          if ( oldVm != null ) {
            PublicAddresses.markDirty( address.getDisplayName( ), oldVm.getPartition( ) );
          }
          addresses.unassign( address );
          if ( oldVm != null ) {
            addresses.system( oldVm );
          }
        }

        if ( oldAddr != null ) { // clear current address for vm assigning to
          PublicAddresses.markDirty( oldAddr.getDisplayName( ), vm.getPartition( ) );
          addresses.unassign( oldAddr );
        }

        if ( addresses.assign( address, vm ) ) {
          Addresses.updatePublicIpByInstanceId( vm.getInstanceId( ), address.getName( ) );
        }
      } else { // VPC
        final NetworkInterface networkInterface;
        try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
          if ( vm != null ) {
            if ( VmStateSet.EXPECTING_TEARDOWN.apply( vm ) || VmStateSet.DONE.apply( vm ) ) { // STOPPED is OK
              throw new ClientComputeException( "InvalidInstanceID", "The instance '" + vm.getDisplayName( ) + "' is not in a valid state for this operation." );
            }
            networkInterface = Iterables.getOnlyElement( Entities.merge( vm ).getNetworkInterfaces( ) );
          } else {
            if ( networkInterfaceId == null ) {
              throw new ClientComputeException( "MissingParameter", "Either instance ID or network interface id must be specified" );
            }
            networkInterface = RestrictedTypes.doPrivileged( networkInterfaceId, NetworkInterface.class );
            if ( networkInterface.isAttached( ) ) {
              final VmInstance attachedVm = networkInterface.getInstance( );
              if ( VmStateSet.EXPECTING_TEARDOWN.apply( attachedVm ) ) { // STOPPED is OK
                throw new ClientComputeException( "IncorrectInstanceState", "The instance to which '" + networkInterfaceId + "' is attached is not in a valid state for this operation" );
              }
            }
          }
        }
        reply.set_return( true );

        if ( !address.isAssigned( ) || !networkInterface.getDisplayName( ).equals( address.getNetworkInterfaceId( ) ) ) {
          if ( address.isAssigned( ) && !request.getAllowReassociation( ) ) {
            throw new ClientComputeException( "Resource.AlreadyAssociated", "Address already associated" );
          }

          if ( address.isAssigned( ) ) {
            final NetworkInterface oldNetworkInterface = RestrictedTypes.doPrivileged( address.getNetworkInterfaceId( ), NetworkInterface.class );
            try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
              final NetworkInterface eni = Entities.merge( oldNetworkInterface );
              addresses.unassign( address, null );
              eni.disassociate( );
              handleEniAddressUnassigned( addresses, address, eni );
              tx.commit( );
            }
          }

          try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
            final NetworkInterface eni = Entities.merge( networkInterface );
            internetGateways.lookupByVpc(
                null,
                eni.getVpc( ).getDisplayName( ),
                CloudMetadatas.toDisplayName( ) );

            if ( eni.isAssociated( ) ) {
              PublicAddresses.markDirty( eni.getAssociation( ).getPublicIp( ), eni.getPartition( ) );
              NetworkInterfaceHelper.releasePublic( eni );
              eni.disassociate( );
              if ( eni.isAttached( ) && eni.getAttachment( ).getDeviceIndex( ) == 0 ) {
                final VmInstance instance = eni.getInstance( );
                VmInstances.updatePublicAddress( instance, VmNetworkConfig.DEFAULT_IP );
              }
            }

            NetworkInterfaceHelper.associate( address, eni );

            tx.commit( );
          } catch ( final VpcMetadataNotFoundException e ) {
            throw new ClientComputeException( "Gateway.NotAttached", "Internet gateway not found for VPC" );
          }
        }
        reply.setAssociationId( address.getAssociationId( ) );
      }
    }

    return reply;
  }

  public DisassociateAddressResponseType disassociateAddress( final DisassociateAddressType request ) throws Exception {
    final DisassociateAddressResponseType reply = request.getReply( ).markFailed( );
    final Context ctx = Contexts.lookup( );
    final String associationId = ResourceIdentifiers.tryNormalize( ).apply( request.getAssociationId( ) );
    final Address address;
    try {
      address = RestrictedTypes.doPrivileged(
          Objects.firstNonNull( request.getPublicIp( ), associationId ),
          Address.class );
    } catch ( final NoSuchElementException e ) {
      if ( request.getAssociationId( ) != null ) {
        throw new ClientComputeException( "InvalidAssociationID.NotFound", "Association identifier (" + request.getAssociationId() + ") not found." );
      } else {
        throw e;
      }
    }

    reply.set_return( true );
    try ( final Addresses.AddressingBatch batch = addresses.batch( ) ) {
      if ( address.isSystemOwned( ) && !ctx.isAdministrator( ) ) {
        throw new EucalyptusCloudException( "Only administrators can unassign system owned addresses: " + address.toString( ) );
      } else if ( address.getDomain( ) != AddressDomain.vpc ) { // EC2-Classic (allow for null domain)
        final String vmId = address.getInstanceId( );
        try {
          addresses.unassign( address );
          final VmInstance instance = VmInstances.lookup( vmId );
          PublicAddresses.markDirty( address.getDisplayName( ), instance.getPartition( ) );
          if ( address.getAddress( ).equals( instance.getPublicAddress( ) ) ) {
            Addresses.updatePublicIpByInstanceId( instance.getDisplayName( ), null );
            try {
              addresses.system( instance );
            } catch ( NoSuchElementException e ) {
              LOG.debug( e, e );
            } catch ( Exception e ) {
              LOG.error( "Error assigning system address for instance " + vmId, e );
            }
          }
        } catch ( Exception e ) {
          LOG.debug( e );
          Logs.extreme( ).debug( e, e );
        }
      } else { // VPC
        final NetworkInterface networkInterface = address.getNetworkInterfaceId( ) == null ?
            null :
            RestrictedTypes.doPrivileged( address.getNetworkInterfaceId( ), NetworkInterface.class );
        if ( networkInterface != null ) {
          if ( NetworkInterface.Type.NatGateway == networkInterface.getType( ) ) {
            throw new ClientComputeException(
                "InvalidIPAddress.InUse",
                "Address ("+address.getName( )+") in use for NAT gateway interface ("+address.getNetworkInterfaceId( )+")" );
          }
          try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
            final NetworkInterface eni = Entities.merge( networkInterface );
            if ( addresses.unassign( address, associationId ) ) {
              eni.disassociate( );
              handleEniAddressUnassigned( addresses, address, eni );
              tx.commit( );
            }
          }
        }
      }
    }
    return reply;
  }

  private static Address findVmExistingAddress( final Addresses addresses, final VmInstance vm ) {
    Address oldAddr = null;
    if ( vm.hasPublicAddress( ) ) {
      try {
        oldAddr = addresses.lookupActiveAddress( vm.getPublicAddress( ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    return oldAddr;
  }

  private static VmInstance findCurrentAssignedVm( Address address ) {
    VmInstance oldVm = null;
    if ( address.isAssigned( ) ) {
      try {
        oldVm = VmInstances.lookup( address.getInstanceId( ) );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
    return oldVm;
  }

  private static String getDefaultVpcId( ) {
    return getDefaultVpcId( Contexts.lookup( ).getUserFullName( ).asAccountFullName( ) );
  }

  private static String getDefaultVpcId( final AccountFullName accountFullName ) {
    try ( final TransactionResource tx = Entities.transactionFor( Vpc.class ) ) {
      return Iterables.tryFind(
          Entities.query( Vpc.exampleDefault( accountFullName ) ),
          Predicates.alwaysTrue()
      ).transform( CloudMetadatas.toDisplayName() ).orNull( );
    }
  }

  /**
   * Caller must have open transaction for eni
   */
  private static void handleEniAddressUnassigned( final Addresses addresses,
                                                  final Address address,
                                                  final NetworkInterface eni ) {

    if ( eni.isAttached( ) ) {
      PublicAddresses.markDirty( address.getAddress( ), eni.getPartition( ) );

      if ( eni.getAttachment( ).getDeviceIndex( ) == 0 ) {
        final VmInstance vm = eni.getInstance( );
        VmInstances.updatePublicAddress( vm, VmNetworkConfig.DEFAULT_IP );
        if ( !vm.isUsePrivateAddressing( ) &&
            ( VmInstance.VmState.PENDING.equals( vm.getState( ) ) || VmInstance.VmState.RUNNING.equals( vm.getState( ) ) ) ) {
          try {
            NetworkInterfaceHelper.associate( addresses.allocateSystemAddress( ), eni );
          } catch ( final NotEnoughResourcesException e ) {
            LOG.warn( "No addresses available, not assigning system address for: " + vm.getDisplayName( )
                + " : " + e.getMessage( ) );
          }
        }
      }
    }
  }

  private static String normalizeIdentifier( final String identifier,
                                             final String prefix,
                                             final boolean required,
                                             final String message ) throws ClientComputeException {
    try {
      return Strings.emptyToNull( identifier ) == null && !required ?
          null :
          ResourceIdentifiers.parse( prefix, identifier ).getIdentifier( );
    } catch ( final InvalidResourceIdentifier e ) {
      throw new ClientComputeException( "InvalidParameterValue", String.format( message, e.getIdentifier( ) ) );
    }
  }

  private static String normalizeInstanceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, VmInstance.ID_PREFIX, true, "Value (%s) for parameter instanceId is invalid. Expected: 'i-...'." );
  }

  private static String normalizeNetworkInterfaceIdentifier( final String identifier ) throws EucalyptusCloudException {
    return normalizeIdentifier(
        identifier, "eni", true, "Value (%s) for parameter networkInterface is invalid. Expected: 'eni-...'." );
  }
}
