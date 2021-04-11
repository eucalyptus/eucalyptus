/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws;

import org.apache.log4j.Logger;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.rds.common.msgs.ErrorResponse;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@ComponentNamed
public class RdsErrorHandler extends ErrorHandlerSupport {

  private static final Logger LOG = Logger.getLogger(RdsErrorHandler.class);

  private static final String INTERNAL_FAILURE = "InternalFailure";

  public RdsErrorHandler() {
    super(LOG, RdsQueryBinding.DEFAULT_NAMESPACE, INTERNAL_FAILURE);
  }

  @Override
  protected BaseMessage buildErrorResponse(final String correlationId,
                                           final Role role,
                                           final String code,
                                           final String message) {
    final ErrorResponse errorResp = new ErrorResponse();
    errorResp.setCorrelationId(correlationId);
    errorResp.setRequestId(correlationId);
    final com.eucalyptus.rds.common.msgs.Error error = new com.eucalyptus.rds.common.msgs.Error();
    error.setType(role == Role.Receiver ? "Receiver" : "Sender");
    error.setCode(code);
    error.setMessage(message);
    errorResp.getError().add(error);
    return errorResp;
  }
}
