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
      resourceInfo.setResourceAttributeJson(attributeName, attributeNode.get(attributeName).asText());
    }
  }

  public static String getResourceAttributesJson(ResourceInfo resourceInfo) throws CloudFormationException {
    Collection<String> attributeNames = resourceInfo.getAttributeNames();
    ObjectNode attributesNode = JsonHelper.createObjectNode();
    if (attributeNames != null) {
      for (String attributeName: attributeNames) {
        attributesNode.put(attributeName, resourceInfo.getResourceAttributeJson(attributeName));
      }
    }
    return JsonHelper.getStringFromJsonNode(attributesNode);
  }

}
