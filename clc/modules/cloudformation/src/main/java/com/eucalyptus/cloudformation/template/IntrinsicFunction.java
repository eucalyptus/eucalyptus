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
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

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
