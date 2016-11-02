/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.

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
package com.eucalyptus.compute.vpc;

import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAssociation;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Optional;

/**
 *
 */
public class NatGatewayHelper {
  private static final Logger logger = Logger.getLogger( NatGatewayHelper.class );

  /**
   * Caller must have open transaction for the NAT gateway and subnet.
   *
   * @param natGateway The NAT gateway to update
   * @param subnet The subnet in which to create the network interface.
   * @return The network interface to save
   */
  static NetworkInterface createNetworkInterface(
      @Nonnull final NatGateway natGateway,
      @Nonnull final Subnet subnet
  ) throws ComputeException {
    final Vpc vpc = subnet.getVpc( );
    final String identifier = ResourceIdentifiers.generateString( "eni" );
    final String mac = NetworkInterfaceHelper.mac( identifier );
    final String accountNumber;
    try {
      accountNumber = Accounts.lookupAccountIdentifiersByAlias( AccountIdentifiers.SYSTEM_ACCOUNT ).getAccountNumber( );
    } catch ( final AuthException e ) {
      throw ( ComputeException )
          new ComputeException( "InternalError", "Requester identifier lookup failure" ).initCause( e );
    }
    final String ip;
    try {
      ip = NetworkInterfaceHelper.allocate(
          vpc.getDisplayName( ),
          subnet.getDisplayName( ),
          identifier,
          mac, null );
    } catch ( final ResourceAllocationException e ) {
      throw new ClientComputeException(
          "InsufficientFreeAddressesInSubnet", "Cannot allocate address for subnet " + subnet.getDisplayName( ) );
    }
    final NetworkInterface networkInterface = NetworkInterface.create(
        natGateway.getOwner( ),
        accountNumber,
        vpc,
        subnet,
        identifier,
        mac,
        ip,
        null,
        "Interface for NAT Gateway "  + natGateway.getDisplayName( ) );
    networkInterface.attach( NetworkInterfaceAttachment.create(
        ResourceIdentifiers.generateString( "ela-attach" ),
        natGateway.getDisplayName( ),
        NetworkInterfaceAttachment.Status.attached
    ) );
    natGateway.attach( networkInterface );
    return networkInterface;
  }

  /**
   * Caller must have open transaction for NAT gateway
   */
  static Address associatePublicAddress(
      @Nonnull final NatGateway natGateway
  ) throws ComputeException {
    final String allocationId = natGateway.getAllocationId( );
    Address address;
    try {
      address = RestrictedTypes.resolver( Address.class ).apply( allocationId );
    } catch ( final NoSuchElementException e ) {
      address = null;
    }
    if ( address == null ) {
      throw new ClientComputeException(
          "InvalidAllocationID.NotFound", "Address not found for allocation (" + allocationId + ")" );
    }
    if ( address.isAssigned( ) ) {
      throw new ClientComputeException(
          "Resource.AlreadyAssociated", "Elastic IP address ["+allocationId+"] is already associated" );
    }
    final NetworkInterface networkInterface = natGateway.getNetworkInterface( );
    Addresses.getInstance( ).assign( address, networkInterface );
    networkInterface.associate( NetworkInterfaceAssociation.create(
        address.getAssociationId( ),
        address.getAllocationId( ),
        address.getOwnerAccountNumber( ),
        address.getDisplayName( ),
        null
    ) );
    natGateway.associate( address.getDisplayName( ), address.getAssociationId( ) );
    return address;
  }

  /**
   * Caller must have open transaction for NAT gateway
   *
   * @return The network interface to delete (if present)
   */
  static Optional<NetworkInterface> cleanupResources( final NatGateway natGateway ) {
    final NetworkInterface networkInterface = natGateway.getNetworkInterface( );
    if ( networkInterface != null ) {
      if ( networkInterface.isAttached( ) ) {
        networkInterface.detach( );
      }
      if ( networkInterface.isAssociated( ) ) {
        networkInterface.disassociate( );
      }
    }
    final String associationId = natGateway.getAssociationId( );
    if ( associationId != null ) {
      Address address;
      try {
        address = RestrictedTypes.resolver( Address.class ).apply( associationId );
      } catch ( final Exception e ) {
        address = null;
      }
      if ( address != null ) {
        Addresses.getInstance( ).unassign( address, associationId );
      }
    }
    return Optional.fromNullable( networkInterface );
  }
}
