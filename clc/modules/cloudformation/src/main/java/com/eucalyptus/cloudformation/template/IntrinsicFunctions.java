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

import com.eucalyptus.cloudformation.AccessDeniedException;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.CloudFormationService;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.DescribeAvailabilityZonesType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bouncycastle.util.encoders.Base64;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public enum IntrinsicFunctions implements IntrinsicFunction {
  NO_VALUE {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = ((jsonNode != null)
        && (
        jsonNode.isObject() && jsonNode.size() == 1 && jsonNode.has(FunctionEvaluation.REF_STR) && jsonNode.get(FunctionEvaluation.REF_STR) != null
          && jsonNode.get(FunctionEvaluation.REF_STR).isValueNode() && FunctionEvaluation.AWS_NO_VALUE.equals(jsonNode.get(FunctionEvaluation.REF_STR).asText())
      ));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      return validateResult.getJsonNode();
    }


    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      // TODO: not sure in this case, but certainly shouldn't be in Fn::String
      return false;
    }
  },
  REF {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.REF_STR));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // This is one where the value literally has to be a string
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.REF_STR);
      if (keyJsonNode == null || !keyJsonNode.isValueNode()) {
        throw new ValidationErrorException("Template error: All References must be of type string");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Already known to be string from validate
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.REF_STR);
      String key = keyJsonNode.asText();
      Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
      Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
      if (pseudoParameterMap.containsKey(key)) {
        return JsonHelper.getJsonNodeFromString(pseudoParameterMap.get(key));
      } else if (parameterMap.containsKey(key)) {
        return JsonHelper.getJsonNodeFromString(parameterMap.get(key).getJsonValue());
      } else if (template.getResourceInfoMap().containsKey(key)) {
        ResourceInfo resourceInfo = template.getResourceInfoMap().get(key);
        if (!resourceInfo.getReady()) {
          throw new ValidationErrorException("Template error: reference " + key + " not ready");
        } else {
          return JsonHelper.getJsonNodeFromString(resourceInfo.getReferenceValueJson());
        }
      } else {
        throw new ValidationErrorException("Template error: unresolved resource dependency: " + key);
      }

    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }
    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  CONDITION {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.CONDITION_STR));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // This is one where the value literally has to be a string
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.CONDITION_STR);
      if (keyJsonNode == null || !keyJsonNode.isValueNode()) {
        throw new ValidationErrorException("Template error: All Conditions must be of type string");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }
    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Already known to be string from validate
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.CONDITION_STR);
      String key = keyJsonNode.asText();
      Map<String, Boolean> conditionMap = template.getConditionMap();
      if (!conditionMap.containsKey(key)) {
        throw new ValidationErrorException("Template error: unresolved condition dependency: " + key);
      }
      return new TextNode(String.valueOf(conditionMap.get(key)));
    }
    @Override
    public boolean isBooleanFunction() {
      return true;
    }
    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  },
  IF {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_IF));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function returns an array of 3 elements, the first a string that is a condition, and two other functions,
      // So we check for a literal 3 element array, the first one has to be a string, the other two we can evaluate
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.FN_IF);
      if (keyJsonNode == null || !keyJsonNode.isArray() || keyJsonNode.size() < 1
        || keyJsonNode.get(0) == null || !keyJsonNode.get(0).isValueNode() ) {
        throw new ValidationErrorException("Template error: Fn::If requires a list argument with the first element " +
          "being a condition");
      } else if (keyJsonNode.size() != 3) {
        throw new ValidationErrorException("Template error: Fn::If requires a list argument with three elements");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // We know from validate this is an array of 3 elements
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.FN_IF);
      String key = keyJsonNode.get(0).asText();
      boolean booleanValue = template.getConditionMap().get(key);
      // Note: We are not evaluating both conditions because AWS does not for dependency purposes.  Don't want
      // to get a non-ready reference if we choose the wrong path
      // But evaluate (as it could be a function) the one we are returning
      return FunctionEvaluation.evaluateFunctions(keyJsonNode.get(booleanValue ? 1 : 2), template, effectiveUserId);
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }
    @Override
    public boolean mayBeStringFunction() {
      return true;
    }

  },
  EQUALS {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_EQUALS));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // Requires a literal list of two items (so no function evaluation for the list itself)
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.FN_EQUALS);

      if (keyJsonNode == null || !keyJsonNode.isArray() || keyJsonNode.size() != 2 ) {
        throw new ValidationErrorException("Template error: Fn::Equals requires a list argument with two elements");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Array verified by validate
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.FN_EQUALS);
      // On the other hand, the arguments can be functions
      JsonNode evaluatedArg0 = FunctionEvaluation.evaluateFunctions(keyJsonNode.get(0), template, effectiveUserId);
      JsonNode evaluatedArg1 = FunctionEvaluation.evaluateFunctions(keyJsonNode.get(1), template, effectiveUserId);
      // TODO: not sure if this is true
      if (evaluatedArg0 == null || evaluatedArg1 == null) return new TextNode("false");
      return new TextNode(String.valueOf(evaluatedArg0.equals(evaluatedArg1)));
    }
    @Override
    public boolean isBooleanFunction() {
      return true;
    }
    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  },
  AND {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_AND));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function evaluates to an array of boolean functions so no need to evaluate
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.FN_AND);
      if (keyJsonNode == null || !keyJsonNode.isArray() || keyJsonNode.size() < 2 || keyJsonNode.size() > 10) {
        throw new ValidationErrorException("Template error: every Fn::And object requires a list of at least 2 " +
          "and at most 10 boolean parameters.");
      }
      for (int i = 0;i < keyJsonNode.size(); i++) {
        // Make sure the argument is a function like Fn::Not, Fn::Equals, Fn::And, Fn::Condition
        JsonNode argNode = keyJsonNode.get(i);
        if (argNode == null || !argNode.isObject() || !FunctionEvaluation.representsBooleanFunction(argNode)) {
          throw new ValidationErrorException("Template error: every Fn::And object requires a list of at least 2 " +
            "and at most 10 boolean parameters.");
        }
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Args types validated already
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.FN_AND);
      boolean returnValue = true;
      for (int i = 0;i < keyJsonNode.size(); i++) {
        // Evaluate the argument
        JsonNode argNode = keyJsonNode.get(i);
        JsonNode evaluatedArgNode = FunctionEvaluation.evaluateFunctions(argNode, template, effectiveUserId);
        boolean boolValueArgNode = FunctionEvaluation.evaluateBoolean(evaluatedArgNode);
        returnValue = returnValue && boolValueArgNode;
      }
      return new TextNode(String.valueOf(returnValue));
    }
    @Override
    public boolean isBooleanFunction() {
      return true;
    }
    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  },
  OR {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_OR));
        return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function evaluates to an array of boolean functions so no need to evaluate
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.FN_OR);
      if (keyJsonNode == null || !keyJsonNode.isArray() || keyJsonNode.size() < 2 || keyJsonNode.size() > 10) {
        throw new ValidationErrorException("Template error: every Fn::Or object requires a list of at least 2 " +
          "and at most 10 boolean parameters.");
      }
      for (int i = 0;i < keyJsonNode.size(); i++) {
        // Make sure the argument is a function like Fn::Not, Fn::Equals, Fn::And, Fn::Condition
        JsonNode argNode = keyJsonNode.get(i);
        if (argNode == null || !argNode.isObject() || !FunctionEvaluation.representsBooleanFunction(argNode)) {
          throw new ValidationErrorException("Template error: every Fn::Or object requires a list of at least 2 " +
            "and at most 10 boolean parameters.");
        }
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Args types validated already
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.FN_OR);
      boolean returnValue = false;
      for (int i = 0;i < keyJsonNode.size(); i++) {
        // Evaluate the argument
        JsonNode argNode = keyJsonNode.get(i);
        JsonNode evaluatedArgNode = FunctionEvaluation.evaluateFunctions(argNode, template, effectiveUserId);
        boolean boolValueArgNode = FunctionEvaluation.evaluateBoolean(evaluatedArgNode);
        returnValue = returnValue || boolValueArgNode;
      }
      return new TextNode(String.valueOf(returnValue));
    }
    @Override
    public boolean isBooleanFunction() {
      return true;
    }
    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  },
  NOT {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_NOT));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function evaluates to an array of boolean functions so no need to evaluate
      JsonNode keyJsonNode = matchResult.getJsonNode().get(FunctionEvaluation.FN_NOT);
      if (keyJsonNode == null || !keyJsonNode.isArray() || keyJsonNode.size() != 1) {
        throw new ValidationErrorException("Template error: Fn::Not requires a list argument with one element");
      }

      // Make sure the argument is a function like Fn::Not, Fn::Equals, Fn::And, Fn::Condition
      JsonNode arg0Node = keyJsonNode.get(0);
      if (arg0Node == null || !arg0Node.isObject() || !FunctionEvaluation.representsBooleanFunction(arg0Node)) {
        throw new ValidationErrorException("Template error: Fn::Not requires a list argument " +
          "with one function token");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Args types validated already
      JsonNode keyJsonNode = validateResult.getJsonNode().get(FunctionEvaluation.FN_NOT);
      // Evaluate the argument
      JsonNode arg0Node = keyJsonNode.get(0);
      JsonNode evaluatedArg0Node = FunctionEvaluation.evaluateFunctions(arg0Node, template, effectiveUserId);
      boolean boolValueArg0Node = FunctionEvaluation.evaluateBoolean(evaluatedArg0Node);
      return new TextNode(String.valueOf(!boolValueArg0Node));
    }
    @Override
    public boolean isBooleanFunction() {
      return true;
    }
    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  },
  FIND_IN_MAP {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_FIND_IN_MAP));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // Experiments show must be literal array of size 3 (but can have function elements)
      JsonNode key = matchResult.getJsonNode().get(FunctionEvaluation.FN_FIND_IN_MAP);
      if (key == null || !key.isArray() || key.size() != 3) {
        throw new ValidationErrorException("Template error: every Fn::FindInMap object requires three parameters, " +
          "the map name, map key and the attribute for return value");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }


    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Size 3 array verified in validate
      JsonNode key = validateResult.getJsonNode().get(FunctionEvaluation.FN_FIND_IN_MAP);
      // Array elements might be functions so evaluate them
      JsonNode arg0Node = FunctionEvaluation.evaluateFunctions(key.get(0), template, effectiveUserId);
      JsonNode arg1Node = FunctionEvaluation.evaluateFunctions(key.get(1), template, effectiveUserId);
      JsonNode arg2Node = FunctionEvaluation.evaluateFunctions(key.get(2), template, effectiveUserId);
      // Make sure types ok
      if (arg0Node == null || arg1Node == null || arg2Node == null
        || !arg0Node.isValueNode() || !arg1Node.isValueNode() || !arg2Node.isValueNode()
        || arg0Node.asText() == null || arg1Node.asText() == null || arg2Node.asText() == null) {
        throw new ValidationErrorException("Template error: every Fn::FindInMap object requires three parameters, " +
          "the map name, map key and the attribute for return value");
      }

      String mapName = arg0Node.asText();
      String mapKey = arg1Node.asText();
      String attribute = arg2Node.asText();
      Map<String, Map<String, Map<String, String>>> mapping = template.getMapping();
      if (!mapping.containsKey(mapName)) {
        throw new ValidationErrorException("Template error: Mapping named '" + mapName + "' is not " +
          "present in the 'Mappings' section of template");
      }
      if (!mapping.get(mapName).containsKey(mapKey) ||
        !mapping.get(mapName).get(mapKey).containsKey(attribute)) {
        throw new ValidationErrorException("Template error: Unable to get mapping for " +
          mapName + "::" + mapKey + "::" + attribute);
      }
      return JsonHelper.getJsonNodeFromString(mapping.get(mapName).get(mapKey).get(attribute));
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }
    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  BASE64 {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_BASE64));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // no intrinsic evaluation
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // This one could evaluate from a function
      JsonNode keyJsonNode = FunctionEvaluation.evaluateFunctions(validateResult.getJsonNode().get(FunctionEvaluation.FN_BASE64), template, effectiveUserId);
      if (keyJsonNode == null || !keyJsonNode.isValueNode()) {
        throw new ValidationErrorException("Template error: every Fn::Base64 object must have a String-typed value.");
      }
      String key = keyJsonNode.asText();
      if (key == null) {
        throw new ValidationErrorException("Template error: every Fn::Base64 object must not have a null value.");
      }
      return (key == null) ? validateResult.getJsonNode() :
        new TextNode(new String(Base64.encode(key.getBytes()))); // TODO: are we just delaying an NPE?
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  SELECT {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_SELECT));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function returns an array with two elements: a string and an array so the top level element is literal
      JsonNode key = matchResult.getJsonNode().get(FunctionEvaluation.FN_SELECT);
      if (key == null || !key.isArray() || key.size() < 1) {
        throw new ValidationErrorException("Template error: Fn::Select requires a list " +
          "argument with a valid index value as its first element");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Top level array validated already
      JsonNode key = validateResult.getJsonNode().get(FunctionEvaluation.FN_SELECT);
      // on the other hand, both fields within this function can be functions (including the second array) so
      // let's evaluate
      JsonNode evaluatedIndex = FunctionEvaluation.evaluateFunctions(key.get(0), template, effectiveUserId);
      if (evaluatedIndex == null || !evaluatedIndex.isValueNode() || evaluatedIndex.asText() == null) {
        throw new ValidationErrorException("Template error: Fn::Select requires a list " +
          "argument with a valid index value as its first element");
      }
      int index = -1;
      try {
        index = Integer.parseInt(evaluatedIndex.asText());
      } catch (NumberFormatException ex) {
        throw new ValidationErrorException("Template error: Fn::Select requires a list argument with a valid index value as its first element");
      }
      if (key.size() != 2) {
        throw new ValidationErrorException("Template error: Fn::Select requires a list argument with two elements: an integer index and a list");
      }
      // Second argument must be an array but can be one as the result of a function
      JsonNode argArray = FunctionEvaluation.evaluateFunctions(key.get(1), template, effectiveUserId);
      if (argArray == null || !argArray.isArray()) {
        throw new ValidationErrorException("Template error: Fn::Select requires a list argument with two elements: an integer index and a list");
      }
      if (argArray == null || index < 0 || index >= argArray.size()) {
        throw new ValidationErrorException("Template error: Fn::Select cannot select nonexistent value at index " + index);
      }
      return argArray.get(index);
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  JOIN {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_JOIN));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function returns an array with two elements: a string and an array so the top level element is literal
      JsonNode key = matchResult.getJsonNode().get(FunctionEvaluation.FN_JOIN);
      if (key == null || !key.isArray() || key.size() != 2) {
        throw new ValidationErrorException("Template error: every Fn::Join object requires two parameters, "
          + "(1) a string delimiter and (2) a list of strings to be joined or a function that returns a list of "
          + "strings (such as Fn::GetAZs) to be joined.");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // Top level array validated already
      JsonNode key = validateResult.getJsonNode().get(FunctionEvaluation.FN_JOIN);
      // On the other hand, the delimiter and the list of strings can be functions
      JsonNode delimiterNode = FunctionEvaluation.evaluateFunctions(key.get(0), template, effectiveUserId);
      JsonNode arrayOfStrings = FunctionEvaluation.evaluateFunctions(key.get(1), template, effectiveUserId);
      if (delimiterNode == null || !delimiterNode.isValueNode() || delimiterNode.asText() == null ||
        arrayOfStrings == null || !arrayOfStrings.isArray()) {
        throw new ValidationErrorException("Template error: every Fn::Join object requires two parameters, "
          + "(1) a string delimiter and (2) a list of strings to be joined or a function that returns a list of "
          + "strings (such as Fn::GetAZs) to be joined.");
      }
      String delimiter = delimiterNode.asText();
      if (arrayOfStrings == null || arrayOfStrings.size() == 0) return new TextNode("");
      String tempDelimiter = "";
      StringBuilder buffer = new StringBuilder();
      for (int i=0;i<arrayOfStrings.size();i++) {
        if (arrayOfStrings.get(i) == null || !arrayOfStrings.get(i).isValueNode()
          || arrayOfStrings.get(i).asText() == null) {
          throw new ValidationErrorException("Template error: every Fn::Join object requires two parameters, (1) "
            + "a string delimiter and (2) a list of strings to be joined or a function that returns a list of strings"
            + " (such as Fn::GetAZs) to be joined.");
        }
        buffer.append(tempDelimiter).append(arrayOfStrings.get(i).asText());
        tempDelimiter = delimiter;
      }
      return new TextNode(buffer.toString());
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  GET_AZS {
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_GET_AZS));
      return new MatchResult(match, jsonNode, this);
    }
    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // no intrinsic evaluation
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      // This one could evaluate from a function
      JsonNode keyJsonNode = FunctionEvaluation.evaluateFunctions(validateResult.getJsonNode().get(FunctionEvaluation.FN_GET_AZS), template, effectiveUserId);
      if (keyJsonNode == null || !keyJsonNode.isValueNode()) {
        throw new ValidationErrorException("Template error: every Fn::GetAZs object must have a String-typed value.");
      }
      String key = keyJsonNode.asText();
      if (key == null) {
        throw new ValidationErrorException("Template error: every Fn::GetAZs object must not have a null value.");
      }
      List<String> availabilityZones = Lists.newArrayList();
      final List<String> defaultRegionAvailabilityZones;
      try {
        defaultRegionAvailabilityZones = describeAvailabilityZones(effectiveUserId);
      } catch (Exception e) {
        Throwable rootCause = Throwables.getRootCause(e);
        throw new AccessDeniedException("Unable to access availability zones.  " + (rootCause.getMessage() == null ? "" : rootCause.getMessage()));
      }
      final Map<String, List<String>> availabilityZoneMap = Maps.newHashMap();
      availabilityZoneMap.put(CloudFormationService.getRegion( ), defaultRegionAvailabilityZones);
      availabilityZoneMap.put("",defaultRegionAvailabilityZones); // "" defaults to the default region
      if (availabilityZoneMap != null && availabilityZoneMap.containsKey(key)) {
        availabilityZones.addAll(availabilityZoneMap.get(key));
      } else {
        // AWS appears to return no values in a different (or non-existant) region so we do the same.
      }
      ObjectMapper objectMapper = new ObjectMapper();
      ArrayNode arrayNode = objectMapper.createArrayNode();
      for (String availabilityZone: availabilityZones) {
        arrayNode.add(availabilityZone);
      }
      return arrayNode;
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return false;
    }

    private List<String> describeAvailabilityZones(String userId) throws Exception {
      ServiceConfiguration configuration = Topology.lookup(Compute.class);
      DescribeAvailabilityZonesType describeAvailabilityZonesType = new DescribeAvailabilityZonesType();
      describeAvailabilityZonesType.setEffectiveUserId(userId);
      DescribeAvailabilityZonesResponseType describeAvailabilityZonesResponseType =
        AsyncRequests.<DescribeAvailabilityZonesType,DescribeAvailabilityZonesResponseType>
          sendSync(configuration, describeAvailabilityZonesType);
      List<String> availabilityZones = Lists.newArrayList();
      for (ClusterInfoType clusterInfoType: describeAvailabilityZonesResponseType.getAvailabilityZoneInfo()) {
        availabilityZones.add(clusterInfoType.getZoneName());

      }
      return availabilityZones;
    }
  },
  FN_SUB {
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_SUB));
      return new MatchResult(match, jsonNode, this);
    }
    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      JsonNode key = matchResult.getJsonNode().get(FunctionEvaluation.FN_SUB);
      if (key == null || !(
        (key.isValueNode()) ||
          (key.isArray() && key.size() == 2 && key.get(0) != null &&
           key.get(0).isValueNode() && key.get(1) != null && key.get(1).isObject()))
        ) {
        throw new ValidationErrorException("Template error: One or more Fn::Sub intrinsic functions don't " +
          "specify expected arguments. Specify a string as first argument, and an optional second " +
          "argument to specify a mapping of values to replace in the string");
      }
      // a little checking of field values
      if (key.isValueNode()) {
        FnSubHelper.extractVariables(key.asText());
      } else {
        FnSubHelper.extractVariables(key.get(0).asText());
        checkValidSubstitutionKeys(key.get(1).fieldNames());
        checkValidStringOrStringFunctions(key.get(1));
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    private void checkValidSubstitutionKeys(Iterator<String> stringIterator) throws ValidationErrorException {
      if (stringIterator != null) {
        for (String key: (Iterable<String>) ()-> stringIterator) {
          Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
          if (!pattern.matcher(key).matches()) {
            throw new ValidationErrorException("Template error: every key of the context object of every Fn::Sub object " +
              "must contain only alphanumeric characters and underscores");
          }
        }
      }
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
      Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
      JsonNode key = validateResult.getJsonNode().get(FunctionEvaluation.FN_SUB);
      String value;
      // a little checking of field values
      Map<String, String> variableMapping = Maps.newHashMap();
      if (key.isValueNode()) {
        value = key.asText();
      } else {
        value = key.get(0).asText();
        for (String fieldName : Lists.newArrayList(key.get(1).fieldNames())) {
          JsonNode valueNode = FunctionEvaluation.evaluateFunctions(key.get(1).get(fieldName), template, effectiveUserId);
          if (valueNode == null || !valueNode.isValueNode()) {
            throw new ValidationErrorException("Template error: every value of the context object of every Fn::Sub object must be a string or a function that returns a string");
          }
          variableMapping.put(fieldName, valueNode.asText());
        }
      }
      Collection<String> variables = FnSubHelper.extractVariables(value);
      // check variables
      for (String variable: variables) {
        if (!variableMapping.containsKey(variable)) {
          if (pseudoParameterMap.containsKey(variable)) {
            checkAndAddJsonNodeToVariableMapping(variable, variableMapping, pseudoParameterMap.get(variable));
          } else if (parameterMap.containsKey(variable)) {
            checkAndAddJsonNodeToVariableMapping(variable, variableMapping, parameterMap.get(variable).getJsonValue());
          } else if (template.getResourceInfoMap().containsKey(variable)) {
            ResourceInfo resourceInfo = template.getResourceInfoMap().get(variable);
            if (!resourceInfo.getReady()) {
              throw new ValidationErrorException("Template error: reference " + key + " not ready");
            } else {
              checkAndAddJsonNodeToVariableMapping(variable, variableMapping, resourceInfo.getReferenceValueJson());
            }
          } else if (variable.indexOf(".") == -1) {
            throw new ValidationErrorException("Unresolved resource dependencies [" + variable + "] in the Resources block of the template");
          } else {
            int dotPos = variable.indexOf(".");
            String resourceName = variable.substring(0, dotPos);
            String attributeName = variable.substring(dotPos + 1);
            if (template.getResourceInfoMap().containsKey(resourceName) &&
              template.getResourceInfoMap().get(resourceName).isAttributeAllowed(attributeName)) {
              ResourceInfo resourceInfo = template.getResourceInfoMap().get(resourceName);
              if (!resourceInfo.getReady()) {
                throw new ValidationErrorException("Template error: reference " + resourceName + " not ready");
              }
              try {
                checkAndAddJsonNodeToVariableMapping(variable, variableMapping, resourceInfo.getResourceAttributeJson(attributeName));
              } catch (Exception ex) {
                throw new ValidationErrorException("Template error: resource " + resourceName + " does not support " +
                  "attribute type " + attributeName + " in Fn::GetAtt");
              }
            } else {
              throw new ValidationErrorException("Template error: instance of Fn::Sub references invalid resource attribute " + variable);
            }
          }
        }
      }
      return new TextNode(FnSubHelper.replaceVariables(value, variableMapping));
    }

    private void checkValidStringOrStringFunctions(JsonNode jsonNode) throws ValidationErrorException {
      for (String fieldName: Lists.newArrayList(jsonNode.fieldNames())) {
        JsonNode valueNode = jsonNode.get(fieldName);
        if (valueNode == null || !(valueNode.isValueNode() || FunctionEvaluation.mayRepresentStringFunction(valueNode))) {
          throw new ValidationErrorException("Template error: every value of the context object of every Fn::Sub object must be a string or a function that returns a string");
        }
      }
    }

    private void checkAndAddJsonNodeToVariableMapping(String variable, Map<String, String> variableMapping, String jsonString) throws ValidationErrorException {
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(jsonString);
      if (jsonNode == null || !jsonNode.isValueNode()) {
        throw new ValidationErrorException("Template error: every variable in an Fn::sub object must reference a string");
      }
      variableMapping.put(variable, jsonNode.asText());
    }

    @Override
    public boolean isBooleanFunction() {
      return false;
    }
    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  GET_ATT {
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      boolean match = (jsonNode != null &&  jsonNode.isObject() && (jsonNode.size() == 1) && jsonNode.has(FunctionEvaluation.FN_GET_ATT));
      return new MatchResult(match, jsonNode, this);
    }
    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // No function returns an array with two elements: a string and an array so the top level element is literal
      JsonNode key = matchResult.getJsonNode().get(FunctionEvaluation.FN_GET_ATT);
      if (key == null || !key.isArray() || key.size() != 2 || key.get(0) == null || !key.get(0).isValueNode()
        || key.get(0).asText() == null || key.get(0).asText().isEmpty() || key.get(1) == null ||
        !key.get(1).isValueNode() || key.get(1).asText() == null || key.get(1).asText().isEmpty()) {
        throw new ValidationErrorException("Template error: every Fn::GetAtt object requires two non-empty parameters, " +
          "the resource name and the resource attribute");
      }
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      JsonNode key = validateResult.getJsonNode().get(FunctionEvaluation.FN_GET_ATT);
      String resourceName = key.get(0).asText();
      if (!template.getResourceInfoMap().containsKey(resourceName)) {
        throw new ValidationErrorException("Template error: instance of Fn::GetAtt references undefined resource "
          + resourceName);
      }
      ResourceInfo resourceInfo = template.getResourceInfoMap().get(resourceName);
      if (!resourceInfo.getReady()) {
        throw new ValidationErrorException("Template error: reference " + resourceName + " not ready");
      }
      String attributeName = key.get(1).asText();
      try {
        return JsonHelper.getJsonNodeFromString(resourceInfo.getResourceAttributeJson(attributeName));
      } catch (Exception ex) {
        throw new ValidationErrorException("Template error: resource " + resourceName + " does not support " +
            "attribute type " + attributeName + " in Fn::GetAtt");
      }
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return true;
    }
  },
  UNKNOWN {
    @Override
    public MatchResult evaluateMatch(JsonNode jsonNode) {
      // Something that starts with Fn:  (any existing functions will already have been evaluated)
      boolean match = ((jsonNode != null)
        && (
        jsonNode.isObject() && jsonNode.size() == 1 && jsonNode.fieldNames().next().startsWith("Fn:")
      ));
      return new MatchResult(match, jsonNode, this);
    }

    @Override
    public ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException {
      checkState(matchResult, this);
      // no intrinsic evaluation
      return new ValidateResult(matchResult.getJsonNode(), this);
    }

    @Override
    public JsonNode evaluateFunction(ValidateResult validateResult, Template template, String effectiveUserId) throws CloudFormationException {
      checkState(validateResult, this);
      throw new ValidationErrorException("Template Error: Encountered unsupported function: " +
        validateResult.getJsonNode().fieldNames().next()+" Supported functions are: [Fn::Base64, Fn::GetAtt, " +
        "Fn::GetAZs, Fn::Join, Fn::FindInMap, Fn::Select, Ref, Fn::Equals, Fn::If, Fn::Not, " +
        "Condition, Fn::And, Fn::Or, Fn::Sub]");
    }
    @Override
    public boolean isBooleanFunction() {
      return false;
    }

    @Override
    public boolean mayBeStringFunction() {
      return false;
    }
  };

  public abstract boolean isBooleanFunction();
  public abstract boolean mayBeStringFunction();

  protected void checkState(MatchResult matchResult, IntrinsicFunction intrinsicFunction) {
    if (matchResult == null || matchResult.isMatch() == false || !intrinsicFunction.equals(matchResult.getCallingFunction())) {
      throw new IllegalStateException("MatchResult passed in is null, false or used with the wrong function");
    }
  }
  protected void checkState(ValidateResult validateResult, IntrinsicFunction intrinsicFunction) {
    if (validateResult == null || !equals(validateResult.getCallingFunction())) {
      throw new IllegalStateException("ValidateResult passed in is null, false or used with the wrong function");
    }
  }
}
