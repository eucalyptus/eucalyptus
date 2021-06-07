/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

/**
 *
 */
@QueryBindingInfo(statusCode = 500)
public class Loadbalancingv2ServiceException extends Loadbalancingv2Exception {

  private static final long serialVersionUID = 1L;

  public Loadbalancingv2ServiceException(final String code, final String message) {
    super(code, Role.Receiver, message);
  }
}
