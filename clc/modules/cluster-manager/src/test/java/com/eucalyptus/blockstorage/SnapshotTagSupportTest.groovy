package com.eucalyptus.blockstorage

import com.eucalyptus.tags.TagSupportTest
import org.junit.Test

/**
 * 
 */
class SnapshotTagSupportTest extends TagSupportTest {

  @Test
  void testValidTagSupport() {
    assertValidTagSupport( new SnapshotTag.SnapshotTagSupport(), SnapshotTag.class )
  }
}
