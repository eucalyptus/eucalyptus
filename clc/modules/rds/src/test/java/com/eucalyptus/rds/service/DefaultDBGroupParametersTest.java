/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.rds.service.persist.DBParameterGroups;
import com.eucalyptus.rds.service.persist.DefaultDBGroupParameters;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class DefaultDBGroupParametersTest {
  
  @Test
  public void testUnkonwnDefaultDBParameters() {
    final DefaultDBGroupParameters defaultParameters = DBParameterGroups.loadDefaultParameters("postgres1");
    Assert.assertNotNull("Expected empty parameters for unknown family", defaultParameters);
    Assert.assertTrue("Expected empty parameters for unknown family", defaultParameters.getParameters().isEmpty());
  }

  @Test
  public void testDefaultDBParameters() {
    for (final String parameterGroupFamily : Lists.newArrayList(
        "mariadb10.1",
        "mariadb10.2",
        "mariadb10.3",
        "mariadb10.4",
        "mariadb10.5",
        "postgres9.6",
        "postgres10",
        "postgres11",
        "postgres12",
        "postgres13"
    )) {
      final DefaultDBGroupParameters defaultParameters = DBParameterGroups.loadDefaultParameters(parameterGroupFamily);
      Assert.assertNotNull("Expected parameters for family " + parameterGroupFamily, defaultParameters);
      Assert.assertFalse("Expected parameters for family " + parameterGroupFamily, defaultParameters.getParameters().isEmpty());
    }
  }
}
