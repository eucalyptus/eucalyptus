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

import static com.eucalyptus.compute.common.CloudMetadata.NetworkGroupMetadata;
import java.util.NoSuchElementException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.vpc.SecurityGroups;
import com.eucalyptus.compute.vpc.VpcMetadataException;
import com.eucalyptus.compute.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentNamed
public class PersistenceSecurityGroups extends VpcPersistenceSupport<NetworkGroupMetadata, NetworkGroup> implements SecurityGroups {

  public PersistenceSecurityGroups( ) {
    super( "security-group" );
  }

  @Override
  public <T> T lookupDefault( final String vpcId,
                              final Function<? super NetworkGroup, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          NetworkGroup.namedForVpc( vpcId, NetworkGroups.defaultNetworkName( ) ),
          Predicates.alwaysTrue(),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( "Default security group not found for " + vpcId );
    }
  }

  public boolean delete( final NetworkGroup networkGroup ) throws VpcMetadataException {
    try {
      return Transactions.delete( networkGroup );
    } catch ( NoSuchElementException e ) {
      return false;
    } catch ( Exception e ) {
      throw metadataException( "Error deleting "+typeDescription+" '"+describe( networkGroup )+"'", e );
    }

  }

  @Override
  protected NetworkGroup exampleWithOwner( final OwnerFullName ownerFullName ) {
    return NetworkGroup.withOwner( ownerFullName );
  }

  @Override
  protected NetworkGroup exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return NetworkGroup.named( ownerFullName, name );
  }
}
