/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * <p/>
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.address;

import java.util.NoSuchElementException;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.util.LockResource;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

/**
 *
 */
public class AddressRegistry extends AbstractNamedRegistry<Address> {

  private static final AddressRegistry instance = new AddressRegistry( );

  public static AddressRegistry getInstance( ) {
    return instance;
  }

  public Optional<Address> tryEnable( final String ip ) {
    try ( final LockResource lock = writeLock( ) ) {
      final Address address = lookupDisabled( ip );
      enable( address );
      return Optional.of( address );
    } catch ( final NoSuchElementException e ) {
      return Optional.absent( );
    }
  }

  public Optional<Address> tryEnable( ) {
    try {
      return Optional.of( enableFirst( Predicates.<Address>alwaysTrue( ) ) );
    } catch ( final NoSuchElementException e ) {
      return Optional.absent( );
    }
  }

  LockResource writeLock( ) {
    return LockResource.lock( canHas.writeLock( ) );
  }
}
