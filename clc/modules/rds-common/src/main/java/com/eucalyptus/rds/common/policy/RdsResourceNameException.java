/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.policy;

public class RdsResourceNameException extends net.sf.json.JSONException {

  public RdsResourceNameException(final String msg) {
    super(msg);
  }

  public RdsResourceNameException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}
