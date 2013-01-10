package com.eucalyptus.blockstorage

import com.eucalyptus.tags.FilterSupportTest
import org.junit.Test

/**
 * Unit tests for snapshot filter support
 */
class SnapshotFilterSupportTest extends FilterSupportTest.InstanceTest<Snapshot> {

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
    assertMatch( false, "owner-alias", "user1", new Snapshot( ownerAccountName: "user2" ) )
    assertMatch( false, "owner-alias", "user1", new Snapshot( ) )

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
