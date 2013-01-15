package com.eucalyptus.vm

import com.eucalyptus.tags.TagSupportTest
import org.junit.Test

/**
 * 
 */
class VmInstanceTagSupportTest extends TagSupportTest {
  
  @Test
  void testValidTagSupport() {
    assertValidTagSupport( new VmInstanceTag.VmInstanceTagSupport(), VmInstanceTag.class )
  }
}
