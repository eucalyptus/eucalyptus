/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;


public class RecordHandlerProgressType extends CloudFormationMessage {

  @Nonnull
  @FieldRange(min = 1, max = 128)
  private String bearerToken;

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @FieldRegex(FieldRegexValue.ENUM_OPERATIONSTATUS)
  private String currentOperationStatus;

  @FieldRegex(FieldRegexValue.ENUM_HANDLERERRORCODE)
  private String errorCode;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_OPERATIONSTATUS)
  private String operationStatus;

  @FieldRange(min = 1, max = 16384)
  private String resourceModel;

  @FieldRange(max = 1024)
  private String statusMessage;

  public String getBearerToken() {
    return bearerToken;
  }

  public void setBearerToken(final String bearerToken) {
    this.bearerToken = bearerToken;
  }

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public String getCurrentOperationStatus() {
    return currentOperationStatus;
  }

  public void setCurrentOperationStatus(final String currentOperationStatus) {
    this.currentOperationStatus = currentOperationStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public String getOperationStatus() {
    return operationStatus;
  }

  public void setOperationStatus(final String operationStatus) {
    this.operationStatus = operationStatus;
  }

  public String getResourceModel() {
    return resourceModel;
  }

  public void setResourceModel(final String resourceModel) {
    this.resourceModel = resourceModel;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(final String statusMessage) {
    this.statusMessage = statusMessage;
  }

}
