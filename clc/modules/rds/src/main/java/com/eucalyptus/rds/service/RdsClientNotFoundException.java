/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo(statusCode = 404)
public class RdsClientNotFoundException extends RdsClientException {

  private static final long serialVersionUID = 1L;

  public RdsClientNotFoundException(final String code, final String message) {
    super(code, message);
  }
}
