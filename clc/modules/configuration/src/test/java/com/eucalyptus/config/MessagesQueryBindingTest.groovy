package com.eucalyptus.config

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import org.junit.Test

/**
 *
 */
class MessagesQueryBindingTest extends QueryBindingTestSupport{
  @Test
  void testValidBinding() {
    URL resource = MessagesQueryBindingTest.getResource('/msgs-binding.xml')
    assertValidBindingXml(resource)
  }
}
