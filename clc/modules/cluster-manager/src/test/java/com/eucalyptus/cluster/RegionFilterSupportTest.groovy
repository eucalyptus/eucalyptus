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

import com.eucalyptus.auth.RegionService
import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for snapshot filter support
 */
class RegionFilterSupportTest extends FilterSupportTest.InstanceTestSupport<RegionService> {

  @Test
  void testFilteringSupport() {
    assertValid( new ClusterEndpoint.RegionFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "region-name", "eucalyptus", new RegionService( "eucalyptus", "ec2", "http://eucalyptus.com" ) )
    assertMatch( false, "region-name", "eucalyptus", new RegionService( "eucalyptus-west", "ec2", "http://eucalyptus.com" ) )

    assertMatch( true, "endpoint", "http://eucalyptus.com", new RegionService( "eucalyptus", "ec2", "http://eucalyptus.com" ) )
    assertMatch( false, "endpoint", "http://eucalyptus.com", new RegionService( "eucalyptus", "ec2", "http://eucalyptus.com/foo" ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "region-name", "eu*", new RegionService( "eucalyptus", "ec2", "http://eucalyptus.com" ) )
    assertMatch( false, "region-name", "eur*", new RegionService( "eucalyptus", "ec2", "http://eucalyptus.com" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final RegionService target ) {
    super.assertMatch( new ClusterEndpoint.RegionFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

}
