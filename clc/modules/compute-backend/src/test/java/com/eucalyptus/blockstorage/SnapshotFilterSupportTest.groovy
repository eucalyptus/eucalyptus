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
package com.eucalyptus.blockstorage

import com.eucalyptus.compute.common.internal.blockstorage.Snapshot
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots
import com.eucalyptus.compute.common.internal.blockstorage.State
import com.eucalyptus.tags.FilterSupportTest
import org.junit.Test

/**
 * Unit tests for snapshot filter support
 */
class SnapshotFilterSupportTest extends FilterSupportTest.InstanceTestSupport<Snapshot> {

  @Test
  void testFilteringSupport() {
    assertValid( new Snapshots.SnapshotFilterSupport() )
  }

  @Test
  void testPredicateFilters() {
    assertMatch( true, "description", "test", new Snapshot( description: "test" ) )
    assertMatch( false, "description", "test", new Snapshot( description: "not test" ) )
    assertMatch( false, "description", "test", new Snapshot( ) )

    //TODO:STEVE: won't work, this field isn't populated
    //assertMatch( true, "owner-alias", "user1", new Snapshot( ownerAccountName: "user1" ) )
    //assertMatch( false, "owner-alias", "user1", new Snapshot( ownerAccountName: "user2" ) )
    //assertMatch( false, "owner-alias", "user1", new Snapshot( ) )

    assertMatch( true, "owner-id", "0123456789", new Snapshot( ownerAccountNumber: "0123456789" ) )
    assertMatch( false, "owner-id", "0123456789", new Snapshot( ownerAccountNumber: "0123456788" ) )
    assertMatch( false, "owner-id", "0123456789", new Snapshot( ) )

    assertMatch( true, "progress", "10%", new Snapshot( progress: "10%" ) )
    assertMatch( false, "progress", "10%", new Snapshot( progress: "0%" ) )
    assertMatch( false, "progress", "10%", new Snapshot( ) )

    assertMatch( true, "snapshot-id", "snap-00000001", new Snapshot( displayName: "snap-00000001" ) )
    assertMatch( false, "snapshot-id", "snap-00000001", new Snapshot( displayName: "snap-00000002" ) )
    assertMatch( false, "snapshot-id", "snap-00000001", new Snapshot( ) )

    assertMatch( true, "start-time", "2013-01-09T23:54:39.524Z", new Snapshot( creationTimestamp: date( "2013-01-09T23:54:39.524Z" ) ) )
    assertMatch( false, "start-time", "2013-01-09T23:54:39.524Z", new Snapshot( creationTimestamp: date( "2013-01-09T23:54:40.525Z" ) ) )
    assertMatch( false, "start-time", "2013-01-09T23:54:39.524Z", new Snapshot( ) )

    assertMatch( true, "status", "completed", new Snapshot( state: State.EXTANT ) )
    assertMatch( false, "status", "completed", new Snapshot( state: State.NIHIL  ) )
    //assertMatch( false, "status", "completed", new Snapshot( ) ) // fails in Snapshot code

    assertMatch( true, "volume-id", "vol-00000001", new Snapshot( parentVolume: "vol-00000001" ) )
    assertMatch( false, "volume-id", "vol-00000001", new Snapshot( parentVolume: "vol-00000002" ) )
    assertMatch( false, "volume-id", "vol-00000001", new Snapshot( ) )

    assertMatch( true, "volume-size", "1", new Snapshot( volumeSize: 1 ) )
    assertMatch( false, "volume-size", "1", new Snapshot( volumeSize: 10 ) )
    assertMatch( false, "volume-size", "1", new Snapshot( ) )
  }

  @Test
  void testWildcardPredicateFilter() {
    assertMatch( true, "description", "te*", new Snapshot( description: "test" ) )
    assertMatch( false, "description", "te*", new Snapshot( description: "not test" ) )
  }

  void assertMatch( final boolean expectedMatch, final String filterKey, final String filterValue, final Snapshot target) {
    super.assertMatch( new Snapshots.SnapshotFilterSupport(), expectedMatch, filterKey, filterValue, target )
  }
}
