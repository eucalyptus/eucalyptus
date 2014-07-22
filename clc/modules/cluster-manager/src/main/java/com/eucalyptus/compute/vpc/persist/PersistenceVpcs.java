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

import java.util.Collections;
import java.util.NoSuchElementException;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.vpc.Vpc;
import com.eucalyptus.compute.vpc.VpcMetadataException;
import com.eucalyptus.compute.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.vpc.Vpcs;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentNamed
public class PersistenceVpcs extends VpcPersistenceSupport<CloudMetadata.VpcMetadata, Vpc> implements Vpcs {

  public PersistenceVpcs( ) {
    super( "vpc" );
  }

  @Override
  public <T> T lookupDefault( final OwnerFullName ownerFullName, final Function<? super Vpc, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          Vpc.exampleDefault( ownerFullName ),
          Predicates.alwaysTrue(),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( qualifyOwner( "Default VPC not found", ownerFullName ) );
    }
  }

  @Override
  protected Vpc exampleWithOwner( final OwnerFullName ownerFullName ) {
    return Vpc.exampleWithOwner( ownerFullName );
  }

  @Override
  protected Vpc exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return Vpc.exampleWithName( ownerFullName, name );
  }
}
