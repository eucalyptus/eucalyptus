/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 *
 */
public class Route53Exception extends EucalyptusWebServiceException {

  private static final long serialVersionUID = 1L;

  public Route53Exception(
      final String code,
      final Role role,
      final String message) {
    super(code, role, message);
  }
}
