/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ethomas on 12/10/13.
 */
public class Template {

  public DependencyManager getResourceDependencyManager() {
    return resourceDependencyManager;
  }

  public void setResourceDependencyManager(DependencyManager resourceDependencyManager) {
    this.resourceDependencyManager = resourceDependencyManager;
  }

  private DependencyManager resourceDependencyManager = new DependencyManager();
  private String templateFormatVersion = "";
  private String description = "";
  private Map<String, Map<String, Map<String, String>>> mapping = Maps.newLinkedHashMap();
  private Map<String, List<String>> availabilityZoneMap = Maps.newLinkedHashMap();
  private Map<String, String> outputJsonMap = Maps.newLinkedHashMap();

  public Map<String, String> getOutputJsonMap() {
    return outputJsonMap;
  }

  public void setOutputJsonMap(Map<String, String> outputJsonMap) {
    this.outputJsonMap = outputJsonMap;
  }

  public Map<String, List<String>> getAvailabilityZoneMap() {
    return availabilityZoneMap;
  }

  public void setAvailabilityZoneMap(Map<String, List<String>> availabilityZoneMap) {
    this.availabilityZoneMap = availabilityZoneMap;
  }

  public Template() {
  }
  public enum ReferenceType {
    Parameter,
    PseudoParameter,
    Resource
  }

  public Map<String, Map<String, Map<String, String>>> getMapping() {
    return mapping;
  }

  public void setMapping(Map<String, Map<String, Map<String, String>>> mapping) {
    this.mapping = mapping;
  }

  public String getTemplateFormatVersion() {
    return templateFormatVersion;
  }

  public void setTemplateFormatVersion(String templateFormatVersion) {
    this.templateFormatVersion = templateFormatVersion;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Parameter> getParameterList() {
    return parameterList;
  }

  public void addParameter(Parameter parameter) {
    parameterList.add(parameter);
  }

  public void setParameterList(List<Parameter> parameterList) {
    this.parameterList = parameterList;
  }

  private List<Parameter> parameterList = Lists.newArrayList();

  public Map<String, ResourceInfo> getResourceMap() {
    return resourceMap;
  }

  public void setResourceMap(Map<String, ResourceInfo> resourceMap) {
    this.resourceMap = resourceMap;
  }

  private Map<String, ResourceInfo> resourceMap = Maps.newHashMap();


  private Map<String, Reference> referenceMap = Maps.newHashMap();

  public Map<String, Reference> getReferenceMap() {
    return referenceMap;
  }

  public void setReferenceMap(Map<String, Reference> referenceMap) {
    this.referenceMap = referenceMap;
  }

  private Map<String, Boolean> conditionMap = Maps.newHashMap();

  public Map<String, Boolean> getConditionMap() {
    return conditionMap;
  }

  public void setConditionMap(Map<String, Boolean> conditionMap) {
    this.conditionMap = conditionMap;
  }

  public static class Reference {
    String referenceName;
    String referenceValueJson;
    ReferenceType referenceType;
    boolean isReady;

    public Reference() {
    }

    public String getReferenceName() {
      return referenceName;
    }

    public void setReferenceName(String referenceName) {
      this.referenceName = referenceName;
    }

    public String getReferenceValueJson() {
      return referenceValueJson;
    }

    public void setReferenceValueJson(String referenceValueJson) {
      this.referenceValueJson = referenceValueJson;
    }

    public ReferenceType getReferenceType() {
      return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
      this.referenceType = referenceType;
    }

    public boolean isReady() {
      return isReady;
    }

    public void setReady(boolean isReady) {
      this.isReady = isReady;
    }

    @Override
    public String toString() {
      return "Reference{" +
        "referenceName='" + referenceName + '\'' +
        ", referenceValueJson='" + referenceValueJson + '\'' +
        ", referenceType=" + referenceType +
        ", isReady=" + isReady +
        '}';
    }
  }

  @Override
  public String toString() {
    return "Template{" +
      "resourceDependencyManager=" + resourceDependencyManager +
      ", templateFormatVersion='" + templateFormatVersion + '\'' +
      ", description='" + description + '\'' +
      ", mapping=" + mapping +
      ", availabilityZoneMap=" + availabilityZoneMap +
      ", outputJsonMap=" + outputJsonMap +
      ", parameterList=" + parameterList +
      ", resourceMap=" + resourceMap +
      ", referenceMap=" + referenceMap +
      ", conditionMap=" + conditionMap +
      '}';
  }

  public JsonNode toJsonNode() throws CloudFormationException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    objectNode.put("resourceDependencyManager", resourceDependencyManagerToJsonNode(resourceDependencyManager));
    objectNode.put("templateFormatVersion", templateFormatVersion);
    objectNode.put("description", description);
    objectNode.put("mapping", mappingToJsonNode(mapping));
    objectNode.put("availabilityZoneMap", availabilityZoneMapToJsonNode(availabilityZoneMap));
    objectNode.put("outputJsonMap", outputJsonMapToJsonNode(outputJsonMap));
    objectNode.put("parameterList", parameterListToJsonNode(parameterList));
    objectNode.put("resourceMap", resourceMapToJsonNode(resourceMap));
    objectNode.put("referenceMap", referenceMapToJsonNode(referenceMap));
    objectNode.put("conditionMap", conditionMapToJsonNode(conditionMap));
    return objectNode;
  }

  public static Template fromJsonNode(JsonNode jsonNode) throws CloudFormationException {
    if (jsonNode == null) return null;
    try {
      Template template = new Template();
      template.setResourceDependencyManager(jsonNodeToResourceDependencyManager(jsonNode.get("resourceDependencyManager")));
      template.setTemplateFormatVersion(jsonNode.get("templateFormatVersion").textValue());
      template.setDescription(jsonNode.get("description").textValue());
      template.setMapping(jsonNodeToMapping(jsonNode.get("mapping")));
      template.setAvailabilityZoneMap(jsonNodeToAvailabilityZoneMap(jsonNode.get("availabilityZoneMap")));
      template.setOutputJsonMap(jsonNodeToOutputJsonMap(jsonNode.get("outputJsonMap")));
      template.setParameterList(jsonNodeToParameterList(jsonNode.get("parameterList")));
      template.setResourceMap(jsonNodeToResourceMap(jsonNode.get("resourceMap")));
      template.setReferenceMap(jsonNodeToReferenceMap(jsonNode.get("referenceMap")));
      template.setConditionMap(jsonNodeToConditionMap(jsonNode.get("conditionMap")));
      return template;
    } catch (Exception e) {
      throw new InternalFailureException(e.getMessage());
    }
  }



  private JsonNode conditionMapToJsonNode(Map<String, Boolean> conditionMap) {
    if (conditionMap == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (String key: outputJsonMap.keySet()) {
      objectNode.put(key, outputJsonMap.get(key));
    }
    return objectNode;
  }

  private static Map<String, Boolean> jsonNodeToConditionMap(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    Map<String, Boolean> conditionMap = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      conditionMap.put(key, jsonNode.get(key).booleanValue());
    }
    return conditionMap;
  }

  private JsonNode referenceMapToJsonNode(Map<String, Reference> referenceMap) {
    if (referenceMap == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (String key: referenceMap.keySet()) {
      Reference reference = referenceMap.get(key);
      if (reference == null) {
        objectNode.put(key, (JsonNode) null);
      } else {
        ObjectNode referenceNode = mapper.createObjectNode();
        referenceNode.put("referenceName", reference.getReferenceName());
        referenceNode.put("referenceValueJson", reference.getReferenceValueJson());
        referenceNode.put("referenceType", reference.getReferenceType().toString());
        referenceNode.put("isReady", reference.isReady());
        objectNode.put(key, referenceNode);
      }
    }
    return objectNode;
  }

  private static Map<String, Reference> jsonNodeToReferenceMap(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    Map<String, Reference> referenceMap = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      JsonNode referenceNode = jsonNode.get(key);
      Reference reference = new Reference();
      reference.setReady(referenceNode.get("isReady").booleanValue());
      reference.setReferenceName(referenceNode.get("referenceName").textValue());
      reference.setReferenceType(ReferenceType.valueOf(referenceNode.get("referenceType").textValue()));
      reference.setReferenceValueJson(referenceNode.get("referenceValueJson").textValue());
      referenceMap.put(key, reference);
    }
    return referenceMap;
  }


  private JsonNode resourceMapToJsonNode(Map<String, ResourceInfo> resourceMap) throws CloudFormationException {
    if (resourceMap == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (String key: resourceMap.keySet()) {
      ResourceInfo resourceInfo = resourceMap.get(key);
      if (resourceInfo == null) {
        objectNode.put(key, (JsonNode) null);
      } else {
        ObjectNode resourceNode = mapper.createObjectNode();
        resourceNode.put("accountId", resourceInfo.getAccountId());
        resourceNode.put("type", resourceInfo.getType());
        resourceNode.put("allowedByCondition", resourceInfo.getAllowedByCondition());
        resourceNode.put("deletionPolicy", resourceInfo.getDeletionPolicy());
        resourceNode.put("effectiveUserId", resourceInfo.getEffectiveUserId());
        resourceNode.put("logicalResourceId", resourceInfo.getLogicalResourceId());
        resourceNode.put("metadataJson", resourceInfo.getMetadataJson());
        resourceNode.put("physicalResourceId", resourceInfo.getPhysicalResourceId());
        resourceNode.put("propertiesJson", resourceInfo.getPropertiesJson());
        resourceNode.put("referenceValueJson", resourceInfo.getReferenceValueJson());
        resourceNode.put("updatePolicyJson", resourceInfo.getUpdatePolicyJson());
        Collection<String> attributeNames = resourceInfo.getAttributeNames();
        if (attributeNames == null) {
          resourceNode.put("attributes", (JsonNode) null);
        } else {
          ObjectNode attributesNode = mapper.createObjectNode();
          for (String attributeName: attributeNames) {
            attributesNode.put(attributeName, resourceInfo.getResourceAttributeJson(attributeName));
          }
          resourceNode.put("attributes", attributesNode);
        }
        objectNode.put(key, resourceNode);
      }
    }
    return objectNode;
  }

  private static Map<String, ResourceInfo> jsonNodeToResourceMap(JsonNode jsonNode) throws CloudFormationException {
    if (jsonNode == null) return null;
    Map<String, ResourceInfo> resourceMap = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      JsonNode resourceNode = jsonNode.get(key);
      String type = resourceNode.get("type").textValue();
      ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(type);
      resourceInfo.setAccountId(resourceNode.get("accountId").textValue());
      resourceInfo.setAllowedByCondition(resourceNode.get("allowedByCondition").booleanValue());
      resourceInfo.setDeletionPolicy(resourceNode.get("deletionPolicy").textValue());
      resourceInfo.setEffectiveUserId(resourceNode.get("effectiveUserId").textValue());
      resourceInfo.setLogicalResourceId(resourceNode.get("logicalResourceId").textValue());
      resourceInfo.setMetadataJson(resourceNode.get("metadataJson").textValue());
      resourceInfo.setPhysicalResourceId(resourceNode.get("physicalResourceId").textValue());
      resourceInfo.setPropertiesJson(resourceNode.get("propertiesJson").textValue());
      resourceInfo.setReferenceValueJson(resourceNode.get("referenceValueJson").textValue());
      resourceInfo.setUpdatePolicyJson(resourceNode.get("updatePolicyJson").textValue());
      ObjectNode attributeNode = (ObjectNode) resourceNode.get("attributes");
      for (String attributeName: Lists.newArrayList(attributeNode.fieldNames())) {
        resourceInfo.setResourceAttributeJson(attributeName, attributeNode.get(attributeName).textValue());
      }
      resourceMap.put(key, resourceInfo);
    }
    return resourceMap;
  }

  private JsonNode parameterListToJsonNode(List<Parameter> parameterList) {
    if (parameterList == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (Parameter parameter: parameterList) {
      objectNode.put(parameter.getParameterKey(), parameter.getParameterValue());
    }
    return objectNode;
  }

  private static List<Parameter> jsonNodeToParameterList(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    List<Parameter> parameterList = Lists.newArrayList();
    for (String parameterName: Lists.newArrayList(jsonNode.fieldNames())) {
      Parameter parameter = new Parameter();
      parameter.setParameterKey(parameterName);
      parameter.setParameterValue(jsonNode.get(parameterName).textValue());
      parameterList.add(parameter);
    }
    return parameterList;
  }

  private JsonNode outputJsonMapToJsonNode(Map<String, String> outputJsonMap) {
    if (outputJsonMap == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (String key: outputJsonMap.keySet()) {
      objectNode.put(key, outputJsonMap.get(key));
    }
    return objectNode;
  }

  private static Map<String, String> jsonNodeToOutputJsonMap(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    Map<String, String> outputJsonMap = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      outputJsonMap.put(key, jsonNode.get(key).textValue());
    }
    return outputJsonMap;
  }

  private JsonNode availabilityZoneMapToJsonNode(Map<String,List<String>> availabilityZoneMap) {
    if (availabilityZoneMap == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    for (String key: availabilityZoneMap.keySet()) {
      List<String> values = availabilityZoneMap.get(key);
      if (values == null) {
        objectNode.put(key, (JsonNode) null);
      } else {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (String value: values) {
          arrayNode.add(value);
        }
        objectNode.put(key, arrayNode);
      }
    }
    return objectNode;
  }

  private static Map<String, List<String>> jsonNodeToAvailabilityZoneMap(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    Map<String, List<String>> availabilityZoneMap = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      ArrayNode arrayNode = (ArrayNode) jsonNode.get(key);
      if (arrayNode == null) {
        availabilityZoneMap.put(key, null);
      } else {
        List<String> valueList = Lists.newArrayList();
        for (int i=0;i<arrayNode.size();i++) {
          valueList.add(arrayNode.get(i).textValue());
        }
        availabilityZoneMap.put(key, valueList);
      }
    }
    return availabilityZoneMap;
  }

  private JsonNode mappingToJsonNode(Map<String, Map<String, Map<String, String>>> mapping) {
    if (mapping == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode1 = mapper.createObjectNode();
    for (String key1: mapping.keySet()) {
      Map<String, Map<String, String>> val1 = mapping.get(key1);
      if (val1 == null) {
        objectNode1.put(key1, (JsonNode) null);
      } else {
        ObjectNode objectNode2 = mapper.createObjectNode();
        for (String key2: val1.keySet()) {
          Map<String, String> val2 = val1.get(key2);
          if (val2 == null) {
            objectNode2.put(key2, (JsonNode) null);
          } else {
            ObjectNode objectNode3 = mapper.createObjectNode();
            for (String key3: val2.keySet()) {
              objectNode3.put(key3, val2.get(key3));
            }
            objectNode2.put(key2, objectNode3);
          }
        }
        objectNode1.put(key1, objectNode2);
      }
    }
    return objectNode1;
  }

  private static Map<String, Map<String, Map<String, String>>> jsonNodeToMapping(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    Map<String, Map<String, Map<String, String>>> mapping = Maps.newLinkedHashMap();
    for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
      JsonNode jsonNode1 = jsonNode.get(key);
      if (jsonNode1 == null) {
        mapping.put(key, null);
      } else {
        Map<String, Map<String, String>> mapping1 = Maps.newLinkedHashMap();
        for (String key1: Lists.newArrayList(jsonNode1.fieldNames())) {
          JsonNode jsonNode2 = jsonNode1.get(key1);
          if (jsonNode2 == null) {
            mapping1.put(key1, null);
          } else {
            Map<String, String> mapping2 = Maps.newLinkedHashMap();
            for (String key2: Lists.newArrayList(jsonNode2.fieldNames())) {
              mapping2.put(key2, jsonNode2.get(key2).textValue());
            }
            mapping1.put(key1, mapping2);
          }
        }
        mapping.put(key, mapping1);
      }
    }
    return mapping;
  }

  private JsonNode resourceDependencyManagerToJsonNode(DependencyManager dependencyManager) {
    if (dependencyManager == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode objectNode = mapper.createObjectNode();
    Set<String> nodes = dependencyManager.getNodes();
    if (nodes == null) {
      objectNode.put("nodes", (JsonNode) null);
    } else {
      ArrayNode arrayNode = mapper.createArrayNode();
      for (String node: nodes) {
        arrayNode.add(node);
      }
      objectNode.put("nodes", arrayNode);
    }
    Multimap<String, String> outEdges = dependencyManager.getOutEdges();
    if (outEdges == null) {
      objectNode.put("outEdges", (JsonNode) null);
    } else {
      ObjectNode outEdgesNode = mapper.createObjectNode();
      for (String key: outEdges.keySet()) {
        Collection<String> values = outEdges.get(key);
        if (values == null) {
          outEdgesNode.put(key, (JsonNode) null);
        } else {
          ArrayNode arrayNode = mapper.createArrayNode();
          for (String value: values) {
            arrayNode.add(value);
          }
          outEdgesNode.put(key, arrayNode);
        }
      }
      objectNode.put("outEdges", outEdgesNode);
    }
    return objectNode;
  }

  private static DependencyManager jsonNodeToResourceDependencyManager(JsonNode jsonNode) {
    if (jsonNode == null) return null;
    DependencyManager resourceDependencyManager = new DependencyManager();
    ArrayNode arrayNode = (ArrayNode) jsonNode.get("nodes");
    if (arrayNode == null) {
      resourceDependencyManager.setNodes(null);
    } else {
      Set<String> nodes = Sets.newLinkedHashSet();
      for (int i=0;i<arrayNode.size();i++) {
        nodes.add(arrayNode.get(i).textValue());
      }
      resourceDependencyManager.setNodes(nodes);
    }
    JsonNode outEdgesNode = jsonNode.get("outEdges");
    if (outEdgesNode == null) {
      resourceDependencyManager.setOutEdges(null);
    } else {
      Multimap<String, String> outEdges = TreeMultimap.create(); // sorted so consistent dependency list result
      for (String key: Lists.newArrayList(outEdgesNode.fieldNames())) {
        ArrayNode arrayNode1 = (ArrayNode) outEdgesNode.get(key);
        if (arrayNode1 != null) {
          for (int i=0;i<arrayNode1.size();i++) {
            outEdges.put(key, arrayNode1.get(i).textValue());
          }
        }
      }
      resourceDependencyManager.setOutEdges(outEdges);
    }
    return resourceDependencyManager;
  }

}
