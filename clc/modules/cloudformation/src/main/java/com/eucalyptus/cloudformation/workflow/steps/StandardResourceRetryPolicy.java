/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.workflow.steps;

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.workflow.NotAResourceFailureException;
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
