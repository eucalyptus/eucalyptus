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
package com.eucalyptus.loadbalancing.workflow;

public class LoadBalancingWorkflowException extends Exception {
  private static final long serialVersionUID = 1L;
  private int statusCode = 500;

  public LoadBalancingWorkflowException() { }

  public LoadBalancingWorkflowException(final String message) {
    super(message);
  }

  public LoadBalancingWorkflowException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public LoadBalancingWorkflowException(final int statusCode) {
    this.statusCode = statusCode;
  }

  public LoadBalancingWorkflowException(final String message, final int statusCode) {
    this(message);
    this.statusCode = statusCode;
  }

  public LoadBalancingWorkflowException(String message, Throwable cause, final int statusCode) {
    this(message, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return this.statusCode;
  }
}
