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

import static com.eucalyptus.compute.common.CloudMetadata.NetworkAclMetadata;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl;
import com.eucalyptus.compute.common.internal.vpc.NetworkAcls;
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
public class PersistenceNetworkAcls extends VpcPersistenceSupport<NetworkAclMetadata, NetworkAcl> implements NetworkAcls {

  public PersistenceNetworkAcls() {
    super( "network-acl" );
  }

  @Override
  public <T> T lookupDefault( final String vpcId,
                              final Function<? super NetworkAcl, T> transform ) throws VpcMetadataException {
    try {
      return Iterables.getOnlyElement( listByExample(
          NetworkAcl.exampleDefault( ),
          Predicates.alwaysTrue( ),
          Restrictions.eq( "vpc.displayName", vpcId ),
          Collections.singletonMap( "vpc", "vpc" ),
          transform ) );
    } catch ( NoSuchElementException e ) {
      throw new VpcMetadataNotFoundException( "Default network acl not found for " + vpcId );
    }
  }

  @Override
  protected NetworkAcl exampleWithOwner( final OwnerFullName ownerFullName ) {
    return NetworkAcl.exampleWithOwner( ownerFullName );
  }

  @Override
  protected NetworkAcl exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return NetworkAcl.exampleWithName( ownerFullName, name );
  }
}
