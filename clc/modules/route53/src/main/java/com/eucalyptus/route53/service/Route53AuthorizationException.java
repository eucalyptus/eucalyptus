/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

/**
 *
 */
@QueryBindingInfo( statusCode = 403 )
public class Route53AuthorizationException extends Route53Exception {
  private static final long serialVersionUID = 1L;

  public Route53AuthorizationException( final String code, final String message ) {
    super( code, Role.Sender, message );
  }
}
