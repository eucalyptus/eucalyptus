/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.rds.common.msgs.Parameter;
import com.eucalyptus.rds.service.persist.DBParameterGroups;
import com.eucalyptus.rds.service.persist.DefaultDBGroupParameters;
import com.google.common.collect.Lists;
import java.util.Objects;
import org.hamcrest.Matchers;
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
    for (final String parameterGroupFamily : Lists.newArrayList(  //TODO:STEVE: get values from RdsEngine[Version]
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

      for (final Parameter parameter : defaultParameters.getParameters()) {
        Assert.assertThat(parameterGroupFamily + " allowedValues length", Objects.toString(parameter.getAllowedValues(), "").length(), Matchers.lessThan(4096));
        Assert.assertThat(parameterGroupFamily + " applyMethod length", Objects.toString(parameter.getApplyMethod(), "").length(), Matchers.lessThan(255));
        Assert.assertThat(parameterGroupFamily + " applyType length", Objects.toString(parameter.getApplyType(), "").length(), Matchers.lessThan(255));
        Assert.assertThat(parameterGroupFamily + " dataType length", Objects.toString(parameter.getDataType(), "").length(), Matchers.lessThan(255));
        Assert.assertThat(parameterGroupFamily + " description length", Objects.toString(parameter.getDescription(), "").length(), Matchers.lessThan(1024));
        Assert.assertThat(parameterGroupFamily + " minimumEngineVersion length", Objects.toString(parameter.getMinimumEngineVersion(), "").length(), Matchers.lessThan(255));
        Assert.assertThat(parameterGroupFamily + " parameterName length", Objects.toString(parameter.getParameterName(), "").length(), Matchers.lessThan(255));
        Assert.assertThat(parameterGroupFamily + " parameterValue length", Objects.toString(parameter.getParameterValue(), "").length(), Matchers.lessThan(1024));
        Assert.assertThat(parameterGroupFamily + " source length", Objects.toString(parameter.getSource(), "").length(), Matchers.lessThan(255));
      }
    }
  }
}
