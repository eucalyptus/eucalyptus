/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.workflow.NotAResourceFailureException;
import com.eucalyptus.configurable.ConfigurableField;
import com.google.common.collect.Lists;

import java.util.Collection;

/**
 * Created by ethomas on 10/2/14.
 */
public class StandardResourceRetryPolicy {
  private Integer retryExpirationIntervalSeconds;

  public StandardResourceRetryPolicy(Integer retryExpirationIntervalSeconds) {
    this.retryExpirationIntervalSeconds = retryExpirationIntervalSeconds;
  }

  public RetryPolicy getPolicy() {
    Collection<Class<? extends Throwable>> exceptionList = Lists.newArrayList();
    exceptionList.add(NotAResourceFailureException.class);
    ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(1L).withExceptionsToRetry(exceptionList);
    if (retryExpirationIntervalSeconds != null && retryExpirationIntervalSeconds > 0) {
      retryPolicy.setRetryExpirationIntervalSeconds(retryExpirationIntervalSeconds);
    }
    // TODO: maybe I should let max interval not be too bad
    retryPolicy.setMaximumRetryIntervalSeconds(30L);
    return retryPolicy;

  }
}
