/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.ws.WebServiceError;

public class ErrorResponse extends CloudFormationMessage implements WebServiceError {

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
