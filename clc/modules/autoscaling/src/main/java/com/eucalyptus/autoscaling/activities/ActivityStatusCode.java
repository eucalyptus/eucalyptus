/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.autoscaling.activities;

import javax.annotation.Nullable;

/**
 * Scaling activity status codes.
 */
public enum ActivityStatusCode {

  WaitingForSpotInstanceRequestId,
  
  WaitingForSpotInstanceId,
  
  WaitingForInstanceId,
  
  PreInService,
  
  InProgress,
  
  Successful,
  
  Failed,
  
  Cancelled;

  /**
   * Get an optional description for the status code.
   * 
   * @return The description or null.
   */
  @Nullable
  public String getDescription() {
    return null; 
  }
}
