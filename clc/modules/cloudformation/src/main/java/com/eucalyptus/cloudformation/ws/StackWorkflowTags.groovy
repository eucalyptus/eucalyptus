package com.eucalyptus.cloudformation.ws

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import com.netflix.glisten.WorkflowTags
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 10/10/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class StackWorkflowTags extends WorkflowTags {
  String stackId;
  String stackName;
  String accountId;
  String accountName;
  StackWorkflowTags(String stackId, String stackName, String accountId, String accountName) {
    this.stackId = stackId;
    this.stackName = stackName;
    this.accountId = accountId;
    this.accountName = accountName;
  }

  private String truncate(String s, int i) {
    if (s == null) return null;
    if (s.length() > i) return s.substring(0, i);
    return s;
  }

  @Override
  @SuppressWarnings('CatchException')
  protected void populatePropertyFromJson(String json, String key) {
    DataConverter dataConverter = new JsonDataConverter()
    String valueString = null
    try {
      valueString = new ObjectMapper().readTree(json ?: '""').get(key).asText();
    } catch (Exception ignore) {
      // This is not the property we are looking for, no reason to fail
    }
    if (valueString) {
      Class type = hasProperty(key)?.type
      try {
        def value = valueString
        if (type != String) {
          value = dataConverter.fromData(valueString, type)
        }
        this."${key}" = value
      } catch (Exception ignore) {
        // Could not convert data so the property will not be populated
      }
    }
  }

  /**
   * @return tags based on the properties of this class that can be used in an SWF workflow
   */
  @Override
  List<String> constructTags() {
    List<String> retVal = Lists.newArrayList();
    retVal.add(truncate("StackId:"+stackId, 255));
    retVal.add(truncate("StackName:"+stackName, 255));
    retVal.add(truncate("AccountId:"+accountId, 255));
    retVal.add(truncate("AccountName:"+accountName, 255));
    return retVal;
  }

}
