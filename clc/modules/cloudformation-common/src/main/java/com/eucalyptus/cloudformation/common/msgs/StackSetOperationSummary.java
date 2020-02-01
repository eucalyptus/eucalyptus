/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StackSetOperationSummary extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_STACKSETOPERATIONACTION)
  private String action;

  private java.util.Date creationTimestamp;

  private java.util.Date endTimestamp;

  @FieldRange(min = 1, max = 128)
  private String operationId;

  @FieldRegex(FieldRegexValue.ENUM_STACKSETOPERATIONSTATUS)
  private String status;

  public String getAction() {
    return action;
  }

  public void setAction(final String action) {
    this.action = action;
  }

  public java.util.Date getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(final java.util.Date creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public java.util.Date getEndTimestamp() {
    return endTimestamp;
  }

  public void setEndTimestamp(final java.util.Date endTimestamp) {
    this.endTimestamp = endTimestamp;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(final String operationId) {
    this.operationId = operationId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
