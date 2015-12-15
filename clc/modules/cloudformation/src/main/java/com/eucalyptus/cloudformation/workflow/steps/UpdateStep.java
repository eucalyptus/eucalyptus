/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.workflow.steps;

import com.eucalyptus.cloudformation.resources.ResourceAction;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 9/28/14.
 */
public interface UpdateStep extends Nameable{
  Integer MAX_TIMEOUT = (int) TimeUnit.DAYS.toSeconds( 365 );

  ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception;

  /**
   * The timeout for the step in seconds.
   *
   * @return The timeout, or null if there is no wait for the step
   * @see #MAX_TIMEOUT
   */
  @Nullable
  Integer getTimeout();
}
