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
@QueryBindingInfo(statusCode = 400)
public class Loadbalancingv2ClientException extends Loadbalancingv2Exception {

  private static final long serialVersionUID = 1L;

  public Loadbalancingv2ClientException(final String code, final String message) {
    super(code, Role.Sender, message);
  }
}
