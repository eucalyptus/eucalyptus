/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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