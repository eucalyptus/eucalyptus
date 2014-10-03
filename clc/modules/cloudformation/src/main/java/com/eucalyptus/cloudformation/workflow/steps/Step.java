package com.eucalyptus.cloudformation.workflow.steps;

import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.resources.ResourceAction;

/**
 * Created by ethomas on 9/28/14.
 */
public interface Step extends Nameable{
  public ResourceAction perform(ResourceAction resourceAction) throws Exception;
  public RetryPolicy getRetryPolicy();
}
