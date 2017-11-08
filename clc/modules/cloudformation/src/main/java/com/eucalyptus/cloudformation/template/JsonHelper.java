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
