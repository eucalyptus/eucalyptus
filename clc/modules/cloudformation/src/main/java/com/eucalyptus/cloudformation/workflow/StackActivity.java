package com.eucalyptus.cloudformation.workflow;

/**
 * Created by ethomas on 2/18/14.
 */
public interface StackActivity {
  public String createResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson);
  public String deleteResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason);
  public String finalizeCreateStack(String stackId, String accountId);
  public String logException(Throwable t);
  public String logInfo(String message);
  public String deleteAllStackRecords(String stackId, String accountId);
}
