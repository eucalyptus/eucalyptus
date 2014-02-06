/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.resources.ResourceAttributeResolver;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.cloudformation.template.dependencies.CyclicDependencyException;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import sun.org.mozilla.javascript.Function;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

/**
 * Created by ethomas on 12/10/13.
 */
public class TemplateParser {
  public TemplateParser() {
  }

  private enum TemplateSection {
    AWSTemplateFormatVersion,
    Description,
    Parameters,
    Mappings,
    Conditions,
    Resources,
    Outputs
  };

  private enum ParameterKey {
    Type,
    Default,
    NoEcho,
    AllowedValues,
    AllowedPattern,
    MaxLength,
    MinLength,
    MaxValue,
    MinValue,
    Description,
    ConstraintDescription
  };

  private enum ResourceKey {
    Properties,
    DeletionPolicy,
    DependsOn,
    Metadata,
    UpdatePolicy,
    Condition
  }

  private enum DeletionPolicyValues {
    Delete,
    Retain,
    Snapshot
  }

  private static final String AWS_ACCOUNT_ID = "AWS::AccountId";
  private static final String AWS_NOTIFICATION_ARNS = "AWS::NotificationARNs";
  private static final String AWS_NO_VALUE = "AWS::NoValue";
  private static final String AWS_REGION = "AWS::Region";
  private static final String AWS_STACK_ID = "AWS::StackId";
  private static final String AWS_STACK_NAME = "AWS::StackName";

  private static final String DEFAULT_TEMPLATE_VERSION = "2010-09-09";
  private static final String[] validTemplateVersions = new String[] {DEFAULT_TEMPLATE_VERSION};
  private ObjectMapper objectMapper = new ObjectMapper();

  public Template parse(String templateBody, List<Parameter> userParameters,
                        PseudoParameterValues pseudoParameterValues) throws CloudFormationException {
    Map<String, String> paramMap = Maps.newHashMap();
    Template template = new Template();
    JsonNode templateJsonNode = null;
    try {
      templateJsonNode = objectMapper.readTree(templateBody);
    } catch (IOException ex) {
      throw new ValidationErrorException(ex.getMessage());
    }
    if (!templateJsonNode.isObject()) {
      throw new ValidationErrorException("Template body is not a JSON object");
    }
    addPseudoParameters(template, pseudoParameterValues);
    buildResourceMap(template, templateJsonNode);
    parseValidTopLevelKeys(templateJsonNode);
    parseVersion(template, templateJsonNode);
    parseDescription(template, templateJsonNode);
    parseMappings(template, templateJsonNode);
    parseParameters(userParameters, paramMap, template, templateJsonNode);
    parseConditions(template, templateJsonNode);
    parseResources(template, templateJsonNode);
    parseOutputs(template, templateJsonNode);
    return template;
  }

  private void addPseudoParameters(Template template, PseudoParameterValues pseudoParameterValues) {

    ObjectMapper mapper = new ObjectMapper();

    Template.Reference accountIdReference = new Template.Reference();
    accountIdReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    accountIdReference.setReferenceName(AWS_ACCOUNT_ID);
    accountIdReference.setReferenceValue(new TextNode(pseudoParameterValues.getAccountId()));
    accountIdReference.setReady(true);
    template.getReferenceMap().put(AWS_ACCOUNT_ID, accountIdReference);

    Template.Reference notificationArnsReference = new Template.Reference();
    ArrayNode notificationsArnNode = objectMapper.createArrayNode();
    for (String notificationArn: pseudoParameterValues.getNotificationArns()) {
      notificationsArnNode.add(notificationArn);
    }
    notificationArnsReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    notificationArnsReference.setReferenceName(AWS_NOTIFICATION_ARNS);
    notificationArnsReference.setReferenceValue(notificationsArnNode);
    notificationArnsReference.setReady(true);
    template.getReferenceMap().put(AWS_NOTIFICATION_ARNS, notificationArnsReference);

    Template.Reference noValueReference = new Template.Reference();
    ObjectNode noValueNode = mapper.createObjectNode();
    noValueNode.put(FunctionEvaluation.REF_STR, AWS_NO_VALUE);
    noValueReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    noValueReference.setReferenceName(AWS_NO_VALUE);
    noValueReference.setReferenceValue(noValueNode);
    noValueReference.setReady(true);
    template.getReferenceMap().put(AWS_NO_VALUE, noValueReference);

    Template.Reference regionReference = new Template.Reference();
    regionReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    regionReference.setReferenceName(AWS_REGION);
    regionReference.setReferenceValue(new TextNode(pseudoParameterValues.getRegion()));
    regionReference.setReady(true);
    template.getReferenceMap().put(AWS_REGION, regionReference);

    Template.Reference stackIdReference = new Template.Reference();
    stackIdReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    stackIdReference.setReferenceName(AWS_STACK_ID);
    stackIdReference.setReferenceValue(new TextNode(pseudoParameterValues.getStackId()));
    stackIdReference.setReady(true);
    template.getReferenceMap().put(AWS_STACK_ID, stackIdReference);

    Template.Reference stackNameReference = new Template.Reference();
    stackNameReference.setReferenceType(Template.ReferenceType.PseudoParameter);
    stackNameReference.setReferenceName(AWS_STACK_NAME);
    stackNameReference.setReferenceValue(new TextNode(pseudoParameterValues.getStackName()));
    stackNameReference.setReady(true);
    template.getReferenceMap().put(AWS_STACK_NAME, stackNameReference);

    template.setAvailabilityZoneMap(pseudoParameterValues.getAvailabilityZones());
  }

  private void parseValidTopLevelKeys(JsonNode templateJsonNode) throws CloudFormationException {
    Set<String> tempTopLevelKeys = Sets.newHashSet(templateJsonNode.fieldNames());
    for (TemplateSection section: TemplateSection.values()) {
      tempTopLevelKeys.remove(section.toString());
    }
    if (!tempTopLevelKeys.isEmpty()) {
      throw new ValidationErrorException("Invalid template property or properties " + tempTopLevelKeys);
    }
  }

  private void parseVersion(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    String templateFormatVersion = JSONHelper.getString(templateJsonNode,
      TemplateSection.AWSTemplateFormatVersion.toString(),
      "unsupported value for " + TemplateSection.AWSTemplateFormatVersion + ".  No such version.");
    if (templateFormatVersion == null) {
      template.setTemplateFormatVersion(DEFAULT_TEMPLATE_VERSION);
      return;
    }
    if (!Arrays.asList(validTemplateVersions).contains(templateFormatVersion)) {
      throw new ValidationErrorException("Template format error: unsupported value for "
        + TemplateSection.AWSTemplateFormatVersion + ".");
    }
    template.setTemplateFormatVersion(templateFormatVersion);
  }

  private void parseDescription(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    String description = JSONHelper.getString(templateJsonNode, TemplateSection.Description.toString());
    if (description == null) return;
    if (description.length() > 4000) {
      throw new ValidationErrorException("Template format error: " + TemplateSection.Description + " must "
        + "be no longer than 4000 characters.");
    }
    template.setDescription(description);
  }


  private void parseMappings(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode mappingsJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Mappings.toString());
    if (mappingsJsonNode == null) return;
    for (String mapName: Lists.newArrayList(mappingsJsonNode.fieldNames())) {
      JsonNode mappingJsonNode = JSONHelper.checkObject(mappingsJsonNode, mapName, "Every "
        + TemplateSection.Mappings + " member " + mapName + " must be a map");

      for (String mapKey: Lists.newArrayList(mappingJsonNode.fieldNames())) {
        JsonNode attributesJsonNode = JSONHelper.checkObject(mappingJsonNode, mapKey, "Every "
          + TemplateSection.Mappings + " member " + mapKey + " must be a map");
        for (String attribute: Lists.newArrayList(attributesJsonNode.fieldNames())) {
          JsonNode valueJsonNode = JSONHelper.checkStringOrArray(attributesJsonNode, attribute, "Every "
            + TemplateSection.Mappings + " attribute must be a String or a List.");
          if (!template.getMapping().containsKey(mapName)) {
            template.getMapping().put(mapName, Maps.<String, Map<String, JsonNode>>newHashMap());
          }
          if (!template.getMapping().get(mapName).containsKey(mapKey)) {
            template.getMapping().get(mapName).put(mapKey, Maps.<String, JsonNode>newHashMap());
          }
          template.getMapping().get(mapName).get(mapKey).put(attribute, valueJsonNode);
        }
      }
    }
  }
  private void parseParameters(List<Parameter> userParameters, Map<String, String> paramMap, Template template,
                               JsonNode templateJsonNode) throws CloudFormationException {
    if (userParameters != null) {
      for (Parameter userParameter: userParameters) {
        paramMap.put(userParameter.getParameterKey(), userParameter.getParameterValue());
      }
    }
    JsonNode parametersJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Parameters.toString());
    if (parametersJsonNode == null) return;
    Set<String> parameterNames = Sets.newHashSet(parametersJsonNode.fieldNames());
    Set<String> noValueParameters = Sets.newHashSet();
    for (String parameterName: parameterNames) {
      JsonNode parameterJsonNode = JSONHelper.checkObject(parametersJsonNode, parameterName, "Any "
        + TemplateSection.Parameters + " member must be a JSON object.");
      parseParameter(parameterName, parameterJsonNode, paramMap, template, noValueParameters);
    }
    if (!noValueParameters.isEmpty()) {
      throw new ValidationErrorException("Parameters: " + noValueParameters + " must have values");
    }
    // one last sanity check
    Set<String> userParamKeys = Sets.newHashSet();
    Set<String> templateParamKeys = Sets.newHashSet();
    if (userParameters != null) {
      userParamKeys.addAll(paramMap.keySet());
    }
    if (parametersJsonNode != null) {
      templateParamKeys.addAll(Sets.newHashSet(parametersJsonNode.fieldNames()));
    }
    userParamKeys.removeAll(templateParamKeys);
    if (!userParamKeys.isEmpty()) {
      throw new ValidationErrorException("Parameters: " + userParamKeys + " do not exist in the template");
    }
  }

  private void parseParameter(String parameterName, JsonNode parameterJsonNode, Map<String, String> paramMap,
                              Template template, Set<String> noValueParameters) throws CloudFormationException {
    Set<String> tempParameterKeys = Sets.newHashSet(parameterJsonNode.fieldNames());
    for (ParameterKey validParameterKey: ParameterKey.values()) {
      tempParameterKeys.remove(validParameterKey.toString());
    }
    if (!tempParameterKeys.isEmpty()) {
      throw new ValidationErrorException("Invalid template parameter property or properties " + tempParameterKeys);
    }
    Template.Parameter parameter = new Template.Parameter();
    String type = JSONHelper.getString(parameterJsonNode, ParameterKey.Type.toString());
    if (type == null) {
      throw new ValidationErrorException("Template format error: Every " + TemplateSection.Parameters + " object "
        + "must contain a " +  ParameterKey.Type +" member.");
    }
    Template.ParameterType parameterType = null;
    try {
      parameterType = Template.ParameterType.valueOf(type);
    } catch (Exception ex) {
      throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + type);
    }
    JsonNode allowedValuesJsonNode = JSONHelper.checkArray(parameterJsonNode, ParameterKey.AllowedValues.toString());
    if (allowedValuesJsonNode != null) {
      String[] allowedValues = new String[allowedValuesJsonNode.size()];
      for (int index=0;index<allowedValues.length; index++) {
        String errorMsg = "Every " + ParameterKey.AllowedValues + "value must be a string.";
        String allowedValue = JSONHelper.getString(allowedValuesJsonNode, index, errorMsg);
        if (allowedValue == null) {
          throw new ValidationErrorException("Template format error: " + errorMsg);
        }
        allowedValues[index] = allowedValue;
      }
      parameter.setAllowedValues(allowedValues);
    }
    parameter.setAllowedPattern(JSONHelper.getString(parameterJsonNode, ParameterKey.AllowedPattern.toString()));
    String constraintDescription = JSONHelper.getString(parameterJsonNode,
      ParameterKey.ConstraintDescription.toString());
    if (constraintDescription != null && constraintDescription.length() > 4000) {
      throw new ValidationErrorException("Template format error: " +
        ParameterKey.ConstraintDescription + " must be no longer than 4000 characters.");
    }
    parameter.setConstraintDescription(constraintDescription);
    parameter.setDefaultValue(JSONHelper.getString(parameterJsonNode, ParameterKey.Default.toString()));
    String description = JSONHelper.getString(parameterJsonNode, ParameterKey.Description.toString());
    if (description != null && description.length() > 4000) {
      throw new ValidationErrorException("Template format error: " + ParameterKey.Description + " must be no "
        + "longer than 4000 characters.");
    }
    parameter.setDescription(description);
    parameter.setMaxLength(JSONHelper.getDouble(parameterJsonNode, ParameterKey.MaxLength.toString()));
    parameter.setMinLength(JSONHelper.getDouble(parameterJsonNode, ParameterKey.MinLength.toString()));
    parameter.setMaxValue(JSONHelper.getDouble(parameterJsonNode, ParameterKey.MaxValue.toString()));
    parameter.setMinValue(JSONHelper.getDouble(parameterJsonNode, ParameterKey.MinValue.toString()));
    parameter.setDefaultValue(JSONHelper.getString(parameterJsonNode, ParameterKey.Default.toString()));
    parameter.setNoEcho("true".equalsIgnoreCase(JSONHelper.getString(parameterJsonNode,
      ParameterKey.NoEcho.toString())));
    parameter.setParameterKey(parameterName);
    parameter.setType(parameterType);
    parameter.setParameterValue(paramMap.get(parameterName) != null ? paramMap.get(parameterName) :
      parameter.getDefaultValue());
    if (parameter.getParameterValue() == null) {
      noValueParameters.add(parameterName);
      return;
    }
    if (parameter.getAllowedValues() != null
      && !Arrays.asList(parameter.getAllowedValues()).contains(parameter.getParameterValue())) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterName + "' must be one of " + ParameterKey.AllowedValues
      );
    }
    JsonNode referenceValue = null; // reference value will be a JsonNode so we have a common object when evaluating.
    switch(parameterType) {
      case Number:
        parseNumberParameter(parameterName, parameter);
        referenceValue = new TextNode(parameter.getParameterValue());
        break;
      case String:
        parseStringParameter(parameterName, parameter);
        referenceValue = new TextNode(parameter.getParameterValue());
        break;
      case CommaDelimitedList:
        parseCommaDelimitedListParameter(parameterName, parameter);
        referenceValue = objectMapper.createArrayNode();
        StringTokenizer tokenizer = new StringTokenizer(parameter.getParameterValue(), ",");
        while (tokenizer.hasMoreTokens()) {
          ((ArrayNode) referenceValue).add(tokenizer.nextToken());
        }
        break;
      default:
        throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + parameterType);
    }
    template.addParameter(parameter);
    Template.Reference reference = new Template.Reference();
    reference.setReady(true);
    reference.setReferenceName(parameter.getParameterKey());
    reference.setReferenceValue(referenceValue);
    reference.setReferenceType(Template.ReferenceType.Parameter);
    template.getReferenceMap().put(parameter.getParameterKey(), reference);
  }

  private void parseCommaDelimitedListParameter(String parameterKey, Template.Parameter parameter)
    throws CloudFormationException {
    if (parameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinLength
        + " must be on a parameter of type String");
    }
    if (parameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxLength +
        " must be on a parameter of type String");
    }
    if (parameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " +
        ParameterKey.AllowedPattern + " must be on a parameter of type String");
    }
    if (parameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " +
        ParameterKey.MinValue + " must be on a parameter of type Number");
    }
    if (parameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxValue +
        " must be on a parameter of type Number");
    }
  }

  private void parseStringParameter(String parameterKey, Template.Parameter parameter) throws CloudFormationException {
    // boot out the unallowed parameters
    if (parameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be on a parameter of type Number");
    }
    if (parameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxValue
        + " must be on a parameter of type Number");
    }
    if (parameter.getMaxLength() != null && parameter.getMinLength() != null
      && parameter.getMaxLength() < parameter.getMinLength()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be less than " + ParameterKey.MaxValue + ".");
    }
    if (parameter.getMinLength() != null && parameter.getMinLength() > parameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at least " + parameter.getMinLength()
            + " characters"
      );
    }
    if (parameter.getMaxLength() != null && parameter.getMaxLength() < parameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at most " + parameter.getMaxLength()
            + " characters"
      );
    }
    if (parameter.getAllowedPattern() != null) {
      try {
        if (!parameter.getParameterValue().matches(parameter.getAllowedPattern())) {
          throw new ValidationErrorException(
            parameter.getConstraintDescription() != null ?
              parameter.getConstraintDescription() :
              "Template error: Parameter '" + parameterKey + "' must match pattern " + parameter.getAllowedPattern()
          );
        }
      } catch (PatternSyntaxException ex) {
        throw new ValidationErrorException("Parameter '"+parameterKey+"' " + ParameterKey.AllowedPattern
          + " must be a valid regular expression.");
      }
    }
  }

  private void parseNumberParameter(String parameterKey, Template.Parameter parameter) throws CloudFormationException {
    // boot out the unallowed parameters
    if (parameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinLength
        + " must be on a parameter of type String");
    }
    if (parameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxLength
        + " must be on a parameter of type String");
    }
    if (parameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' "
        + ParameterKey.AllowedPattern + " must be on a parameter of type String");
    }
    if (parameter.getMaxValue() != null && parameter.getMinValue() != null
      && parameter.getMaxValue() < parameter.getMinValue()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be less than " + ParameterKey.MaxValue + ".");
    }
    Double valueDouble = null;
    try {
      valueDouble = Double.parseDouble(parameter.getParameterValue());
    } catch (NumberFormatException ex) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number"
      );
    }
    if (parameter.getMinValue() != null && parameter.getMinValue() > valueDouble) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number not less than " + parameter.getMinValue()
      );
    }
    if (parameter.getMaxValue() != null && parameter.getMaxValue() < valueDouble) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number not greater than "
            + parameter.getMaxValue()
      );
    }
  }

  private void parseConditions(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode conditionsJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Conditions.toString());
    if (conditionsJsonNode == null) return;
    Set<String> conditionNames = Sets.newHashSet(conditionsJsonNode.fieldNames());
    DependencyManager conditionDependencyManager = template.getConditionDependencyManager();
    for (String conditionName: conditionNames) {
      conditionDependencyManager.addNode(conditionName);
      Template.Condition condition = new Template.Condition();
      condition.setConditionName(conditionName);
      condition.setReady(false);
      template.getConditionMap().put(conditionName, condition);
    }
    // Now crawl for dependencies and make sure no resource references...
    Set<String> resourceReferences = Sets.newHashSet();
    Set<String> unresolvedConditionDependencies = Sets.newHashSet();
    for (String conditionName: conditionNames) {
      JsonNode conditionJsonNode = JSONHelper.checkObject(conditionsJsonNode, conditionName, "Any "
        + TemplateSection.Conditions + " member must be a JSON object.");
      conditionDependencyCrawl(conditionName, conditionJsonNode, conditionDependencyManager, template,
        resourceReferences, unresolvedConditionDependencies);
      FunctionEvaluation.validateConditionSectionArgTypesWherePossible(conditionJsonNode);
    }

    if (resourceReferences != null && !resourceReferences.isEmpty()) {
      throw new ValidationErrorException("Template format error: Unresolved dependencies " +
        resourceReferences + ". Cannot reference resources in the Conditions block of the template");
    }
    if (unresolvedConditionDependencies != null && !resourceReferences.isEmpty()) {
      throw new ValidationErrorException("Template format error: Unresolved condition dependencies " +
        unresolvedConditionDependencies + " in the Conditions block of the template");
    }
    try {
      for (String conditionName: conditionDependencyManager.dependencyList()) {
      JsonNode conditionJsonNode = conditionsJsonNode.get(conditionName);
      template.getConditionMap().get(conditionName).setConditionValue(FunctionEvaluation.evaluateFunctions(conditionJsonNode, template));
      template.getConditionMap().get(conditionName).setReady(true);
    }
    } catch (CyclicDependencyException ex) {
      throw new ValidationErrorException("Template error: Found circular condition dependency: " + ex.getMessage());
    }
  }

  private void conditionDependencyCrawl(String originalConditionName, JsonNode currentNode,
                                        DependencyManager conditionDependencyManager, Template template,
                                        Set<String> resourceReferences, Set<String> unresolvedConditionDependencies)
    throws CloudFormationException {
    if (currentNode == null) return;
    if (currentNode.isArray()) {
      for (int i = 0;i < currentNode.size(); i++) {
        conditionDependencyCrawl(originalConditionName, currentNode.get(i), conditionDependencyManager, template,
          resourceReferences, unresolvedConditionDependencies);
      }
    } else if (!currentNode.isObject()) {
      return;
    }
    // Now we are dealing with an object, perhaps a function
    // Check Fn::If
    IntrinsicFunction.MatchResult ifMatcher = IntrinsicFunctions.IF.evaluateMatch(currentNode);
    if (ifMatcher.isMatch()) {
      IntrinsicFunctions.IF.validateArgTypesWherePossible(ifMatcher);
      // we have a match against an "if"...
      String conditionName = currentNode.get(FunctionEvaluation.FN_IF).get(0).textValue();
      if (!template.getConditionMap().containsKey(conditionName)) {
        unresolvedConditionDependencies.add(conditionName);
      } else {
        conditionDependencyManager.addDependency(originalConditionName, conditionName);
      }
      return;
    }
    // Check "Condition"
    IntrinsicFunction.MatchResult conditionMatcher = IntrinsicFunctions.CONDITION.evaluateMatch(currentNode);
    if (conditionMatcher.isMatch()) {
      IntrinsicFunctions.CONDITION.validateArgTypesWherePossible(conditionMatcher);
      // we have a match against an "condition"...
      String conditionName = currentNode.get(FunctionEvaluation.CONDITION_STR).textValue();
      if (!template.getConditionMap().containsKey(conditionName)) {
        unresolvedConditionDependencies.add(conditionName);
      } else {
        conditionDependencyManager.addDependency(originalConditionName, conditionName);
      }
      return;
    }
    // Check "Ref" (only make sure not resource)
    IntrinsicFunction.MatchResult refMatcher = IntrinsicFunctions.REF.evaluateMatch(currentNode);
    if (refMatcher.isMatch()) {
      IntrinsicFunctions.REF.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = currentNode.get(FunctionEvaluation.REF_STR).textValue();
      if (!template.getReferenceMap().containsKey(refName) || template.getReferenceMap().get(refName) == null ||
        template.getReferenceMap().get(refName).getReferenceType() == Template.ReferenceType.Resource) {
        resourceReferences.add(refName);
      }
      return;
    }

    // Check "Fn::GetAtt" (make sure not resource or attribute)
    IntrinsicFunction.MatchResult fnAttMatcher = IntrinsicFunctions.GET_ATT.evaluateMatch(currentNode);
    if (fnAttMatcher.isMatch()) {
      IntrinsicFunctions.GET_ATT.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = currentNode.get(FunctionEvaluation.FN_GET_ATT).get(0).textValue();
      String attName = currentNode.get(FunctionEvaluation.FN_GET_ATT).get(1).textValue();
      // Not sure why, but AWS validates attribute types even in Conditions
      if (template.getResourceMap().containsKey(refName)) {
        Resource resource = template.getResourceMap().get(refName);
        if (!ResourceAttributeResolver.resourceHasAttribute(resource, attName)) {
          throw new ValidationErrorException("Template error: resource " + refName +
            " does not support attribute type " + attName + " in Fn::GetAtt");
        } else {
          resourceReferences.add(refName);
        }
      }
      return;
    }

    // Now either just an object or a different function.  Either way, crawl in the innards
    List<String> fieldNames = Lists.newArrayList(currentNode.fieldNames());
    for (String fieldName: fieldNames) {
      conditionDependencyCrawl(originalConditionName, currentNode.get(fieldName), conditionDependencyManager, template,
        resourceReferences, unresolvedConditionDependencies);
    }
  }
  private void buildResourceMap(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    // This is only done before everything else because Fn::GetAtt needs resource info to determine if it is a
    // "good" fit, which is done at "compile time"...
    JsonNode resourcesJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    if (resourcesJsonNode == null || resourcesJsonNode.size() == 0) {
      throw new ValidationErrorException("At least one " + TemplateSection.Resources + " member must be defined.");
    }
    List<String> resourceKeys = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    for (String resourceKey: resourceKeys) {
      JsonNode resourceJsonNode = resourcesJsonNode.get(resourceKey);
      if (!(resourceJsonNode.isObject())) {
        throw new ValidationErrorException("Template format error: Any Resources member must be a JSON object.");
      }
      String type = JSONHelper.getString(resourceJsonNode, "Type");
      if (type == null) {
        throw new ValidationErrorException("Type is a required property of Resource");
      }
      Resource resource = new ResourceResolverManager().resolveResource(type);
      if (resource == null) {
        throw new ValidationErrorException("Unknown resource type " + type);
      }
      template.getResourceMap().put(resourceKey, resource);
    }
  }

  private void parseResources(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode resourcesJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    List<String> resourceKeys = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    // make sure no duplicates betwen parameters and resources
    Set<String> commonParametersAndResources = Sets.intersection(Sets.newHashSet(resourceKeys),
      template.getReferenceMap().keySet());
    if (!commonParametersAndResources.isEmpty()) {
      throw new ValidationErrorException("Template error: all resources and parameters must have unique names. " +
        "Common name(s):"+commonParametersAndResources);
    }
    for (String resourceKey: resourceKeys) {
      Template.Reference reference = new Template.Reference();
      reference.setReady(false);
      reference.setReferenceName(resourceKey);
      reference.setReferenceValue(null);
      reference.setReferenceType(Template.ReferenceType.Resource);
      template.getReferenceMap().put(resourceKey, reference);
    }

    DependencyManager resourceDependencies = template.getResourceDependencyManager();

    for (String resourceKey: resourceKeys) {
      resourceDependencies.addNode(resourceKey);
    }
    // evaluate resource dependencies and do some type checking...
    Set<String> unresolvedResourceDependencies = Sets.newHashSet();

    for (String resourceKey: resourceKeys) {
      JsonNode resourceJsonNode = resourcesJsonNode.get(resourceKey);
      JsonNode dependsOnJsonNode = resourceJsonNode.get(ResourceKey.DependsOn.toString());
      if (dependsOnJsonNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(dependsOnJsonNode);
        if (dependsOnJsonNode.isArray()) {
          for (int i = 0;i < dependsOnJsonNode.size(); i++) {
            if (dependsOnJsonNode.get(i) != null && dependsOnJsonNode.get(i).isTextual()) {
              String dependeningOnResourceName = dependsOnJsonNode.get(i).textValue();
              if (!template.getReferenceMap().containsKey(dependeningOnResourceName) ||
                template.getReferenceMap().get(dependeningOnResourceName).getReferenceType()
                  != Template.ReferenceType.Resource) {
                unresolvedResourceDependencies.add(dependeningOnResourceName);
              } else {
                resourceDependencies.addDependency(resourceKey, dependeningOnResourceName);
              }
            } else {
              throw new ValidationErrorException("Template format error: Every DependsOn value must be a string.");
            }
          }
        } else if (dependsOnJsonNode.isTextual()) {
          String dependeningOnResourceName = dependsOnJsonNode.textValue();
          if (!template.getReferenceMap().containsKey(dependeningOnResourceName) ||
            template.getReferenceMap().get(dependeningOnResourceName).getReferenceType()
              != Template.ReferenceType.Resource) {
            unresolvedResourceDependencies.add(dependeningOnResourceName);
          } else {
            resourceDependencies.addDependency(resourceKey, dependeningOnResourceName);
          }
        } else {
          throw new ValidationErrorException("Template format error: DependsOn must be a string or list of strings.");
        }
      }
      Resource resource = template.getResourceMap().get(resourceKey);
      JsonNode metadataNode = JSONHelper.checkObject(resourceJsonNode, ResourceKey.Metadata.toString());
      if (metadataNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(metadataNode);
        resource.setMetadataJsonNode(metadataNode);
      }
      JsonNode propertiesNode = JSONHelper.checkObject(resourceJsonNode, ResourceKey.Properties.toString());
      if (propertiesNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(propertiesNode);
        resource.setPropertiesJsonNode(propertiesNode);
      }
      JsonNode updatePolicyNode = JSONHelper.checkObject(resourceJsonNode, ResourceKey.UpdatePolicy.toString());
      if (propertiesNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(propertiesNode);
        resource.setUpdatePolicyJsonNode(updatePolicyNode);
      }
      resource.setLogicalResourceId(resourceKey);
      resourceDependencyCrawl(resourceKey, metadataNode, resourceDependencies, template, unresolvedResourceDependencies);
      resourceDependencyCrawl(resourceKey, propertiesNode, resourceDependencies, template, unresolvedResourceDependencies);
      resourceDependencyCrawl(resourceKey, updatePolicyNode, resourceDependencies, template, unresolvedResourceDependencies);
      String deletionPolicy = JSONHelper.getString(resourceJsonNode, ResourceKey.DeletionPolicy.toString());
      if (deletionPolicy != null) {
        if (!DeletionPolicyValues.Delete.toString().equals(deletionPolicy)
          && !DeletionPolicyValues.Retain.equals(deletionPolicy)
          && !DeletionPolicyValues.Snapshot.equals(deletionPolicy)) {
          throw new ValidationErrorException("Template format error: Unrecognized DeletionPolicy " + deletionPolicy +
            " for resource " + resourceKey);
        }
        if (DeletionPolicyValues.Snapshot.equals(deletionPolicy) && !resource.supportsSnapshots()) {
          throw new ValidationErrorException("Template error: resource type " + resource.getType() + " does not support deletion policy Snapshot");
        }
        resource.setDeletionPolicy(deletionPolicy);
      }
      String conditionKey = JSONHelper.getString(resourceJsonNode, ResourceKey.Condition.toString());
      if (conditionKey != null) {
        if (!template.getConditionMap().containsKey(conditionKey)) {
          throw new ValidationErrorException("Template format error: Condition " + conditionKey + "  is not defined.");
        }
        Template.Condition condition = template.getConditionMap().get(conditionKey);
        if (!condition.isReady()) {
          throw new ValidationErrorException("Condition " + conditionKey + " has not been evaluated");
        }
        resource.setAllowedByCondition(FunctionEvaluation.evaluateBoolean(condition.getConditionValue()));
      }
    }
    if (!unresolvedResourceDependencies.isEmpty()) {
      throw new ValidationErrorException("Template format error: Unresolved resource dependencies " + unresolvedResourceDependencies + " in the Resources block of the template");
    }

    try {
      resourceDependencies.dependencyList(); // just to trigger the check...
    } catch (CyclicDependencyException ex) {
      throw new ValidationErrorException("Circular dependency between resources: " + ex.getMessage());
    }
  }

  private void resourceDependencyCrawl(String resourceKey, JsonNode jsonNode,
                                       DependencyManager resourceDependencies, Template template,
                                       Set<String> unresolvedResourceDependencies)
    throws CloudFormationException {
    if (jsonNode == null) {
      return;
    }
    if (jsonNode.isArray()) {
      for (int i=0;i<jsonNode.size();i++) {
        resourceDependencyCrawl(resourceKey, jsonNode.get(i), resourceDependencies, template, unresolvedResourceDependencies);
      }
    }
    // Now we are dealing with an object, perhaps a function
    // Check "Ref" (only make sure not resource)
    IntrinsicFunction.MatchResult refMatcher = IntrinsicFunctions.REF.evaluateMatch(jsonNode);
    if (refMatcher.isMatch()) {
      IntrinsicFunctions.REF.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.REF_STR).textValue();
      if (!template.getReferenceMap().containsKey(refName) || template.getReferenceMap().get(refName) == null) {
        unresolvedResourceDependencies.add(refName);
      } else if (template.getReferenceMap().get(refName).getReferenceType() == Template.ReferenceType.Resource) {
        resourceDependencies.addDependency(resourceKey, refName);
      }
      return;
    }

    // Check "Fn::GetAtt" (make sure not resource or attribute)
    IntrinsicFunction.MatchResult fnAttMatcher = IntrinsicFunctions.GET_ATT.evaluateMatch(jsonNode);
    if (fnAttMatcher.isMatch()) {
      IntrinsicFunctions.GET_ATT.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(0).textValue();
      String attName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(1).textValue();
      // Not sure why, but AWS validates attribute types even in Conditions
      if (template.getResourceMap().containsKey(refName)) {
        Resource resource = template.getResourceMap().get(refName);
        if (!ResourceAttributeResolver.resourceHasAttribute(resource, attName)) {
          throw new ValidationErrorException("Template error: resource " + refName +
            " does not support attribute type " + attName + " in Fn::GetAtt");
        } else {
          resourceDependencies.addDependency(resourceKey, refName);
        }
      } else {
        // not a resource...
        throw new ValidationErrorException("Template error: instance of Fn::GetAtt references undefined resource "
          + refName);
      }
      return;
    }

    // Now either just an object or a different function.  Either way, crawl in the innards
    List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
    for (String fieldName: fieldNames) {
      resourceDependencyCrawl(resourceKey, jsonNode.get(fieldName), resourceDependencies, template, unresolvedResourceDependencies);
    }
  }

  private void parseOutputs(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode outputsJsonNode = JSONHelper.checkObject(templateJsonNode, TemplateSection.Outputs.toString());
    if (outputsJsonNode != null) {
      List<String> outputKeys = (List<String>) Lists.newArrayList(outputsJsonNode.fieldNames());
      for (String outputKey: outputKeys) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(outputsJsonNode.get(outputKey));
        template.getOutputJsonNodeMap().put(outputKey, outputsJsonNode.get(outputKey));
      }
    }
  }
}

