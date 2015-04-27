/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
import com.eucalyptus.compute.common.internal.identifier.InvalidResourceIdentifier;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.vpc.InternetGateways;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.vpc.NetworkInterfaceHelper;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.compute.common.internal.tags.Filters;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.UnconditionalCallback;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.google.common.base.Enums;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
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

@ComponentNamed
public class AddressManager {
  
  private static final Logger LOG = Logger.getLogger( AddressManager.class );

  private final InternetGateways internetGateways;

  @Inject
  public AddressManager( final InternetGateways internetGateways ) {
    this.internetGateways = internetGateways;
  }
  
  @SuppressWarnings( "UnusedDeclaration" )
  public AllocateAddressResponseType allocateAddress( final AllocateAddressType request ) throws Exception {
    final AllocateAddressResponseType reply = request.getReply( );
    try {
      final String defaultVpcId = getDefaultVpcId( );
      final Address.Domain domain = defaultVpcId != null ?
          Address.Domain.vpc :
          Optional.fromNullable( request.getDomain( ) )
              .transform( Enums.valueOfFunction( Address.Domain.class ) ).or( Address.Domain.standard );
      final Addresses.Allocator allocator = Addresses.allocator( domain );
      final Address address = RestrictedTypes.allocateNamedUnitlessResources( 1, allocator, allocator ).get( 0 );
      reply.setPublicIp( address.getName( ) );
      switch ( domain ) {
        case vpc:
          reply.setAllocationId( address.getAllocationId( ) );
          // fallthrough
        default:
          reply.setDomain( domain.name( ) );
          break;
      }
    } catch( final RuntimeException e ) {
      if( e.getCause( ) != null ) {
        throw new EucalyptusCloudException( e.getCause() );
      } else {
        throw new EucalyptusCloudException( "couldn't allocate addresses" );
      }
    }
    return reply;
  }
  
  @SuppressWarnings( "UnusedDeclaration" )
  public ReleaseAddressResponseType releaseAddress( final ReleaseAddressType request ) throws Exception {
    final ReleaseAddressResponseType reply = request.getReply( ).markFailed( );
    if ( request.getPublicIp( ) == null && request.getAllocationId( ) == null ) {
      throw new ClientComputeException( "MissingParameter", "PublicIp or AllocationId required" );
    }

    final Address address;
    try {
      address = RestrictedTypes.doPrivileged(
          Objects.firstNonNull(
              request.getPublicIp( ),
              ResourceIdentifiers.tryNormalize().apply( request.getAllocationId( ) ) ),
          Address.class );
    } catch ( NoSuchElementException e ) {
      if ( request.getAllocationId( ) != null ) {
        throw new ClientComputeException(
            "InvalidAddressID.NotFound",
            "Address not found for allocation ("+request.getAllocationId( )+")" );
      }
      throw e;
    }

    if ( address.getDomain() == Address.Domain.vpc && address.isAssigned( ) ) {
      throw new ClientComputeException(
          "InvalidIPAddress.InUse",
          "Address ("+address.getName( )+") in use ("+address.getNetworkInterfaceId( )+")" );
    }

    if ( address.isPending( ) ) {
      address.clearPending( );
    }

    Addresses.release( address );

    reply.set_return( true );
    return reply;
  }
  
  @SuppressWarnings( "UnusedDeclaration" )
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
    for ( final Address address : Iterables.filter( Addresses.getInstance( ).listValues( ), filter ) ) {
      reply.getAddressesSet( ).add( verbose
          ? address.getAdminDescription( )
          : TypeMappers.transform( address, AddressInfoType.class ) );
    }
    if ( verbose ) {
      for ( Address address : Iterables.filter( Addresses.getInstance( ).listDisabledValues( ), filter ) ) {
        reply.getAddressesSet( ).add( new AddressInfoType( address.getName( ), Address.Domain.standard.toString(), Principals.nobodyFullName( ).getUserName( ) ) );
      }
    }
    return reply;
  }

  @SuppressWarnings( "UnusedDeclaration" )
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
    if ( !address.isAllocated( ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated: " + request.getPublicIp( ) );
    } else if ( !Contexts.lookup( ).isAdministrator( ) && !Contexts.lookup( ).getUserFullName( ).asAccountFullName( ).getAccountNumber( ).equals( address.getOwner( ).getAccountNumber( ) ) ) {
      throw new EucalyptusCloudException( "Cannot associate an address which is not allocated to your account: " + request.getPublicIp( ) );
    }
    final VmInstance vm = instanceId == null ? null : RestrictedTypes.doPrivileged( instanceId, VmInstance.class );
    if ( address.getDomain( ) != Address.Domain.vpc ) { // EC2-Classic
      if ( vm == null ) {
        throw new ClientComputeException( "InvalidParameterCombination", "InstanceId must be specified when using PublicIp" );
      }
      if ( VmStateSet.NOT_RUNNING.apply( vm ) ) {
        throw new ClientComputeException( "InvalidInstanceID", "The instance '"+vm.getDisplayName( )+"' is not in a valid state for this operation." );
      }
      final VmInstance oldVm = findCurrentAssignedVm( address );
      final Address oldAddr = findVmExistingAddress( vm );
      reply.set_return( true );

      if ( oldAddr != null && address.equals( oldAddr ) ) {
        return reply;
      }

      final UnconditionalCallback<BaseMessage> assignTarget = new UnconditionalCallback<BaseMessage>( ) {
        public void fire( ) {
          AddressingDispatcher.dispatch(
              AsyncRequests.newRequest( address.assign( vm ).getCallback() ).then(
                  new Callback.Success<BaseMessage>() {
                    @Override
                    public void fire( BaseMessage response ) {
                      Addresses.updatePublicIpByInstanceId( vm.getInstanceId(), address.getName() );
                    }
                  }
              ),
              vm.getPartition() );
          if ( oldVm != null ) {
            Addresses.system( oldVm );
          }
        }
      };

      final UnconditionalCallback<BaseMessage> unassignBystander = new UnconditionalCallback<BaseMessage>( ) {
        public void fire( ) {
          if ( oldAddr != null ) {
            AddressingDispatcher.dispatch(
                AsyncRequests.newRequest( oldAddr.unassign().getCallback() ).then( assignTarget ),
                vm.getPartition() );
          } else {
            assignTarget.fire( );
          }
        }
      };

      if ( address.isAssigned( ) ) {
        AddressingDispatcher.dispatch(
            AsyncRequests.newRequest( address.unassign().getCallback() ).then( unassignBystander ),
            oldVm.getPartition() );
      } else {
        unassignBystander.fire( );
      }

    } else { // VPC
      final NetworkInterface networkInterface;
      try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
        if ( vm != null ) {
          if ( VmStateSet.EXPECTING_TEARDOWN.apply( vm ) || VmStateSet.DONE.apply( vm ) ) { // STOPPED is OK
            throw new ClientComputeException( "InvalidInstanceID", "The instance '"+vm.getDisplayName( )+"' is not in a valid state for this operation." );
          }
          networkInterface = Iterables.getOnlyElement( Entities.merge( vm ).getNetworkInterfaces( ) );
        } else {
          if ( networkInterfaceId == null ) {
            throw new ClientComputeException( "MissingParameter", "Either instance ID or network interface id must be specified" );
          }
          networkInterface = RestrictedTypes.doPrivileged( networkInterfaceId, NetworkInterface.class );
          if ( networkInterface.isAttached( ) ) {
            final VmInstance attachedVm = networkInterface.getAttachment( ).getInstance( );
            if ( VmStateSet.EXPECTING_TEARDOWN.apply( attachedVm ) ) { // STOPPED is OK
              throw new ClientComputeException( "IncorrectInstanceState", "The instance to which '"+networkInterfaceId+"' is attached is not in a valid state for this operation" );
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
            address.unassign( eni );
            eni.disassociate( );
            if ( eni.isAttached( ) ) {
              VmInstances.updatePublicAddress( eni.getAttachment( ).getInstance( ), VmNetworkConfig.DEFAULT_IP );
            }
            tx.commit( );
          }
        }

        try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
          final NetworkInterface eni = Entities.merge( networkInterface );
          internetGateways.lookupByVpc(
              AccountFullName.getInstance( address.getOwnerAccountNumber( ) ),
              eni.getVpc( ).getDisplayName( ),
              CloudMetadatas.toDisplayName( ) );

          if ( eni.isAssociated( ) ) {
            NetworkInterfaceHelper.releasePublic( eni );
            eni.disassociate( );
            if ( eni.isAttached( ) ) {
              VmInstances.updatePublicAddress( eni.getAttachment( ).getInstance( ), VmNetworkConfig.DEFAULT_IP );
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

    return reply;
  }
  
  @SuppressWarnings( "UnusedDeclaration" )
  public DisassociateAddressResponseType disassociateAddress( final DisassociateAddressType request ) throws Exception {
    final DisassociateAddressResponseType reply = request.getReply( ).markFailed( );
    final Context ctx = Contexts.lookup( );
    final Address address;
    try {
      address = RestrictedTypes.doPrivileged(
          Objects.firstNonNull(
            request.getPublicIp( ),
            ResourceIdentifiers.tryNormalize( ).apply( request.getAssociationId( ) ) ),
          Address.class );
    } catch ( final NoSuchElementException e ) {
      if ( request.getAssociationId( ) != null ) {
        throw new ClientComputeException( "InvalidAssociationID.NotFound", "Association identifier (" + request.getAssociationId() + ") not found." );
      } else {
        throw e;
      }
    }

    reply.set_return( true );
    if ( address.isSystemOwned( ) && !ctx.isAdministrator( ) ) {
      throw new EucalyptusCloudException( "Only administrators can unassign system owned addresses: " + address.toString() );
    } else if ( address.getDomain( ) != Address.Domain.vpc ) { // EC2-Classic (allow for null domain)
      final String vmId = address.getInstanceId( );
      try {
        final VmInstance vm = VmInstances.lookup( vmId );
        final UnconditionalCallback<BaseMessage> systemAddressAssignmentCallback = new UnconditionalCallback<BaseMessage>( ) {
          @Override
          public void fire( ) {
            try {
              Addresses.system( VmInstances.lookup( vmId ) );
            } catch ( NoSuchElementException e ) {
              LOG.debug( e, e );
            } catch ( Exception e ) {
              LOG.error("Error assigning system address for instance " + vm.getInstanceId(), e);
            }
          }
        };

        AddressingDispatcher.dispatch(
            AsyncRequests.newRequest( address.unassign().getCallback() ).then( systemAddressAssignmentCallback ),
            vm.getPartition() ); 
      } catch ( Exception e ) {
        LOG.debug( e );
        Logs.extreme( ).debug( e, e );
        address.unassign( ).clearPending( );
      }
    } else { // VPC
      final NetworkInterface networkInterface = RestrictedTypes.doPrivileged( address.getNetworkInterfaceId( ), NetworkInterface.class );
      try ( final TransactionResource tx = Entities.transactionFor( NetworkInterface.class ) ) {
        final NetworkInterface eni = Entities.merge( networkInterface );
        if ( address.isStarted( ) ) {
          address.stop( );
        }
        address.unassign( eni );
        eni.disassociate( );
        if ( eni.isAttached( ) ) {
          final VmInstance vm = eni.getAttachment( ).getInstance( );
          VmInstances.updatePublicAddress( vm, VmNetworkConfig.DEFAULT_IP );
          if( !vm.isUsePrivateAddressing( ) &&
              ( VmInstance.VmState.PENDING.equals( vm.getState( ) ) || VmInstance.VmState.RUNNING.equals( vm.getState( ) ) ) ) {
            NetworkInterfaceHelper.associate( Addresses.allocateSystemAddress( ), eni );
          }
        }
        tx.commit( );
      }
    }
    return reply;
  }

  private static Address findVmExistingAddress( final VmInstance vm ) {
    Address oldAddr = null;
    if ( vm.hasPublicAddress( ) ) {
      try {
        oldAddr = Addresses.getInstance( ).lookup( vm.getPublicAddress( ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
    }
    return oldAddr;
  }

  private static VmInstance findCurrentAssignedVm( Address address ) {
    VmInstance oldVm = null;
    if ( address.isAssigned( ) && !address.isPending( ) ) {
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
