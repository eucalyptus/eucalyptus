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
package com.eucalyptus.address

import com.eucalyptus.compute.common.internal.address.AddressI
import com.eucalyptus.tags.FilterSupportTest
import org.junit.Test

/**
 * Unit tests for address (elastic IP) filter support
 */
class AddressFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Address> {

  @Test
  void testPredicateFilters() {
    assertMatch( true, "domain", "standard", new Address( null ) )
    assertMatch( false, "domain", "other", new Address( null ) )

    assertMatch( true, "instance-id", "i-00000001", new Address( null ){ public String getInstanceId(){ 'i-00000001' } } )
    assertMatch( false, "instance-id", "i-00000001", new Address( null ){ public String getInstanceId(){ 'i-00000002' } } )

    assertMatch( true, "public-ip", "1.2.3.4", new Address( '1.2.3.4' ) )
    assertMatch( false, "public-ip", "1.2.3.4", new Address( '1.2.3.3' ) )

    // VPC filters, should fail by not matching (no error)
    assertMatch( false, "allocation-id", "value", new Address( null ) )
    assertMatch( false, "association-id", "value", new Address( null ) )
    assertMatch( false, "network-interface-id", "value", new Address( null ) )
    assertMatch( false, "network-interface-owner-id", "value", new Address( null ) )
    assertMatch( false, "private-ip-address", "value", new Address( null ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "domain", "st*", new Address( null ) )
    assertMatch( false, "domain", "str*", new Address( null ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final AddressI target) {
    super.assertMatch( new Addresses.AddressFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
