/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.common.msgs.Tag;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StackEntityHelper {

  private static ObjectMapper mapper = new ObjectMapper();

  public static ArrayList<String> jsonToCapabilities(String capabilitiesJson) throws CloudFormationException {
    try {
      return capabilitiesJson == null ? Lists.<String>newArrayList() :
        (ArrayList<String>) mapper.readValue(capabilitiesJson, new TypeReference<ArrayList<String>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String capabilitiesToJson(ArrayList<String> capabilities) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(capabilities == null ? Lists.<String>newArrayList() : capabilities);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static ArrayList<String> jsonToNotificationARNs(String notificationARNsJson) throws CloudFormationException {
    try {
      return notificationARNsJson == null ? Lists.<String>newArrayList() :
        (ArrayList<String>) mapper.readValue(notificationARNsJson, new TypeReference<ArrayList<String>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String notificationARNsToJson(ArrayList<String> notificationARNs) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(notificationARNs == null ? Lists.<String>newArrayList() : notificationARNs);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static ArrayList<StackEntity.Output> jsonToOutputs(String outputsJson) throws CloudFormationException {
    try {
      return outputsJson == null ? Lists.<StackEntity.Output>newArrayList() :
        (ArrayList<StackEntity.Output>) mapper.readValue(outputsJson, new TypeReference<ArrayList<StackEntity.Output>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String outputsToJson(List<StackEntity.Output> outputs) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(outputs == null ? Lists.<StackEntity.Output>newArrayList() : outputs);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static ArrayList<StackEntity.Parameter> jsonToParameters(String parametersJson) throws CloudFormationException {
    try {
      return parametersJson == null ? Lists.<StackEntity.Parameter>newArrayList() :
        (ArrayList<StackEntity.Parameter>) mapper.readValue(parametersJson,
          new TypeReference<ArrayList<StackEntity.Parameter>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static Map<String, StackEntity.Parameter> jsonToParameterMap(String parametersJson) throws CloudFormationException {
    Map<String, StackEntity.Parameter> map = Maps.newLinkedHashMap();
    for (StackEntity.Parameter parameter: jsonToParameters(parametersJson)) {
      map.put(parameter.getKey(), parameter);
    }
    return map;
  }


  public static String parametersToJson(ArrayList<StackEntity.Parameter> parameters) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(parameters == null ? Lists.<StackEntity.Parameter>newArrayList() : parameters);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static ArrayList<Tag> jsonToTags(String tagsJson) throws CloudFormationException {
    try {
      return tagsJson == null ? Lists.<Tag>newArrayList() :
        (ArrayList<Tag>) mapper.readValue(tagsJson, new TypeReference<ArrayList<Tag>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String tagsToJson(ArrayList<Tag> tags) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(tags == null ? Lists.<Tag>newArrayList() : tags);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static Map<String, String> jsonToPseudoParameterMap(String pseudoParameterMapJson) throws CloudFormationException {
    try {
      return pseudoParameterMapJson == null ? Maps.<String, String>newLinkedHashMap() :
        (Map<String, String>) mapper.readValue(pseudoParameterMapJson, new TypeReference<LinkedHashMap<String, String>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String pseudoParameterMapToJson(Map<String, String> pseudoParameterMap) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(pseudoParameterMap == null ? Maps.<String, String>newLinkedHashMap() : pseudoParameterMap);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }


  public static Map<String, Boolean> jsonToConditionMap(String conditionMapJson) throws CloudFormationException {
    try {
      return conditionMapJson == null ? Maps.<String, Boolean>newLinkedHashMap() :
        (Map<String, Boolean>) mapper.readValue(conditionMapJson, new TypeReference<LinkedHashMap<String, Boolean>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String conditionMapToJson(Map<String, Boolean> conditionMap) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(conditionMap == null ? Maps.<String, Boolean>newLinkedHashMap() : conditionMap);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static Map<String, Map<String, Map<String, String>>> jsonToMapping(String mappingJson) throws CloudFormationException {
    try {
      return mappingJson == null ? Maps.<String, Map<String, Map<String, String>>>newLinkedHashMap() :
        (Map<String, Map<String, Map<String, String>>>)
          mapper.readValue(mappingJson, new TypeReference<LinkedHashMap<String, Map<String, Map<String, String>>>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String mappingToJson(Map<String, Map<String, Map<String, String>>> mapping) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(mapping == null ? Maps.<String, Map<String, Map<String, String>>>newLinkedHashMap() : mapping);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static DependencyManager jsonToResourceDependencyManager(String resourceDependencyManagerJson)
    throws CloudFormationException {
    return DependencyManager.fromJson(resourceDependencyManagerJson);
  }

  public static String resourceDependencyManagerToJson(DependencyManager resourceDependencyManager)
    throws CloudFormationException {
    return resourceDependencyManager.toJson();
  }

  public static void populateTemplateWithStackEntity(Template template, VersionedStackEntity stackEntity) throws CloudFormationException {
    template.setDescription(stackEntity.getDescription());
    template.setPseudoParameterMap(jsonToPseudoParameterMap(stackEntity.getPseudoParameterMapJson()));
    template.setTemplateFormatVersion(stackEntity.getTemplateFormatVersion());
    template.setMapping(jsonToMapping(stackEntity.getMappingJson()));
    template.setParameters(jsonToParameters(stackEntity.getParametersJson()));
    template.setConditionMap(jsonToConditionMap(stackEntity.getConditionMapJson()));
    template.setResourceDependencyManager(jsonToResourceDependencyManager(stackEntity.getResourceDependencyManagerJson()));
    template.setWorkingOutputs(jsonToOutputs(stackEntity.getWorkingOutputsJson()));
  }

  public static void populateStackEntityWithTemplate(VersionedStackEntity stackEntity, Template template) throws CloudFormationException {
    stackEntity.setDescription(template.getDescription());
    stackEntity.setPseudoParameterMapJson(pseudoParameterMapToJson(template.getPseudoParameterMap()));
    stackEntity.setTemplateFormatVersion(template.getTemplateFormatVersion());
    stackEntity.setMappingJson(mappingToJson(template.getMapping()));
    stackEntity.setParametersJson(parametersToJson(template.getParameters()));
    stackEntity.setConditionMapJson(conditionMapToJson(template.getConditionMap()));
    stackEntity.setResourceDependencyManagerJson(resourceDependencyManagerToJson(template.getResourceDependencyManager()));
    stackEntity.setWorkingOutputsJson(outputsToJson(template.getWorkingOutputs()));
  }
}
