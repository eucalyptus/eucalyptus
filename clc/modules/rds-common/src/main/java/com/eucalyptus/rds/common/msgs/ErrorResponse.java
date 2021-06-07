/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.ws.WebServiceError;

public class ErrorResponse extends RdsMessage implements WebServiceError {

  private String requestId;

  private ArrayList<Error> error = new ArrayList<Error>();

  public ErrorResponse() {
    set_return(false);
  }

  public ArrayList<Error> getError() {
    return error;
  }

  public void setError(ArrayList<Error> error) {
    this.error = error;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  @Override
  public String getWebServiceErrorCode() {
    final Error at = error.get(0);
    return (at == null ? null : at.getCode());
  }

  @Override
  public String getWebServiceErrorMessage() {
    final Error at = error.get(0);
    return (at == null ? null : at.getMessage());
  }

  @Override
  public String toSimpleString() {
    final Error at = error.get(0);
    return (at == null ? null : at.getType()) + " error (" + getWebServiceErrorCode() + "): " + getWebServiceErrorMessage();
  }
}
