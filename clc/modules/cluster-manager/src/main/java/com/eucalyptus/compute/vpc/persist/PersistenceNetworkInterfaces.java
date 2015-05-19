/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.vpc.persist;

import static com.eucalyptus.compute.common.CloudMetadata.NetworkInterfaceMetadata;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 *
 */
@ComponentNamed
public class PersistenceNetworkInterfaces extends VpcPersistenceSupport<NetworkInterfaceMetadata, NetworkInterface> implements NetworkInterfaces {

  public PersistenceNetworkInterfaces( ) {
    super( "network-interfaces" );
  }

  @Override
  protected NetworkInterface exampleWithOwner( final OwnerFullName ownerFullName ) {
    return NetworkInterface.exampleWithOwner( ownerFullName );
  }

  @Override
  protected NetworkInterface exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return NetworkInterface.exampleWithName( ownerFullName, name );
  }

  @RestrictedTypes.Resolver( NetworkInterface.class )
  public enum Lookup implements Function<String, NetworkInterface> {
    INSTANCE;

    @Override
    public NetworkInterface apply( final String identifier ) {
      try {
        return new PersistenceNetworkInterfaces( ).lookupByName( null, identifier, Functions.<NetworkInterface>identity( ) );
      } catch ( VpcMetadataException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }
}
