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

import java.util.NoSuchElementException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.vpc.Subnet;
import com.eucalyptus.compute.vpc.Subnets;
import com.eucalyptus.compute.vpc.VpcMetadataException;
import com.eucalyptus.compute.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
@ComponentNamed
public class PersistenceSubnets extends VpcPersistenceSupport<CloudMetadata.SubnetMetadata, Subnet> implements Subnets {

  public PersistenceSubnets( ) {
    super( "subnet" );
  }

  @Override
  public <T> T lookupDefault( final OwnerFullName ownerFullName,
                              final String availabilityZone,
                              final Function<? super Subnet, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          Subnet.exampleDefault( ownerFullName, availabilityZone ),
          Predicates.alwaysTrue( ),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( qualifyOwner( "Default subnet not found for zone: " + availabilityZone, ownerFullName ) );
    }
  }



  @Override
  protected Subnet exampleWithOwner( final OwnerFullName ownerFullName ) {
    return Subnet.exampleWithOwner( ownerFullName );
  }

  @Override
  protected Subnet exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return Subnet.exampleWithName( ownerFullName, name );
  }
}
