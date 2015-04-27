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

import static com.eucalyptus.compute.common.CloudMetadata.DhcpOptionSetMetadata;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet;
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSets;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@ComponentNamed
public class PersistenceDhcpOptionSets extends VpcPersistenceSupport<DhcpOptionSetMetadata, DhcpOptionSet> implements DhcpOptionSets {

  public PersistenceDhcpOptionSets( ) {
    super( "dhcp-options" );
  }

  @Override
  protected DhcpOptionSet exampleWithOwner( final OwnerFullName ownerFullName ) {
    return DhcpOptionSet.exampleWithOwner( ownerFullName );
  }

  @Override
  protected DhcpOptionSet exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return DhcpOptionSet.exampleWithName( ownerFullName, name );
  }
}
