package com.eucalyptus.cloudformation.workflow;

/**
 * Created by ethomas on 2/18/14.
 */
public interface DeleteStackWorkflow {
  public void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId);
}
