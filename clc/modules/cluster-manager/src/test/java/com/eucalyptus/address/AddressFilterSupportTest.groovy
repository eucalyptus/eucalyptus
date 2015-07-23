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
