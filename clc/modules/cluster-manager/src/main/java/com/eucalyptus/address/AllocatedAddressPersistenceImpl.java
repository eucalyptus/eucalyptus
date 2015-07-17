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
package com.eucalyptus.address;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.entities.AbstractPersistentSupport;

/**
 *
 */
@ComponentNamed
class AllocatedAddressPersistenceImpl extends AbstractPersistentSupport<AddressMetadata, AllocatedAddressEntity, AllocatedAddressPersistenceException> implements AllocatedAddressPersistence {

  public AllocatedAddressPersistenceImpl() {
    super( "address" );
  }

  @Override
  protected AllocatedAddressEntity exampleWithOwner( final OwnerFullName ownerFullName ) {
    return AllocatedAddressEntity.exampleWithOwnerAndAddress( ownerFullName, null );
  }

  @Override
  protected AllocatedAddressEntity exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return AllocatedAddressEntity.exampleWithOwnerAndAddress( ownerFullName, name );
  }

  @Override
  protected AllocatedAddressPersistenceException notFoundException( final String message, final Throwable cause ) {
    return new AllocatedAddressPersistenceException( message, cause );
  }

  @Override
  protected AllocatedAddressPersistenceException metadataException( final String message, final Throwable cause ) {
    return new AllocatedAddressPersistenceException( message, cause );
  }
}
