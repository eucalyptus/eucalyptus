/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.rds.service.RdsService;
import com.eucalyptus.util.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import static com.eucalyptus.util.Json.JsonOption.UpperCamelPropertyNaming;

public interface DBParameterGroups {

  /**
   * Load default parameters for a family
   *
   * Will load an empty group for unknown family.
   */
  @SuppressWarnings("UnstableApiUsage")
  static DefaultDBGroupParameters loadDefaultParameters(final String family) {
    DefaultDBGroupParameters defaultParameters = new DefaultDBGroupParameters();

    try {
      final String resourceName = "rds-pg-" + family + ".json";
      final String parametersJson = Resources.toString(
          Resources.getResource(RdsService.class, resourceName),
          StandardCharsets.UTF_8);

      final ObjectMapper mapper = Json.mapper(EnumSet.of(UpperCamelPropertyNaming));
      defaultParameters = Json.readObject(
          mapper.reader(),
          DefaultDBGroupParameters.class,
          parametersJson);
    } catch (final Exception ignore) {
    }

    return defaultParameters;
  }
}
