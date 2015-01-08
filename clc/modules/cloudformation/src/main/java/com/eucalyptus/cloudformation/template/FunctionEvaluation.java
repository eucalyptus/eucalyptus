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
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public static JsonNode evaluateFunctions(JsonNode jsonNode, StackEntity stackEntity, Map<String, ResourceInfo> resourceInfoMap) throws CloudFormationException {
    Template template = new Template();
    template.setResourceInfoMap(resourceInfoMap);
    StackEntityHelper.populateTemplateWithStackEntity(template, stackEntity);
    JsonNode result = evaluateFunctions(jsonNode, template);
    // just in case the above function changes the template, put the results back into the stack entity
    StackEntityHelper.populateStackEntityWithTemplate(stackEntity, template);
    return result;
  }
}


