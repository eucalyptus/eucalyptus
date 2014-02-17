package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.List;

public class FunctionEvaluation {

  public static final String REF_STR = "Ref";
  public static final String CONDITION_STR = "Condition";
  public static final String FN_AND = "Fn::And";
  public static final String FN_EQUALS = "Fn::Equals";
  public static final String FN_IF = "Fn::If";
  public static final String FN_NOT = "Fn::Not";
  public static final String FN_OR = "Fn::Or";
  public static final String FN_BASE64 = "Fn::Base64";
  public static final String FN_SELECT = "Fn::Select";
  public static final String FN_JOIN = "Fn::Join";
  public static final String FN_FIND_IN_MAP = "Fn::FindInMap";
  public static final String FN_GET_AZS = "Fn::GetAZs";
  public static final String FN_GET_ATT = "Fn::GetAtt";
  public static final String AWS_NO_VALUE ="AWS::NoValue" ;


  public static boolean representsBooleanFunction(JsonNode jsonNode) {
    for (IntrinsicFunctions value: IntrinsicFunctions.values()) {
      if (value.evaluateMatch(jsonNode).isMatch() && value.isBooleanFunction()) {
        return true;
      }
    }
    return false;
  }

  public static boolean evaluateBoolean(JsonNode jsonNode) throws CloudFormationException {
    if (jsonNode == null || !jsonNode.isTextual() ||
      !("true".equalsIgnoreCase(jsonNode.textValue()) || "false".equalsIgnoreCase(jsonNode.textValue()))) {
      throw new ValidationErrorException("Template error: Invalid boolean value " + jsonNode);
    }
    return "true".equalsIgnoreCase(jsonNode.textValue());
  }
  public static void validateConditionSectionArgTypesWherePossible(JsonNode jsonNode) throws CloudFormationException {
    validateArgTypesWherePossible(jsonNode, true);
  }
  public static void validateNonConditionSectionArgTypesWherePossible(JsonNode jsonNode) throws CloudFormationException {
    validateArgTypesWherePossible(jsonNode, false);
  }

  private static void validateArgTypesWherePossible(JsonNode jsonNode, boolean inConditionsSection)
    throws CloudFormationException {
    if (jsonNode != null) {
      if (jsonNode.isArray()) {
        for (int i = 0;i < jsonNode.size(); i++) {
          validateConditionSectionArgTypesWherePossible(jsonNode.get(i));
        }
      } else if (jsonNode.isObject()) {
        List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
        for (String key: fieldNames) {
          validateConditionSectionArgTypesWherePossible(jsonNode.get(key));
        }

        for (IntrinsicFunction intrinsicFunction: IntrinsicFunctions.values()) {
          IntrinsicFunction.MatchResult matchResult = intrinsicFunction.evaluateMatch(jsonNode);
          if (matchResult.isMatch()) {
            intrinsicFunction.validateArgTypesWherePossible(matchResult);
            if (intrinsicFunction == IntrinsicFunctions.CONDITION && !inConditionsSection) {
              throw new ValidationErrorException("Template error: Condition token can only be used in Conditions block");
            }
            if (intrinsicFunction == IntrinsicFunctions.EQUALS && !inConditionsSection) {
              // strange error but it is what AWS says
              throw new ValidationErrorException("Template error: Fn::Equals cannot be partially collapsed");
            }
          }
        }
      }
    }
    // If not an object or array, nothing to validate
  }

  public static JsonNode evaluateFunctions(JsonNode jsonNode, Template template) throws CloudFormationException {
    if (jsonNode == null) return jsonNode;
    if (!jsonNode.isArray() && !jsonNode.isObject()) return jsonNode;
    ObjectMapper objectMapper = new ObjectMapper();
    if (jsonNode.isArray()) {
      ArrayNode arrayCopy = objectMapper.createArrayNode();
      for (int i = 0;i < jsonNode.size(); i++) {
        JsonNode arrayElement = evaluateFunctions(jsonNode.get(i), template);
        arrayCopy.add(arrayElement);
      }
      return arrayCopy;
    }
    // an object node
    // Some functions require literal values for arguments, so don't recursively evaluate functions on
    // values.  (Will do so within functions where appropriate)
    for (IntrinsicFunction intrinsicFunction: IntrinsicFunctions.values()) {
      IntrinsicFunction.MatchResult matchResult = intrinsicFunction.evaluateMatch(jsonNode);
      if (matchResult.isMatch()) {
        IntrinsicFunction.ValidateResult validateResult = intrinsicFunction.validateArgTypesWherePossible(matchResult);
        return intrinsicFunction.evaluateFunction(validateResult, template);
      }
    }
    // Otherwise, not a function, so evaluate functions of values
    ObjectNode objectCopy = objectMapper.createObjectNode();
    List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
    for (String key: fieldNames) {
      JsonNode objectElement = evaluateFunctions(jsonNode.get(key), template);
      objectCopy.put(key, objectElement);
    }
    return objectCopy;
  }

  public static void main(String[] args) {
    try {
      String evilJson = "{\n" +
        "  \"key1\" : \"value1\",\n" +
        "  \"key2\" : \"value2\",\n" +
        "  \"key3\" : {\"Ref\":\"AWS::NoValue\"},\n" +
        "  \"key4\" : {\"Ref\":\"AWS::NoValue\",\"Ref2\":\"AWS::NoValue\"},\n" +
        "  \"key5\" : {\"array1\":[\"a1\",\"a2\",[\"a3\",\"a4\",{\"Ref\":\"AWS::NoValue\"}]]},\n" +
        "  \"key6\" : [{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},\"a1\",{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},\"a2\",\"a3\",{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"}],\n" +
        "  \"key7\": [{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"},{\"Ref\":\"AWS::NoValue\"}],\n" +
        "  \"key8\" : [],\n" +
        "  \"key9\" : {\"Bob\":{\"Ref\":\"AWS::NoValue\"}},\n" +
        "  \"key10\" : {\"Bob\":{\"Ref\":\"Ref3\"}},\n" +
        "  \"key11\" : {\"Bob\":{\"Fn::Base64\":{\"Ref\":\"Ref2\"}}},\n" +
        "  \"key12\" : {\"Bob\":{\"Fn::Select\":[\"1\",{\"Ref\":\"Ref3\"}]}},\n" +
        "  \"key13\" : {\"Bob\":{\"Fn::Join\":[\"!!\",{\"Ref\":\"Ref3\"}]}}\n" +

        "}";

      Template template = new Template();
      Template.Reference ref1 = new Template.Reference();
      ref1.setReady(false);
      ref1.setReferenceName("Ref1");
      ref1.setReferenceType(Template.ReferenceType.Resource);
      Template.Reference ref2 = new Template.Reference();
      ref2.setReady(true);
      ref2.setReferenceName("Ref2");
      ref2.setReferenceType(Template.ReferenceType.Resource);
      ref2.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode("The bowels of hell")));
      Template.Reference ref3 = new Template.Reference();
      ref3.setReady(true);
      ref3.setReferenceName("Ref3");
      ref3.setReferenceType(Template.ReferenceType.Parameter);
      ObjectMapper objectMapper = new ObjectMapper();
      ArrayNode arrayNode = objectMapper.createArrayNode();
      arrayNode.add("The");
      arrayNode.add("bowels");
      arrayNode.add("of");
      arrayNode.add("hell");
      ref3.setReferenceValueJson(JsonHelper.getStringFromJsonNode(arrayNode));
      template.getReferenceMap().put("Ref1", ref1);
      template.getReferenceMap().put("Ref2", ref2);
      template.getReferenceMap().put("Ref3", ref3);
      JsonNode evilJsonNode = objectMapper.readTree(evilJson);
      evilJsonNode = (JsonNode) evaluateFunctions(evilJsonNode, template);
      System.out.println(objectMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(evilJsonNode));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}


