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
package com.eucalyptus.vm

import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest
import com.eucalyptus.tags.FilterSupport

/**
 * Unit tests for bundle task filter support
 */
class VmBundleTaskFilterSupportTest extends FilterSupportTest.InstanceTestSupport<VmBundleTask> {

  @Test
  void testFilteringSupport() {
    FilterSupport<VmBundleTask> filterSupport = new VmInstances.VmBundleTaskFilterSupport( )
    assertValidAliases( filterSupport )
    assertValidFilters( filterSupport, VmInstance, [:] )
    assertValidKeys( filterSupport )
    assertValidTagConfig( filterSupport )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "error-code", "test", new VmBundleTask( errorCode: "test" ) )
    assertMatch( false, "error-code", "test", new VmBundleTask( errorCode: "not test" ) )
    assertMatch( false, "error-code", "test", new VmBundleTask( ) )

    assertMatch( true, "error-message", "test", new VmBundleTask( errorMessage: "test" ) )
    assertMatch( false, "error-message", "test", new VmBundleTask( errorMessage: "not test" ) )
    assertMatch( false, "error-message", "test", new VmBundleTask( ) )

    assertMatch( true, "progress", "10%", new VmBundleTask( progress: 10 ) )
    assertMatch( false, "progress", "10%", new VmBundleTask( progress: 11 ) )
    assertMatch( false, "progress", "10%", new VmBundleTask( ) )

    assertMatch( true, "s3-bucket", "test", new VmBundleTask( bucket: "test" ) )
    assertMatch( false, "s3-bucket", "test", new VmBundleTask( bucket: "not test" ) )
    assertMatch( false, "s3-bucket", "test", new VmBundleTask( ) )

    assertMatch( true, "s3-prefix", "test", new VmBundleTask( prefix: "test" ) )
    assertMatch( false, "s3-prefix", "test", new VmBundleTask( prefix: "not test" ) )
    assertMatch( false, "s3-prefix", "test", new VmBundleTask( ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "error-code", "te*", new VmBundleTask( errorCode: "test" ) )
    assertMatch( false, "error-code", "te*", new VmBundleTask( errorCode: "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final VmBundleTask target ) {
    super.assertMatch( new VmInstances.VmBundleTaskFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
