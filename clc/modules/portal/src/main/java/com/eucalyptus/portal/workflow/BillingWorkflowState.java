/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.workflow;

public enum BillingWorkflowState {
  WORKFLOW_RUNNING,
  WORKFLOW_SUCCESS,
  WORKFLOW_CANCELLED,
  WORKFLOW_FAILED;

  private String reason = null;
  private int statusCode = 500;
  BillingWorkflowState() {
    this.reason = null;
  }

  public BillingWorkflowState withReason(final String reason) {
    this.reason = reason;
    return this;
  }

  public BillingWorkflowState withStatusCode(final int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public String getReason() {
    return this.reason;
  }
  public int getStatusCode() { return this.statusCode; }
}

