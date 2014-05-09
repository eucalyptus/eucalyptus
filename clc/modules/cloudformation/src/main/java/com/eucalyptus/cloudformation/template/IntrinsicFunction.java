package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
* Created by ethomas on 1/30/14.
*/
public interface IntrinsicFunction {
  MatchResult evaluateMatch(JsonNode jsonNode);
  ValidateResult validateArgTypesWherePossible(MatchResult matchResult) throws CloudFormationException;
  JsonNode evaluateFunction(ValidateResult validateResult, StackEntity stackEntity, Map<String, ResourceInfo> resourceInfoMap)  throws CloudFormationException;

  public static class MatchResult {
    boolean match;
    JsonNode jsonNode;
    IntrinsicFunction callingFunction;

    public boolean isMatch() {
      return match;
    }

    public JsonNode getJsonNode() {
      return jsonNode;
    }

    public IntrinsicFunction getCallingFunction() {
      return callingFunction;
    }

    public MatchResult(boolean match, JsonNode jsonNode, IntrinsicFunction callingFunction) {
      this.match = match;
      this.jsonNode = jsonNode;
      this.callingFunction = callingFunction;
    }
  }

  public static class ValidateResult {
    JsonNode jsonNode;
    IntrinsicFunction callingFunction;

    public JsonNode getJsonNode() {
      return jsonNode;
    }

    public IntrinsicFunction getCallingFunction() {
      return callingFunction;
    }

    public ValidateResult(JsonNode jsonNode, IntrinsicFunction callingFunction) {
      this.jsonNode = jsonNode;
      this.callingFunction = callingFunction;
    }
  }

}
