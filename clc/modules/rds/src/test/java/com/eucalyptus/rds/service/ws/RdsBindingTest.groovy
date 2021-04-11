/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import org.junit.Test

/**
 *
 */
class RdsBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = RdsBindingTest.class.getResource( '/rds-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = RdsBindingTest.class.getResource( '/rds-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testInternalRoundTrip() {
    URL resource = RdsBindingTest.class.getResource( '/rds-binding.xml' )
    assertValidInternalRoundTrip( resource )
  }
}
