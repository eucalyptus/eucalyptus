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
package com.eucalyptus.keys

import com.eucalyptus.tags.FilterSupportTest
import org.junit.Test

/**
 * Unit tests for (SSH) key pair filter support
 */
class KeyPairFilterSupportTest extends FilterSupportTest.InstanceTestSupport<SshKeyPair> {

  @Test
  void testFilteringSupport() {
    assertValid( new KeyPairs.KeyPairFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "fingerprint", "test", new SshKeyPair( fingerPrint: "test" ) )
    assertMatch( false, "fingerprint", "test", new SshKeyPair( fingerPrint: "not test" ) )

    assertMatch( true, "key-name", "test", new SshKeyPair( displayName: "test" ) )
    assertMatch( false, "key-name", "test", new SshKeyPair( displayName: "not test" ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "fingerprint", "te*", new SshKeyPair( fingerPrint: "test" ) )
    assertMatch( false, "fingerprint", "te*", new SshKeyPair( fingerPrint: "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final SshKeyPair target ) {
    super.assertMatch( new KeyPairs.KeyPairFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
