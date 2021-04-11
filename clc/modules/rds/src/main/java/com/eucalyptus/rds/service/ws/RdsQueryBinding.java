/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 *
 */
@ComponentPart(Rds.class)
public class RdsQueryBinding extends BaseQueryBinding<OperationParameter> {

  //TODO verify namespace pattern is correct for ns http://rds.amazonaws.com/doc/2014-10-31/
  static final String NAMESPACE_PATTERN = "http://rds.amazonaws.com/doc/%s/";

  static final String DEFAULT_VERSION = "2014-10-31";

  static final String DEFAULT_NAMESPACE = String.format(NAMESPACE_PATTERN, DEFAULT_VERSION);

  public RdsQueryBinding() {
    super(NAMESPACE_PATTERN, DEFAULT_VERSION, OperationParameter.Action, OperationParameter.Operation);
  }
}
