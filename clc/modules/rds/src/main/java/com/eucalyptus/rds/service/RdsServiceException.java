/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

/**
 *
 */
@QueryBindingInfo(statusCode = 500)
public class RdsServiceException extends RdsException {

  private static final long serialVersionUID = 1L;

  public RdsServiceException(final String code, final String message) {
    super(code, Role.Receiver, message);
  }
}
