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

package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class JsonHelper {

  private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

  public static JsonNode checkObject(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isObject()) {
      throw error(errorMsg);
    }
    return jsonNode;
  }

  public static JsonNode checkObject(JsonNode parent, String key) throws CloudFormationException {
    return checkObject(parent, key, errorMsg(key, "JSON object"));
  }

  public static JsonNode checkArray(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isArray()) {
      throw error(errorMsg);
    }
    return jsonNode;

  }
  public static JsonNode checkArray(JsonNode parent, String key) throws CloudFormationException {
    return checkArray(parent, key, errorMsg(key, "list"));
  }

  public static JsonNode checkString(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isValueNode()) {
      throw error(errorMsg);
    }
    return jsonNode;
  }

  public static JsonNode checkString(JsonNode parent, int index, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(index);
    if (jsonNode != null && !jsonNode.isValueNode()) {
      throw error(errorMsg);
    }
    return jsonNode;
  }

  public static JsonNode checkString(JsonNode parent, String key) throws CloudFormationException {
    return checkString(parent, key, errorMsg(key, "string"));
  }

  public static String getString(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    return getString(checkString(parent, key, errorMsg));
  }

  public static String getString(JsonNode parent, int index, String errorMsg) throws CloudFormationException {
    return getString(checkString(parent, index, errorMsg));
  }

  public static String getString(JsonNode parent, String key) throws CloudFormationException {
    return getString(checkString(parent, key));
  }

  public static Double getDouble(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    return getDouble(checkDouble(parent, key, errorMsg));
  }
  public static Double getDouble(JsonNode parent, String key) throws CloudFormationException {
    return getDouble(checkDouble(parent, key));
  }

  private static String getString(JsonNode node) {
    return (node == null) ? null : node.asText();
  }

  private static Double getDouble(JsonNode node) {
    return (node == null) ? null : Double.parseDouble(node.asText());
  }


  public static JsonNode checkDouble(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = checkString(parent, key, errorMsg);
    try {
      if (jsonNode != null) {
        Double.parseDouble(jsonNode.asText());
      }
    } catch (NullPointerException | NumberFormatException ex) {
      throw error(errorMsg);
    }
    return jsonNode;
  }
  public static JsonNode checkDouble(JsonNode parent, String key) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isValueNode()) {
      throw error(errorMsg(key, "number"));
    }
    try {
      if (jsonNode != null) {
        Double.parseDouble(jsonNode.asText());
      }
    } catch (NumberFormatException ex) {
      throw error(errorMsg(key, "number (" + jsonNode.asText() + ")"));
    }
    return jsonNode;
  }

  public static JsonNode checkStringOrArray(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isValueNode() && !jsonNode.isArray()) {
      throw error(errorMsg);
    }
    return jsonNode;
  }
  public static JsonNode checkStringOrArray(JsonNode parent, String key) throws CloudFormationException {
    return checkStringOrArray(parent, key, errorMsg(key, "String or a List"));
  }

  private static String errorMsg(String key, String valueType) {
    return "Every " + key + " member must be a " + valueType;
  }

  private static ValidationErrorException error(String errorMsg) {
    return new ValidationErrorException("Template format error: " + errorMsg);
  }

  public static JsonNode getJsonNodeFromString(String json) throws ValidationErrorException {
    if (json == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readTree(json);
    } catch (IOException e) {
      throw new ValidationErrorException(e.getMessage());
    }
  }

  public static String getStringFromJsonNode(JsonNode jsonNode) {
    return (jsonNode == null || jsonNode.isValueNode() && jsonNode.asText() == null) ? null : jsonNode.toString();
  }

  public static ArrayNode createArrayNode( ) {
    return nodeFactory.arrayNode( );
  }

  public static ObjectNode createObjectNode( ) {
    return nodeFactory.objectNode( );
  }
}
