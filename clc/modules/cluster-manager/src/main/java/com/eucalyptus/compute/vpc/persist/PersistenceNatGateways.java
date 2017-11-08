/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
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
