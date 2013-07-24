/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.bootstrap

import static org.junit.Assert.*
import org.junit.Test
import com.google.common.collect.ImmutableList

/**
 *
 */
class HostsTest {

  @Test
  void testDatabaseErrorsNoError( ) {
    Host coordinator =
      host( "10.1.1.1", [ status( "eucalyptus_cloud", [ "10.1.1.1" ], [ "10.1.1.2" ] ) ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [
        coordinator,
        host( "10.1.1.2", [ status( "eucalyptus_cloud", [ "10.1.1.1" ], [ "10.1.1.2" ] ) ] ),
    ] )

    assertEquals( "No errors", [], errors )
  }

  @Test
  void testDatabaseErrorsInconsistentCoordinator( ) {
    Host coordinator =
      host( "10.1.1.1", [
          status( "eucalyptus_cloud", [ "10.1.1.1" ], [ "10.1.1.2" ] ),
          status( "eucalyptus_auth", [ "10.1.1.2" ], [ "10.1.1.1" ] ),
      ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [ coordinator ] )

    assertTrue( "Inconsistent coordinator: " + errors, errors.findAll{ error -> error =~ /(?i)inconsistent/ }.size( ) == 1 )
  }

  @Test
  void testDatabaseErrorsInconsistentOther( ) {
    Host coordinator =
      host( "10.1.1.1", [
          status( "eucalyptus_cloud", [ "10.1.1.1" ], [ "10.1.1.2" ] ),
          status( "eucalyptus_auth", [ "10.1.1.1" ], [ "10.1.1.2" ] ),
      ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [
        coordinator,
        host( "10.1.1.2", [
            status( "eucalyptus_cloud", [ "10.1.1.1" ], [ "10.1.1.2" ] ),
            status( "eucalyptus_auth", [ "10.1.1.2" ], [ "10.1.1.1" ] ),
        ] )
    ] )

    assertTrue( "Inconsistent other: " + errors, errors.findAll{ error -> error =~ /(?i)inconsistent/ }.size( ) == 1 )
  }

  @Test
  void testDatabaseErrorsNoPrimary( ) {
    Host coordinator =
      host( "10.1.1.1", [
          status( "eucalyptus_cloud", [  ], [ "10.1.1.1", "10.1.1.2" ] ),
      ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [ coordinator ] )

    assertTrue( "No primary error: " + errors, errors.findAll{ error -> error =~ /(?i)primary database not defined/ }.size( ) == 1 )
  }

  @Test
  void testDatabaseErrorsMultiplePrimary( ) {
    Host coordinator =
      host( "10.1.1.1", [
          status( "eucalyptus_cloud", [  "10.1.1.1", "10.1.1.2" ], [ ] ),
      ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [ coordinator ] )

    assertTrue( "Multiple primary error: " + errors, errors.findAll{ error -> error =~ /(?i)multiple primary/ }.size( ) == 1 )
  }

  @Test
  void testDatabaseErrorsInactive( ) {
    Host coordinator =
      host( "10.1.1.1", [
          status( "eucalyptus_cloud", [  "10.1.1.1" ], [ ], [ "10.1.1.2"  ] ),
      ] )

    List<String> errors = Hosts.getHostDatabaseErrors( coordinator, [ coordinator ] )

    assertTrue( "Inactive error: " + errors, errors.findAll{ error -> error =~ /(?i)inactive/ }.size( ) == 1 )
  }

  private Host host( String ip, List<Host.DBStatus> statusList ) {
    new Host(
        ip,
        new org.jgroups.util.UUID(),
        InetAddress.getByName( ip ),
        ImmutableList.of( InetAddress.getByName( ip ) ),
        true,
        true,
        true,
        System.currentTimeMillis( ),
        1,
        ImmutableList.copyOf( statusList ) )
  }

  private Host.DBStatus status( String name,
                                List<String> activePrimary,
                                List<String> activeSecondary,
                                List<String> inactive = [] ) {
    new Host.DBStatus(
        name,
        activePrimary,
        activeSecondary,
        inactive
    )
  }
}
