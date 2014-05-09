/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceInfoHelper {
  private static final Logger LOG = Logger.getLogger(ResourceInfoHelper.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  public static ResourceInfo jsonToResourceInfo(String json) throws CloudFormationException {
    JsonNode resourceNode = JsonHelper.getJsonNodeFromString(json);
    LOG.info("resourceNode="+resourceNode);
    LOG.info("resourceNode.get(\"type\")="+resourceNode.get("type"));
    String type = resourceNode.get("type").textValue();
    ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(type);
    resourceInfo.setAccountId(resourceNode.get("accountId").textValue());
    resourceInfo.setAllowedByCondition(resourceNode.get("allowedByCondition").booleanValue());
    resourceInfo.setDescription(resourceNode.get("description").textValue());
    resourceInfo.setDeletionPolicy(resourceNode.get("deletionPolicy").textValue());
    resourceInfo.setEffectiveUserId(resourceNode.get("effectiveUserId").textValue());
    resourceInfo.setLogicalResourceId(resourceNode.get("logicalResourceId").textValue());
    resourceInfo.setMetadataJson(resourceNode.get("metadataJson").textValue());
    resourceInfo.setPhysicalResourceId(resourceNode.get("physicalResourceId").textValue());
    resourceInfo.setPropertiesJson(resourceNode.get("propertiesJson").textValue());
    resourceInfo.setReady(resourceNode.get("ready").booleanValue());
    resourceInfo.setReferenceValueJson(resourceNode.get("referenceValueJson").textValue());
    resourceInfo.setUpdatePolicyJson(resourceNode.get("updatePolicyJson").textValue());
    setResourceAttributesJson(resourceInfo, resourceNode.get("attributes").textValue());
    return resourceInfo;
  }

  public static void setResourceAttributesJson(ResourceInfo resourceInfo, String json) throws CloudFormationException {
    JsonNode attributeNode = JsonHelper.getJsonNodeFromString(json);
    for (String attributeName: Lists.newArrayList(attributeNode.fieldNames())) {
      resourceInfo.setResourceAttributeJson(attributeName, attributeNode.get(attributeName).textValue());
    }
  }

  public static String resourceInfoToJson(ResourceInfo resourceInfo) throws CloudFormationException {
    if (resourceInfo == null) {
      return null;
    } else {
      ObjectNode resourceNode = mapper.createObjectNode();
      resourceNode.put("accountId", resourceInfo.getAccountId());
      resourceNode.put("type", resourceInfo.getType());
      resourceNode.put("allowedByCondition", resourceInfo.getAllowedByCondition());
      resourceNode.put("deletionPolicy", resourceInfo.getDeletionPolicy());
      resourceNode.put("description", resourceInfo.getDescription());
      resourceNode.put("effectiveUserId", resourceInfo.getEffectiveUserId());
      resourceNode.put("logicalResourceId", resourceInfo.getLogicalResourceId());
      resourceNode.put("metadataJson", resourceInfo.getMetadataJson());
      resourceNode.put("physicalResourceId", resourceInfo.getPhysicalResourceId());
      resourceNode.put("propertiesJson", resourceInfo.getPropertiesJson());
      resourceNode.put("ready", resourceInfo.getReady());
      resourceNode.put("referenceValueJson", resourceInfo.getReferenceValueJson());
      resourceNode.put("updatePolicyJson", resourceInfo.getUpdatePolicyJson());
      Collection<String> attributeNames = resourceInfo.getAttributeNames();
      if (attributeNames == null) {
        resourceNode.put("attributes", (String) null);
      } else {
        resourceNode.put("attributes", getResourceAttributesJson(resourceInfo));
      }
      return JsonHelper.getStringFromJsonNode(resourceNode);
    }
  }

  public static String getResourceAttributesJson(ResourceInfo resourceInfo) throws CloudFormationException {
    ObjectMapper mapper = new ObjectMapper();
    Collection<String> attributeNames = resourceInfo.getAttributeNames();
    ObjectNode attributesNode = mapper.createObjectNode();
    if (attributeNames != null) {
      for (String attributeName: attributeNames) {
        attributesNode.put(attributeName, resourceInfo.getResourceAttributeJson(attributeName));
      }
    }
    return JsonHelper.getStringFromJsonNode(attributesNode);
  }

  public static String resourceInfoMapToJson(Map<String, ResourceInfo> resourceInfoMap) throws CloudFormationException {
    try {
      Map<String, String> resourceInfoJsonMap = Maps.newLinkedHashMap();
      if (resourceInfoMap != null) {
        for (String key: resourceInfoMap.keySet()) {
          resourceInfoJsonMap.put(key, resourceInfoToJson(resourceInfoMap.get(key)));
        }
      }
      return mapper.writeValueAsString(resourceInfoMap);
    } catch (JsonProcessingException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static Map<String, ResourceInfo> jsonToResourceInfoMap(String resourceInfoMapJson) throws CloudFormationException {
    try {
      Map<String, String> resourceInfoJsonMap = (resourceInfoMapJson == null ? Maps.<String, String>newLinkedHashMap() :
        (Map<String, String>) mapper.readValue(resourceInfoMapJson, new TypeReference<LinkedHashMap<String, String>>(){}));
      Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
      for (String key: resourceInfoJsonMap.keySet()) {
        resourceInfoMap.put(key, jsonToResourceInfo(resourceInfoJsonMap.get(key)));
      }
      return resourceInfoMap;
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }
}
