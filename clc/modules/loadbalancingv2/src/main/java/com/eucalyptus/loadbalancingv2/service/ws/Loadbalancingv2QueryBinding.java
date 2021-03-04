/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
@ComponentPart(Loadbalancingv2.class)
public class Loadbalancingv2QueryBinding extends BaseQueryBinding<OperationParameter> {

  //TODO verify namespace pattern is correct for ns http://elasticloadbalancing.amazonaws.com/doc/2015-12-01/
  static final String NAMESPACE_PATTERN = "http://loadbalancingv2.amazonaws.com/doc/%s/";

  static final String DEFAULT_VERSION = "2015-12-01";

  static final String DEFAULT_NAMESPACE = String.format(NAMESPACE_PATTERN, DEFAULT_VERSION);

  public Loadbalancingv2QueryBinding() {
    super(NAMESPACE_PATTERN, DEFAULT_VERSION, OperationParameter.Action, OperationParameter.Operation);
  }
}
