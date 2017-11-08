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
package com.eucalyptus.cloudformation.ws

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import com.netflix.glisten.WorkflowTags
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
