/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cluster

import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for availability zone filter support
 */
class AvailabilityZoneFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Cluster> {

  @Test
  void testFilteringSupport() {
    assertValid( new ClusterEndpoint.AvailabilityZoneFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "zone-name", "PARTI00", cluster( "PARTI00" ) )
    assertMatch( false, "zone-name", "PARTI00", cluster( "PARTI01" ) )
    assertMatch( false, "zone-name", "PARTI00", cluster( null ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "zone-name", "PAR*", cluster( "PARTI00" ) )
    assertMatch( false, "zone-name", "AR*", cluster( "PARTI00" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final Cluster target ) {
    super.assertMatch( new ClusterEndpoint.AvailabilityZoneFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

  private Cluster cluster( String name ) {
    new Cluster( new ClusterConfiguration( partition: name ), (Void) null )
  }
}
