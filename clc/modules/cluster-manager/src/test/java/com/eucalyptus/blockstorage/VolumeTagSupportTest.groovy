package com.eucalyptus.blockstorage

import com.eucalyptus.tags.TagSupportTest
import org.junit.Test

/**
 * 
 */
class VolumeTagSupportTest extends TagSupportTest {

  @Test
  void testValidTagSupport() {
    assertValidTagSupport( new VolumeTag.VolumeTagSupport(), VolumeTag.class )
  }
}
