package com.eucalyptus.auth.ws

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import org.junit.Test

/**
 * 
 */
class EuareBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = EuareBindingTest.class.getResource( '/euare-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = EuareBindingTest.class.getResource( '/euare-binding.xml' )
    assertValidQueryBinding( resource )
  }
}
