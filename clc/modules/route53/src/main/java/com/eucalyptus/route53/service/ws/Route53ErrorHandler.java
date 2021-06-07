/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws;

import org.apache.log4j.Logger;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.route53.common.msgs.ErrorResponse;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@ComponentNamed
public class Route53ErrorHandler extends ErrorHandlerSupport {

  private static final Logger LOG = Logger.getLogger(Route53ErrorHandler.class);

  private static final String INTERNAL_FAILURE = "InternalFailure";

  public Route53ErrorHandler() {
    super(LOG, Route53RestXmlBinding.DEFAULT_NAMESPACE, INTERNAL_FAILURE);
  }

  @Override
  protected BaseMessage buildErrorResponse(final String correlationId,
                                           final Role role,
                                           final String code,
                                           final String message) {
    final ErrorResponse errorResp = new ErrorResponse();
    errorResp.setCorrelationId(correlationId);
    errorResp.setRequestId(correlationId);
    final com.eucalyptus.route53.common.msgs.Error error = new com.eucalyptus.route53.common.msgs.Error();
    error.setType(role == Role.Receiver ? "Receiver" : "Sender");
    error.setCode(code);
    error.setMessage(message);
    errorResp.getError().add(error);
    return errorResp;
  }
}
