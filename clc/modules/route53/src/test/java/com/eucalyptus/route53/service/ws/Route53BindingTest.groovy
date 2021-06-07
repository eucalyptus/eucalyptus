/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import org.junit.Test

/**
 *
 */
class Route53BindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = Route53BindingTest.class.getResource('/route53-binding.xml')
    assertValidBindingXml(resource)
  }

  @Test
  void testValidQueryBinding() {
    URL resource = Route53BindingTest.class.getResource('/route53-binding.xml')
    assertValidQueryBinding(resource)
  }

  @Test
  void testInternalRoundTrip() {
    URL resource = Route53BindingTest.class.getResource('/route53-binding.xml')
    assertValidInternalRoundTrip(resource)
  }
}
