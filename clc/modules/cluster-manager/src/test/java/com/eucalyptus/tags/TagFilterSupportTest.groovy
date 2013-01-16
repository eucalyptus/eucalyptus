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
package com.eucalyptus.tags

import org.junit.Test
import com.google.common.base.Function

/**
 * Unit tests for tag filter support
 */
class TagFilterSupportTest extends FilterSupportTest.InstanceTest<Tag> {

  @Test
  void testFilteringSupport() {
    assertValid( new Tags.TagFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "key", "test", new Tag( key: "test" ) )
    assertMatch( false, "key", "test", new Tag( key: "not test" ) )

    assertMatch( true, "resource-id", "test", new Tag( resourceIdFunction: { "test" } as Function ) )
    assertMatch( false, "resource-id", "test", new Tag( resourceIdFunction: { "not test" } as Function ) )

    assertMatch( true, "resource-type", "test", new Tag( resourceType: "test" ) )
    assertMatch( false, "resource-type", "test", new Tag( resourceType: "not test" ) )

    assertMatch( true, "value", "test", new Tag( value: "test" ) )
    assertMatch( false, "value", "test", new Tag( value: "not test" ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "key", "te*", new Tag( key: "test" ) )
    assertMatch( false, "key", "te*", new Tag( key: "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final Tag target ) {
    super.assertMatch( new Tags.TagFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

}
