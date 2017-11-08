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

package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.Collection;

public class ResourceInfoHelper {
  private static final Logger LOG = Logger.getLogger(ResourceInfoHelper.class);

  public static void setResourceAttributesJson(ResourceInfo resourceInfo, String json) throws CloudFormationException {
    JsonNode attributeNode = JsonHelper.getJsonNodeFromString(json);
    for (String attributeName: Lists.newArrayList(attributeNode.fieldNames())) {
      if (resourceInfo.getAttributeNames().contains(attributeName)) {
        resourceInfo.setResourceAttributeJson(attributeName, attributeNode.get(attributeName).asText());
      } else {
        LOG.warn("Attempting to set non-existent attribute '" + attributeName + "' on resource " + resourceInfo.getLogicalResourceId() + " (" + resourceInfo.getType() + "). " +
          "Perhaps this attribute previously existed?  Ignoring this request.");
      }
    }
  }

  public static String getResourceAttributesJson(ResourceInfo resourceInfo) throws CloudFormationException {
    Collection<String> attributeNames = resourceInfo.getAttributeNames();
    ObjectNode attributesNode = JsonHelper.createObjectNode();
    if (attributeNames != null) {
      for (String attributeName: attributeNames) {
        String resourceAttributeJson = resourceInfo.getResourceAttributeJson(attributeName);
        if (resourceAttributeJson == null) {
          continue; // no real point in setting a value to null (which is default)
        }
        attributesNode.put(attributeName, resourceAttributeJson);
      }
    }
    return JsonHelper.getStringFromJsonNode(attributesNode);
  }

}
