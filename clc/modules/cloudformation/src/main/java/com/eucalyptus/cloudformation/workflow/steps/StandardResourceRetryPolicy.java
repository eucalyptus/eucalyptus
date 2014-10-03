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
    return retryPolicy;

  }
}
