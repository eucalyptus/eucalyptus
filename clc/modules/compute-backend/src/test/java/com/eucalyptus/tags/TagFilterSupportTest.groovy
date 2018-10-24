/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.tags

import com.eucalyptus.compute.common.internal.tags.Tag
import com.eucalyptus.compute.common.internal.tags.Tags
import org.junit.Test
import com.google.common.base.Function

/**
 * Unit tests for tag filter support
 */
class TagFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Tag> {

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
