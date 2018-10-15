/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.workflow;

import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter;

/**
 *
 */
public class LoadBalancingJsonDataConverter {

  private static final JsonDataConverter DEFAULT = new JsonDataConverter( );

  static JsonDataConverter getDefault( ) {
    return DEFAULT;
  }
}