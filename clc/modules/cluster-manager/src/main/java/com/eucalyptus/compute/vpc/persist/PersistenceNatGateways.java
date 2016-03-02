/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import java.util.Collections;
import java.util.EnumSet;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.internal.vpc.NatGateway;
import com.eucalyptus.compute.common.internal.vpc.NatGateway.State;
import com.eucalyptus.compute.common.internal.vpc.NatGateways;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceNatGateways extends VpcPersistenceSupport<CloudMetadata.NatGatewayMetadata, NatGateway> implements NatGateways {

  public PersistenceNatGateways( ) {
    super( "nat-gateway" );
  }

  @Override
  public <T> T lookupByClientToken(
      final OwnerFullName ownerFullName,
      final String clientToken,
      final Function<? super NatGateway, T> transform
  ) throws VpcMetadataException {
    return lookupByExample(
        NatGateway.exampleWithClientToken( ownerFullName, clientToken ),
        ownerFullName,
        clientToken,
        Predicates.alwaysTrue( ),
        transform
    );
  }

  @Override
  public long countByZone(
      final OwnerFullName ownerFullName,
      final String availabilityZone
  ) throws VpcMetadataException {
    return countByExample(
        NatGateway.exampleWithOwner( ownerFullName ),
        Restrictions.and(
            Restrictions.in( "state", EnumSet.of( State.pending, State.available, State.deleting ) ),
            Restrictions.eq( "subnet.availabilityZone", availabilityZone )
        ),
        Collections.singletonMap( "subnet", "subnet" ) );
  }

  @Override
  protected NatGateway exampleWithOwner( final OwnerFullName ownerFullName ) {
    return NatGateway.exampleWithOwner( ownerFullName );
  }

  @Override
  protected NatGateway exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return NatGateway.exampleWithName( ownerFullName, name );
  }
}
