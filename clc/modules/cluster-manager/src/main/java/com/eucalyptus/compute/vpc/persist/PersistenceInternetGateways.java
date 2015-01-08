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
import javax.annotation.Nullable;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.vpc.InternetGateway;
import com.eucalyptus.compute.vpc.InternetGateways;
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
public class PersistenceInternetGateways extends VpcPersistenceSupport<CloudMetadata.InternetGatewayMetadata, InternetGateway> implements InternetGateways {

  public PersistenceInternetGateways( ) {
    super( "internet-gateway" );
  }

  @Override
  public <T> T lookupByVpc( @Nullable final OwnerFullName ownerFullName,
                            final String vpcId,
                            final Function<? super InternetGateway, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          InternetGateway.exampleWithOwner( ownerFullName ),
          Predicates.alwaysTrue(),
          Restrictions.eq( "vpc.displayName", vpcId ),
          Collections.singletonMap( "vpc", "vpc" ),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( "Internet gateway not found for " + vpcId );
    }
  }

  @Override
  protected InternetGateway exampleWithOwner( final OwnerFullName ownerFullName ) {
    return InternetGateway.exampleWithOwner( ownerFullName );
  }

  @Override
  protected InternetGateway exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return InternetGateway.exampleWithName( ownerFullName, name );
  }
}
