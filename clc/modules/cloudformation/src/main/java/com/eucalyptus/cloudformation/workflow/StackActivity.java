package com.eucalyptus.cloudformation.workflow;

/**
 * Created by ethomas on 2/18/14.
 */
public interface StackActivity {
  //TODO: review return value
  public String createResource(String resourceId, String templateJson, String resourceMapJson);
  public void rollbackResource(String resourceId, String templateJson, String resourceMapJson);
  public void deleteResource(String resourceId, String templateJson, String resourceMapJson);
  public String updateTemplate(String stackId, String templateJson, String resourceMapJson);
  public void createInitialCreateStackEvent(String templateJson);
  public void createOutputs();

}
