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
package com.eucalyptus.network

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 *
 */
@CompileStatic
class PublicAddresses {
  private static final Map<String,String> dirtyAddresses = Maps.newConcurrentMap( )

  static void markDirty( String address, String partition ) {
    dirtyAddresses.put( address, partition )
  }

  @PackageScope
  static boolean clearDirty( Collection<String> inUse, String partition ) {
    boolean cleared = false;
    dirtyAddresses.each{ String address, String addressPartition ->
      if ( partition == addressPartition && !inUse.contains( address )) {
        cleared = dirtyAddresses.remove( address ) || cleared
      }
    }
    cleared
  }

  @PackageScope
  static boolean clearDirty( String address ) {
    dirtyAddresses.remove( address )
  }

  @PackageScope
  static boolean isDirty( String address ) {
    dirtyAddresses.containsKey( address )
  }

  @PackageScope
  static Set<String> dirtySnapshot( ) {
    Sets.newHashSet( dirtyAddresses.keySet( ) )
  }
}
