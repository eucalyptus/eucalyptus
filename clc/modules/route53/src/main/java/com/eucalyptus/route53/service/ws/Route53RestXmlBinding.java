/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws;

import javax.annotation.Nonnull;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.route53.common.Route53;
import com.eucalyptus.route53.common.msgs.ErrorResponse;
import com.eucalyptus.route53.service.config.Route53Configuration;
import com.eucalyptus.ws.protocol.BaseRestXmlBinding;

/**
 *
 */
@ComponentPart(Route53.class)
public class Route53RestXmlBinding extends BaseRestXmlBinding<ErrorResponse> {

  //TODO verify namespace pattern is correct for ns https://route53.amazonaws.com/doc/2013-04-01/
  static final String NAMESPACE_PATTERN = "http://route53.amazonaws.com/doc/%s/";

  static final String DEFAULT_VERSION = "2013-04-01";

  static final String DEFAULT_NAMESPACE = String.format(NAMESPACE_PATTERN, DEFAULT_VERSION);

  public Route53RestXmlBinding() {
    super(NAMESPACE_PATTERN, DEFAULT_VERSION, Route53Configuration.SERVICE_PATH, true);
  }

  @Override
  protected ErrorResponse errorResponse(
      final String requestId,
      @Nonnull final String type,
      @Nonnull final String code,
      @Nonnull final String message) {
    final ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setRequestId(requestId);
    final com.eucalyptus.route53.common.msgs.Error error = new com.eucalyptus.route53.common.msgs.Error();
    error.setType(type);
    error.setCode(code);
    error.setMessage(message);
    errorResponse.getError().add(error);
    return errorResponse;
  }
}
