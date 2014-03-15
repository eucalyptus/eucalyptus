package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by ethomas on 1/28/14.
 */
public class JsonHelper {

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
    if (jsonNode != null && !jsonNode.isTextual()) {
      throw error(errorMsg);
    }
    return jsonNode;
  }

  public static JsonNode checkString(JsonNode parent, int index, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(index);
    if (jsonNode != null && !jsonNode.isTextual()) {
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
    return (node == null) ? null : node.textValue();
  }

  private static Double getDouble(JsonNode node) {
    return (node == null) ? null : node.doubleValue();
  }


  public static JsonNode checkDouble(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = checkString(parent, key, errorMsg);
    try {
      if (jsonNode != null) {
        Double.parseDouble(jsonNode.textValue());
      }
    } catch (NullPointerException | NumberFormatException ex) {
      throw error(errorMsg);
    }
    return jsonNode;
  }
  public static JsonNode checkDouble(JsonNode parent, String key) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isTextual()) {
      throw error(errorMsg(key, "number"));
    }
    try {
      if (jsonNode != null) {
        Double.parseDouble(jsonNode.textValue());
      }
    } catch (NumberFormatException ex) {
      throw error(errorMsg(key, "number (" + jsonNode.textValue() + ")"));
    }
    return jsonNode;
  }

  public static JsonNode checkStringOrArray(JsonNode parent, String key, String errorMsg) throws CloudFormationException {
    JsonNode jsonNode = parent.get(key);
    if (jsonNode != null && !jsonNode.isTextual() && !jsonNode.isArray()) {
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
    return (jsonNode == null || jsonNode.isTextual() && jsonNode.textValue() == null) ? null : jsonNode.toString();
  }

}
