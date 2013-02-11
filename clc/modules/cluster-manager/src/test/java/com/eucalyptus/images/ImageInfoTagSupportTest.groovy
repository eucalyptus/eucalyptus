package com.eucalyptus.images

import com.eucalyptus.tags.TagSupportTest
import org.junit.Test

/**
 * 
 */
class ImageInfoTagSupportTest extends TagSupportTest {
  
  @Test
  void testValidTagSupport() {
    assertValidTagSupport( new ImageInfoTag.ImageInfoTagSupport(), ImageInfoTag.class )
  }
}
