/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static com.eucalyptus.compute.common.CloudMetadata.NetworkGroupMetadata;
import java.util.NoSuchElementException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vpc.SecurityGroups;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.auth.principal.OwnerFullName;
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
