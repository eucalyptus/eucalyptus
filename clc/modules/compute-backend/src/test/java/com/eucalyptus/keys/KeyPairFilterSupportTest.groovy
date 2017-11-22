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
package com.eucalyptus.keys

import com.eucalyptus.compute.common.internal.keys.KeyPairs
import com.eucalyptus.compute.common.internal.keys.SshKeyPair
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
