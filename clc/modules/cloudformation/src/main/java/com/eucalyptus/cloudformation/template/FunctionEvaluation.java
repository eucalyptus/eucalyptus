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
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.entity.VersionedStackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

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
  public static final String FN_SUB = "Fn::Sub";
  public static final String FN_CIDR = "Fn::Cidr";
  public static final String FN_SPLIT = "Fn::Split";

  public static final String AWS_NO_VALUE ="AWS::NoValue" ;


  public static boolean mayRepresentStringFunction(JsonNode jsonNode) {
    for (IntrinsicFunctions value: IntrinsicFunctions.values()) {
      if (value.evaluateMatch(jsonNode).isMatch() && value.mayBeStringFunction()) {
        return true;
      }
    }
    return false;
  }

  public static boolean representsBooleanFunction(JsonNode jsonNode) {
    for (IntrinsicFunctions value: IntrinsicFunctions.values()) {
      if (value.evaluateMatch(jsonNode).isMatch() && value.isBooleanFunction()) {
        return true;
      }
    }
    return false;
  }

  public static boolean evaluateBoolean(JsonNode jsonNode) throws CloudFormationException {
    if (jsonNode == null || !jsonNode.isValueNode() ||
      !("true".equalsIgnoreCase(jsonNode.asText()) || "false".equalsIgnoreCase(jsonNode.asText()))) {
      throw new ValidationErrorException("Template error: Invalid boolean value " + jsonNode);
    }
    return "true".equalsIgnoreCase(jsonNode.asText());
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
  public static JsonNode evaluateFunctions(JsonNode jsonNode, Template template, String effectiveUserId) throws CloudFormationException {
    if (jsonNode == null) return jsonNode;
    if (!jsonNode.isArray() && !jsonNode.isObject()) return jsonNode;
    if (jsonNode.isArray()) {
      ArrayNode arrayCopy = JsonHelper.createArrayNode();
      for (int i = 0;i < jsonNode.size(); i++) {
        JsonNode arrayElement = evaluateFunctions(jsonNode.get(i), template, effectiveUserId);
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
        return intrinsicFunction.evaluateFunction(validateResult, template, effectiveUserId);
      }
    }
    // Otherwise, not a function, so evaluate functions of values
    ObjectNode objectCopy = JsonHelper.createObjectNode();
    List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
    for (String key: fieldNames) {
      JsonNode objectElement = evaluateFunctions(jsonNode.get(key), template, effectiveUserId);
      objectCopy.put(key, objectElement);
    }
    return objectCopy;
  }

  public static JsonNode evaluateFunctionsPreResourceResolution(JsonNode jsonNode, Template template, String effectiveUserId) throws CloudFormationException {
    if (jsonNode == null) return jsonNode;
    if (!jsonNode.isArray() && !jsonNode.isObject()) return jsonNode;
    if (jsonNode.isArray()) {
      ArrayNode arrayCopy = JsonHelper.createArrayNode();
      for (int i = 0;i < jsonNode.size(); i++) {
        JsonNode arrayElement = evaluateFunctionsPreResourceResolution(jsonNode.get(i), template, effectiveUserId);
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
        try {
          return intrinsicFunction.evaluateFunction(validateResult, template, effectiveUserId);
        } catch (ValidationErrorException ex) {
          // for now if we get an error due to a Ref: or Fn::GetAtt call on a resource we can just return the original value
          return jsonNode;
        }
      }
    }
    // Otherwise, not a function, so evaluate functions of values
    ObjectNode objectCopy = JsonHelper.createObjectNode();
    List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
    for (String key: fieldNames) {
      JsonNode objectElement = evaluateFunctionsPreResourceResolution(jsonNode.get(key), template, effectiveUserId);
      objectCopy.put(key, objectElement);
    }
    return objectCopy;
  }



  public static JsonNode evaluateFunctions(JsonNode jsonNode, VersionedStackEntity stackEntity, Map<String, ResourceInfo> resourceInfoMap, String effectiveUserId) throws CloudFormationException {
    Template template = new Template();
    template.setResourceInfoMap(resourceInfoMap);
    StackEntityHelper.populateTemplateWithStackEntity(template, stackEntity);
    JsonNode result = evaluateFunctions(jsonNode, template, effectiveUserId);
    // just in case the above function changes the template, put the results back into the stack entity
    StackEntityHelper.populateStackEntityWithTemplate(stackEntity, template);
    return result;
  }
}


