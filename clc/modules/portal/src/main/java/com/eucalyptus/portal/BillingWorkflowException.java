/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

public class BillingWorkflowException extends Exception {
  private static final long serialVersionUID = 1L;
  private int statusCode = 500;

  public BillingWorkflowException() {
  }

  public BillingWorkflowException(final String message) {
    super(message);
  }

  public BillingWorkflowException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public BillingWorkflowException(final int statusCode) {
    this.statusCode = statusCode;
  }

  public BillingWorkflowException(final String message, final int statusCode) {
    this(message);
    this.statusCode = statusCode;
  }

  public BillingWorkflowException(String message, Throwable cause, final int statusCode) {
    this(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return this.statusCode;
  }
}