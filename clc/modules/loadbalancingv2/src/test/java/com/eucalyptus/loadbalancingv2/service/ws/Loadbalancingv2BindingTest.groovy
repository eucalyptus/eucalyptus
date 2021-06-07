/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.ws

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import org.junit.Test

/**
 *
 */
class Loadbalancingv2BindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = Loadbalancingv2BindingTest.class.getResource( '/loadbalancingv2-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = Loadbalancingv2BindingTest.class.getResource( '/loadbalancingv2-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testInternalRoundTrip() {
    URL resource = Loadbalancingv2BindingTest.class.getResource( '/loadbalancingv2-binding.xml' )
    assertValidInternalRoundTrip( resource )
  }
}
