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

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.vpc.RouteTable;
import com.eucalyptus.compute.vpc.RouteTables;
import com.eucalyptus.compute.vpc.VpcMetadataException;
import com.eucalyptus.compute.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

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
