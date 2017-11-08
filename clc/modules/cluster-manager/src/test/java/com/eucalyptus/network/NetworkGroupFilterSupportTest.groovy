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
package com.eucalyptus.network

import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.compute.common.internal.network.NetworkPeer
import com.eucalyptus.compute.common.internal.network.NetworkRule
import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for network (a.k.a. security) group filter support
 */
class NetworkGroupFilterSupportTest extends FilterSupportTest.InstanceTestSupport<NetworkGroup> {
  
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

    assertMatch( true, "group-name", "test", new NetworkGroup( null, "test" ) )
    assertMatch( false, "group-name", "test", new NetworkGroup( null, "not test" ) )

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
    assertMatch( true, "group-name", "te*", new NetworkGroup( null, "test" ) )
    assertMatch( false, "group-name", "te*", new NetworkGroup( null, "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final NetworkGroup target) {
    super.assertMatch( new NetworkGroups.NetworkGroupFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
