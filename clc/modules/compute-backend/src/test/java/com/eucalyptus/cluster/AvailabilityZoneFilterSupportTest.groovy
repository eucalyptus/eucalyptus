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
package com.eucalyptus.cluster

import com.eucalyptus.cluster.common.TestClusterProvider
import com.eucalyptus.cluster.common.Cluster
import org.junit.Test
import com.eucalyptus.tags.FilterSupportTest

/**
 * Unit tests for availability zone filter support
 */
class AvailabilityZoneFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Cluster> {

  @Test
  void testFilteringSupport() {
    assertValid( new ClusterEndpoint.AvailabilityZoneFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "zone-name", "PARTI00", cluster( "PARTI00" ) )
    assertMatch( false, "zone-name", "PARTI00", cluster( "PARTI01" ) )
    assertMatch( false, "zone-name", "PARTI00", cluster( null ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "zone-name", "PAR*", cluster( "PARTI00" ) )
    assertMatch( false, "zone-name", "AR*", cluster( "PARTI00" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final Cluster target ) {
    super.assertMatch( new ClusterEndpoint.AvailabilityZoneFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }

  private Cluster cluster( String name ) {
    new Cluster( new TestClusterProvider( name: name, partition: name ) )
  }
}
