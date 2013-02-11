package com.eucalyptus.network

import com.eucalyptus.tags.TagSupportTest
import org.junit.Test

/**
 * 
 */
class NetworkGroupTagSupportTest extends TagSupportTest {

  @Test
  void testValidTagSupport() {
    assertValidTagSupport( new NetworkGroupTag.NetworkGroupTagSupport(), NetworkGroupTag.class )
  }
}
