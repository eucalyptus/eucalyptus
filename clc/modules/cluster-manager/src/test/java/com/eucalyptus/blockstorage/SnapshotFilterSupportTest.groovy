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
}
