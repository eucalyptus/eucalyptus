package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.Tag;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 3/7/14.
 */
public class StackEntityHelper {

  private static ObjectMapper mapper = new ObjectMapper();

  public static Map<String, List<String>> jsonToAvailabilityZoneMap(String availabilityZoneMapJson) throws CloudFormationException {
    try {
      return availabilityZoneMapJson == null ? Maps.<String, List<String>>newLinkedHashMap() :
        (Map<String, List<String>>) mapper.readValue(availabilityZoneMapJson, new TypeReference<LinkedHashMap<String, List<String>>>(){});
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String availabilityZoneMapToJson(Map<String, List<String>> availabilityZoneMap) throws CloudFormationException {
    try {
      return mapper.writeValueAsString(availabilityZoneMap == null ? Maps.<String, List<String>>newLinkedHashMap() : availabilityZoneMap);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

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

  public static String outputsToJson(ArrayList<StackEntity.Output> outputs) throws CloudFormationException {
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
}
