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
package com.eucalyptus.network

import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for network (a.k.a. security) group filter support
 */
class NetworkGroupFilterSupportTest extends FilterSupportTest.InstanceTest<NetworkGroup> {
  
  @Test
  void testFilteringSupport() {
    assertValid( new NetworkGroups.NetworkGroupFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "description", "test", new NetworkGroup( description: "test" ) )
    assertMatch( false, "description", "test", new NetworkGroup( description: "not test" ) )

    assertMatch( true, "group-id", "sg-00000000", new NetworkGroup( groupId: "sg-00000000" ) )
    assertMatch( false, "group-id", "sg-00000000", new NetworkGroup( groupId: "sg-00000001" ) )

    assertMatch( true, "group-name", "test", NetworkGroup.named( null, "test" ) )
    assertMatch( false, "group-name", "test", NetworkGroup.named( null, "not test" ) )

    assertMatch( true, "ip-permission.cidr", "0.0.0.0/0", new NetworkGroup( networkRules: [ new NetworkRule( ipRanges: [ "0.0.0.0/0" ] ) ] as Set ) )
    assertMatch( false, "ip-permission.cidr", "0.0.0.0/0", new NetworkGroup( networkRules: [ new NetworkRule( ipRanges: [ "1.1.1.1/32" ] ) ] as Set ) )

    assertMatch( true, "ip-permission.from-port", "34", new NetworkGroup( networkRules: [ new NetworkRule( lowPort: 34 ) ] as Set ) )
    assertMatch( false, "ip-permission.from-port", "34", new NetworkGroup( networkRules: [ new NetworkRule( lowPort: 35 ) ] as Set ) )

    assertMatch( true, "ip-permission.group-name", "othergroup", new NetworkGroup( networkRules: [ new NetworkRule( networkPeers: [ new NetworkPeer( userQueryKey: "10000000", groupName: "othergroup" ) ] as Set ) ] as Set ) )
    assertMatch( false, "ip-permission.group-name", "othergroup", new NetworkGroup( networkRules: [ new NetworkRule( networkPeers: [ new NetworkPeer( userQueryKey: "10000000", groupName: "not othergroup" ) ] as Set ) ] as Set ) )

    assertMatch( true, "ip-permission.protocol", "icmp", new NetworkGroup( networkRules: [ new NetworkRule( protocol: NetworkRule.Protocol.icmp ) ] as Set ) )
    assertMatch( false, "ip-permission.protocol", "icmp", new NetworkGroup( networkRules: [ new NetworkRule( protocol: NetworkRule.Protocol.tcp ) ] as Set ) )

    assertMatch( true, "ip-permission.to-port", "60000", new NetworkGroup( networkRules: [ new NetworkRule( highPort: 60000 ) ] as Set ) )
    assertMatch( false, "ip-permission.to-port", "60000", new NetworkGroup( networkRules: [ new NetworkRule( highPort: 60001 ) ] as Set ) )

    assertMatch( true, "ip-permission.user-id", "10000000", new NetworkGroup( networkRules: [ new NetworkRule( networkPeers: [ new NetworkPeer( userQueryKey: "10000000", groupName: "othergroup" ) ] as Set ) ] as Set ) )
    assertMatch( false, "ip-permission.user-id", "10000000", new NetworkGroup( networkRules: [ new NetworkRule( networkPeers: [ new NetworkPeer( userQueryKey: "10000001", groupName: "othergroup" ) ] as Set ) ] as Set ) )

    assertMatch( true, "owner-id", "123456789", new NetworkGroup( ownerAccountNumber: "123456789" ) )
    assertMatch( false, "owner-id", "123456789", new NetworkGroup( ownerAccountNumber: "123456788" ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "group-name", "te*", NetworkGroup.named( null, "test" ) )
    assertMatch( false, "group-name", "te*", NetworkGroup.named( null, "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final NetworkGroup target) {
    super.assertMatch( new NetworkGroups.NetworkGroupFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
