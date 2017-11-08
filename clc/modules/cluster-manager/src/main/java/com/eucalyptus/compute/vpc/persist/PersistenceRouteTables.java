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

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vpc.RouteTable;
import com.eucalyptus.compute.common.internal.vpc.RouteTables;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataException;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentNamed
public class PersistenceRouteTables extends VpcPersistenceSupport<RouteTableMetadata, RouteTable> implements RouteTables {

  public PersistenceRouteTables() {
    super( "route-table" );
  }

  @Override
  public <T> T lookupMain( final String vpcId,
                           final Function<? super RouteTable, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          RouteTable.exampleMain( ),
          Predicates.alwaysTrue( ),
          Restrictions.eq( "vpc.displayName", vpcId ),
          Collections.singletonMap( "vpc", "vpc" ),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( "Main route table not found for " + vpcId );
    }  }

  @Override
  public <T> T updateByAssociationId( final String associationId,
                                           final OwnerFullName ownerFullName,
                                           final Function<RouteTable,T> updateTransform ) throws VpcMetadataException {
    return updateByExample(
        RouteTable.exampleWithOwner( ownerFullName ),
        Restrictions.eq( "routeTableAssociations.associationId", associationId ),
        Collections.singletonMap( "routeTableAssociations", "routeTableAssociations" ),
        ownerFullName,
        associationId,
        updateTransform );
  }

  @Override
  protected RouteTable exampleWithOwner( final OwnerFullName ownerFullName ) {
    return RouteTable.exampleWithOwner( ownerFullName );
  }

  @Override
  protected RouteTable exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return RouteTable.exampleWithName( ownerFullName, name );
  }
}
