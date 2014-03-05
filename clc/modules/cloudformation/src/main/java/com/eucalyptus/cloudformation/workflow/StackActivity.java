package com.eucalyptus.cloudformation.workflow;

/**
 * Created by ethomas on 2/18/14.
 */
public interface StackActivity {
  public String createResource(String resourceId, String templateJson, String resourceMapJson);
  public String deleteResource(String resourceId, String templateJson, String resourceMapJson);
  public String updateTemplate(String templateJson, String resourceMapJson);
  public String createGlobalStackEvent(String templateJson, String resourceStatus, String resourceStatusReason);
  public String finalizeCreateStack(String templateJson, String resourceInfoMapJson);
  public String logException(Throwable t);
  public String deleteAllStackRecords(String templateJson);
}
