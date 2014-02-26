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
import com.eucalyptus.cloudformation.resources.ResourceAttributeResolver;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
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
import org.apache.log4j.Logger;

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
  private static final String NO_ECHO_PARAMETER_VALUE = "****";
  private enum ParameterType {
    String,
    Number,
    CommaDelimitedList
  }

  private static class TemplateParameter {
    public TemplateParameter() {
    }

    private String parameterKey;
    private String parameterValue;
    private ParameterType type;
    private String defaultValue;
    private boolean noEcho;
    private String[] allowedValues;
    private String allowedPattern;
    private Double minLength;
    private Double maxLength;
    private Double minValue;
    private Double maxValue;
    private String description;
    private String constraintDescription; // 4000 chars max

    public ParameterType getType() {
      return type;
    }

    public String getParameterKey() {
      return parameterKey;
    }

    public void setParameterKey(String parameterKey) {
      this.parameterKey = parameterKey;
    }

    public String getParameterValue() {
      return parameterValue;
    }

    public void setParameterValue(String parameterValue) {
      this.parameterValue = parameterValue;
    }

    public void setType(ParameterType type) {
      this.type = type;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    public boolean isNoEcho() {
      return noEcho;
    }

    public void setNoEcho(boolean noEcho) {
      this.noEcho = noEcho;
    }

    public String[] getAllowedValues() {
      return allowedValues;
    }

    public void setAllowedValues(String[] allowedValues) {
      this.allowedValues = allowedValues;
    }

    public String getAllowedPattern() {
      return allowedPattern;
    }

    public void setAllowedPattern(String allowedPattern) {
      this.allowedPattern = allowedPattern;
    }

    public Double getMinLength() {
      return minLength;
    }

    public void setMinLength(Double minLength) {
      this.minLength = minLength;
    }

    public Double getMaxLength() {
      return maxLength;
    }

    public void setMaxLength(Double maxLength) {
      this.maxLength = maxLength;
    }

    public Double getMinValue() {
      return minValue;
    }

    public void setMinValue(Double minValue) {
      this.minValue = minValue;
    }

    public Double getMaxValue() {
      return maxValue;
    }

    public void setMaxValue(Double maxValue) {
      this.maxValue = maxValue;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getConstraintDescription() {
      return constraintDescription;
    }

    public void setConstraintDescription(String constraintDescription) {
      this.constraintDescription = constraintDescription;
    }

    @Override
    public String toString() {
      return "Parameter{" +
        "parameterKey='" + parameterKey + '\'' +
        ", parameterValue='" + parameterValue + '\'' +
        ", type=" + type +
        ", defaultValue='" + defaultValue + '\'' +
        ", noEcho=" + noEcho +
        ", allowedValues=" + Arrays.toString(allowedValues) +
        ", allowedPattern='" + allowedPattern + '\'' +
        ", minLength=" + minLength +
        ", maxLength=" + maxLength +
        ", minValue=" + minValue +
        ", maxValue=" + maxValue +
        ", description='" + description + '\'' +
        ", constraintDescription='" + constraintDescription + '\'' +
        '}';
    }
  }

  private static final Logger LOG = Logger.getLogger(TemplateParser.class);
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

  public static final String AWS_ACCOUNT_ID = "AWS::AccountId";
  public static final String AWS_NOTIFICATION_ARNS = "AWS::NotificationARNs";
  public static final String AWS_NO_VALUE = "AWS::NoValue";
  public static final String AWS_REGION = "AWS::Region";
  public static final String AWS_STACK_ID = "AWS::StackId";
  public static final String AWS_STACK_NAME = "AWS::StackName";

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
    template.getPseudoParameterMap().put(AWS_ACCOUNT_ID, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getAccountId())));

    ArrayNode notificationsArnNode = objectMapper.createArrayNode();
    for (String notificationArn: pseudoParameterValues.getNotificationArns()) {
      notificationsArnNode.add(notificationArn);
    }
    template.getPseudoParameterMap().put(AWS_NOTIFICATION_ARNS, JsonHelper.getStringFromJsonNode(notificationsArnNode));

    ObjectNode noValueNode = mapper.createObjectNode();
    noValueNode.put(FunctionEvaluation.REF_STR, AWS_NO_VALUE);
    template.getPseudoParameterMap().put(AWS_NO_VALUE, JsonHelper.getStringFromJsonNode(noValueNode));

    template.getPseudoParameterMap().put(AWS_REGION, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getRegion())));

    template.getPseudoParameterMap().put(AWS_STACK_ID, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getStackId())));

    template.getPseudoParameterMap().put(AWS_STACK_NAME, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getStackName())));

    // More like an intrinsic function, but still do it here...
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
    String templateFormatVersion = JsonHelper.getString(templateJsonNode,
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
    String description = JsonHelper.getString(templateJsonNode, TemplateSection.Description.toString());
    if (description == null) return;
    if (description.length() > 4000) {
      throw new ValidationErrorException("Template format error: " + TemplateSection.Description + " must "
        + "be no longer than 4000 characters.");
    }
    template.setDescription(description);
  }


  private void parseMappings(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode mappingsJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Mappings.toString());
    if (mappingsJsonNode == null) return;
    for (String mapName: Lists.newArrayList(mappingsJsonNode.fieldNames())) {
      JsonNode mappingJsonNode = JsonHelper.checkObject(mappingsJsonNode, mapName, "Every "
        + TemplateSection.Mappings + " member " + mapName + " must be a map");

      for (String mapKey: Lists.newArrayList(mappingJsonNode.fieldNames())) {
        JsonNode attributesJsonNode = JsonHelper.checkObject(mappingJsonNode, mapKey, "Every "
          + TemplateSection.Mappings + " member " + mapKey + " must be a map");
        for (String attribute: Lists.newArrayList(attributesJsonNode.fieldNames())) {
          JsonNode valueJsonNode = JsonHelper.checkStringOrArray(attributesJsonNode, attribute, "Every "
            + TemplateSection.Mappings + " attribute must be a String or a List.");
          if (!template.getMapping().containsKey(mapName)) {
            template.getMapping().put(mapName, Maps.<String, Map<String, String>>newLinkedHashMap());
          }
          if (!template.getMapping().get(mapName).containsKey(mapKey)) {
            template.getMapping().get(mapName).put(mapKey, Maps.<String, String>newLinkedHashMap());
          }
          template.getMapping().get(mapName).get(mapKey).put(attribute, JsonHelper.getStringFromJsonNode(valueJsonNode));
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
    JsonNode parametersJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Parameters.toString());
    if (parametersJsonNode == null) return;
    Set<String> parameterNames = Sets.newHashSet(parametersJsonNode.fieldNames());
    Set<String> noValueParameters = Sets.newHashSet();
    for (String parameterName: parameterNames) {
      JsonNode parameterJsonNode = JsonHelper.checkObject(parametersJsonNode, parameterName, "Any "
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
    TemplateParameter templateParameter = new TemplateParameter();
    String type = JsonHelper.getString(parameterJsonNode, ParameterKey.Type.toString());
    if (type == null) {
      throw new ValidationErrorException("Template format error: Every " + TemplateSection.Parameters + " object "
        + "must contain a " +  ParameterKey.Type +" member.");
    }
    ParameterType parameterType = null;
    try {
      parameterType = ParameterType.valueOf(type);
    } catch (Exception ex) {
      throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + type);
    }
    JsonNode allowedValuesJsonNode = JsonHelper.checkArray(parameterJsonNode, ParameterKey.AllowedValues.toString());
    if (allowedValuesJsonNode != null) {
      String[] allowedValues = new String[allowedValuesJsonNode.size()];
      for (int index=0;index<allowedValues.length; index++) {
        String errorMsg = "Every " + ParameterKey.AllowedValues + "value must be a string.";
        String allowedValue = JsonHelper.getString(allowedValuesJsonNode, index, errorMsg);
        if (allowedValue == null) {
          throw new ValidationErrorException("Template format error: " + errorMsg);
        }
        allowedValues[index] = allowedValue;
      }
      templateParameter.setAllowedValues(allowedValues);
    }
    templateParameter.setAllowedPattern(JsonHelper.getString(parameterJsonNode, ParameterKey.AllowedPattern.toString()));
    String constraintDescription = JsonHelper.getString(parameterJsonNode,
      ParameterKey.ConstraintDescription.toString());
    if (constraintDescription != null && constraintDescription.length() > 4000) {
      throw new ValidationErrorException("Template format error: " +
        ParameterKey.ConstraintDescription + " must be no longer than 4000 characters.");
    }
    templateParameter.setConstraintDescription(constraintDescription);
    templateParameter.setDefaultValue(JsonHelper.getString(parameterJsonNode, ParameterKey.Default.toString()));
    String description = JsonHelper.getString(parameterJsonNode, ParameterKey.Description.toString());
    if (description != null && description.length() > 4000) {
      throw new ValidationErrorException("Template format error: " + ParameterKey.Description + " must be no "
        + "longer than 4000 characters.");
    }
    templateParameter.setDescription(description);
    templateParameter.setMaxLength(JsonHelper.getDouble(parameterJsonNode, ParameterKey.MaxLength.toString()));
    templateParameter.setMinLength(JsonHelper.getDouble(parameterJsonNode, ParameterKey.MinLength.toString()));
    templateParameter.setMaxValue(JsonHelper.getDouble(parameterJsonNode, ParameterKey.MaxValue.toString()));
    templateParameter.setMinValue(JsonHelper.getDouble(parameterJsonNode, ParameterKey.MinValue.toString()));
    templateParameter.setDefaultValue(JsonHelper.getString(parameterJsonNode, ParameterKey.Default.toString()));
    templateParameter.setNoEcho("true".equalsIgnoreCase(JsonHelper.getString(parameterJsonNode,
      ParameterKey.NoEcho.toString())));
    templateParameter.setParameterKey(parameterName);
    templateParameter.setType(parameterType);
    templateParameter.setParameterValue(paramMap.get(parameterName) != null ? paramMap.get(parameterName) :
      templateParameter.getDefaultValue());
    if (templateParameter.getParameterValue() == null) {
      noValueParameters.add(parameterName);
      return;
    }
    if (templateParameter.getAllowedValues() != null
      && !Arrays.asList(templateParameter.getAllowedValues()).contains(templateParameter.getParameterValue())) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterName + "' must be one of " + ParameterKey.AllowedValues
      );
    }
    JsonNode referenceValue = null; // reference value will be a JsonNode so we have a common object when evaluating.
    switch(parameterType) {
      case Number:
        parseNumberParameter(parameterName, templateParameter);
        referenceValue = new TextNode(templateParameter.getParameterValue());
        break;
      case String:
        parseStringParameter(parameterName, templateParameter);
        referenceValue = new TextNode(templateParameter.getParameterValue());
        break;
      case CommaDelimitedList:
        parseCommaDelimitedListParameter(parameterName, templateParameter);
        referenceValue = objectMapper.createArrayNode();
        StringTokenizer tokenizer = new StringTokenizer(templateParameter.getParameterValue(), ",");
        while (tokenizer.hasMoreTokens()) {
          ((ArrayNode) referenceValue).add(tokenizer.nextToken());
        }
        break;
      default:
        throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + parameterType);
    }
    Parameter parameter = new Parameter();
    parameter.setParameterKey(templateParameter.getParameterKey());
    parameter.setParameterValue(templateParameter.isNoEcho() ? NO_ECHO_PARAMETER_VALUE : templateParameter.getParameterValue());
    template.addNoEchoFilteredParameter(parameter);
    template.getParameterMap().put(templateParameter.getParameterKey(), JsonHelper.getStringFromJsonNode(referenceValue));
  }

  private void parseCommaDelimitedListParameter(String parameterKey, TemplateParameter templateParameter)
    throws CloudFormationException {
    if (templateParameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinLength
        + " must be on a parameter of type String");
    }
    if (templateParameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxLength +
        " must be on a parameter of type String");
    }
    if (templateParameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " +
        ParameterKey.AllowedPattern + " must be on a parameter of type String");
    }
    if (templateParameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " +
        ParameterKey.MinValue + " must be on a parameter of type Number");
    }
    if (templateParameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxValue +
        " must be on a parameter of type Number");
    }
  }

  private void parseStringParameter(String parameterKey, TemplateParameter templateParameter) throws CloudFormationException {
    // boot out the unallowed parameters
    if (templateParameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be on a parameter of type Number");
    }
    if (templateParameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxValue
        + " must be on a parameter of type Number");
    }
    if (templateParameter.getMaxLength() != null && templateParameter.getMinLength() != null
      && templateParameter.getMaxLength() < templateParameter.getMinLength()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be less than " + ParameterKey.MaxValue + ".");
    }
    if (templateParameter.getMinLength() != null && templateParameter.getMinLength() > templateParameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at least " + templateParameter.getMinLength()
            + " characters"
      );
    }
    if (templateParameter.getMaxLength() != null && templateParameter.getMaxLength() < templateParameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at most " + templateParameter.getMaxLength()
            + " characters"
      );
    }
    if (templateParameter.getAllowedPattern() != null) {
      try {
        if (!templateParameter.getParameterValue().matches(templateParameter.getAllowedPattern())) {
          throw new ValidationErrorException(
            templateParameter.getConstraintDescription() != null ?
              templateParameter.getConstraintDescription() :
              "Template error: Parameter '" + parameterKey + "' must match pattern " + templateParameter.getAllowedPattern()
          );
        }
      } catch (PatternSyntaxException ex) {
        throw new ValidationErrorException("Parameter '"+parameterKey+"' " + ParameterKey.AllowedPattern
          + " must be a valid regular expression.");
      }
    }
  }

  private void parseNumberParameter(String parameterKey, TemplateParameter templateParameter) throws CloudFormationException {
    // boot out the unallowed parameters
    if (templateParameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinLength
        + " must be on a parameter of type String");
    }
    if (templateParameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MaxLength
        + " must be on a parameter of type String");
    }
    if (templateParameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' "
        + ParameterKey.AllowedPattern + " must be on a parameter of type String");
    }
    if (templateParameter.getMaxValue() != null && templateParameter.getMinValue() != null
      && templateParameter.getMaxValue() < templateParameter.getMinValue()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' " + ParameterKey.MinValue
        + " must be less than " + ParameterKey.MaxValue + ".");
    }
    Double valueDouble = null;
    try {
      valueDouble = Double.parseDouble(templateParameter.getParameterValue());
    } catch (NumberFormatException ex) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number"
      );
    }
    if (templateParameter.getMinValue() != null && templateParameter.getMinValue() > valueDouble) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number not less than " + templateParameter.getMinValue()
      );
    }
    if (templateParameter.getMaxValue() != null && templateParameter.getMaxValue() < valueDouble) {
      throw new ValidationErrorException(
        templateParameter.getConstraintDescription() != null ?
          templateParameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must be a number not greater than "
            + templateParameter.getMaxValue()
      );
    }
  }

  private void parseConditions(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode conditionsJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Conditions.toString());
    if (conditionsJsonNode == null) return;
    Set<String> conditionNames = Sets.newLinkedHashSet(Lists.newArrayList(conditionsJsonNode.fieldNames()));
    DependencyManager conditionDependencyManager = new DependencyManager();
    for (String conditionName: conditionNames) {
      conditionDependencyManager.addNode(conditionName);
    }
    // Now crawl for dependencies and make sure no resource references...
    Set<String> resourceReferences = Sets.newLinkedHashSet();
    Set<String> unresolvedConditionDependencies = Sets.newLinkedHashSet();
    for (String conditionName: conditionNames) {
      JsonNode conditionJsonNode = JsonHelper.checkObject(conditionsJsonNode, conditionName, "Any "
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
        template.getConditionMap().put(conditionName, FunctionEvaluation.evaluateBoolean(FunctionEvaluation.evaluateFunctions(conditionJsonNode, template)));
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
      if (!conditionDependencyManager.containsNode(conditionName)) {
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
      if (!conditionDependencyManager.containsNode(conditionName)) {
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
      // it's ok if it's a psueodparameter or a parameter, but not a resource, or doesn't exist
      if (!template.getPseudoParameterMap().containsKey(refName) && !template.getParameterMap().containsKey(refName)) {
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
        ResourceInfo resourceInfo = template.getResourceMap().get(refName);
        if (resourceInfo.canCheckAttributes() && !ResourceAttributeResolver.resourceHasAttribute(resourceInfo, attName)) {
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
    JsonNode resourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    if (resourcesJsonNode == null || resourcesJsonNode.size() == 0) {
      throw new ValidationErrorException("At least one " + TemplateSection.Resources + " member must be defined.");
    }
    List<String> resourceKeys = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    for (String resourceKey: resourceKeys) {
      JsonNode resourceJsonNode = resourcesJsonNode.get(resourceKey);
      if (!(resourceJsonNode.isObject())) {
        throw new ValidationErrorException("Template format error: Any Resources member must be a JSON object.");
      }
      String type = JsonHelper.getString(resourceJsonNode, "Type");
      if (type == null) {
        throw new ValidationErrorException("Type is a required property of Resource");
      }
      ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(type);
      if (resourceInfo == null) {
        throw new ValidationErrorException("Unknown resource type " + type);
      }
      template.getResourceMap().put(resourceKey, resourceInfo);
    }
  }

  private void parseResources(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode resourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    List<String> resourceKeys = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    // make sure no duplicates betwen parameters and resources
    Set<String> commonParametersAndResources = Sets.intersection(Sets.newHashSet(resourceKeys),
      Sets.union(template.getParameterMap().keySet(), template.getPseudoParameterMap().keySet()));
    if (!commonParametersAndResources.isEmpty()) {
      throw new ValidationErrorException("Template error: all resources and parameters must have unique names. " +
        "Common name(s):"+commonParametersAndResources);
    }

    DependencyManager resourceDependencies = template.getResourceDependencyManager();

    for (String resourceKey: resourceKeys) {
      resourceDependencies.addNode(resourceKey);
    }
    // evaluate resource dependencies and do some type checking...
    Set<String> unresolvedResourceDependencies = Sets.newLinkedHashSet();

    for (String resourceKey: resourceKeys) {
      JsonNode resourceJsonNode = resourcesJsonNode.get(resourceKey);
      JsonNode dependsOnJsonNode = resourceJsonNode.get(ResourceKey.DependsOn.toString());
      if (dependsOnJsonNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(dependsOnJsonNode);
        if (dependsOnJsonNode.isArray()) {
          for (int i = 0;i < dependsOnJsonNode.size(); i++) {
            if (dependsOnJsonNode.get(i) != null && dependsOnJsonNode.get(i).isTextual()) {
              String dependeningOnResourceName = dependsOnJsonNode.get(i).textValue();
              if (!template.getResourceMap().containsKey(dependeningOnResourceName)) {
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
          if (!template.getResourceMap().containsKey(dependeningOnResourceName)) {
            unresolvedResourceDependencies.add(dependeningOnResourceName);
          } else {
            resourceDependencies.addDependency(resourceKey, dependeningOnResourceName);
          }
        } else {
          throw new ValidationErrorException("Template format error: DependsOn must be a string or list of strings.");
        }
      }
      ResourceInfo resourceInfo = template.getResourceMap().get(resourceKey);
      JsonNode metadataNode = JsonHelper.checkObject(resourceJsonNode, ResourceKey.Metadata.toString());
      if (metadataNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(metadataNode);
        resourceInfo.setMetadataJson(JsonHelper.getStringFromJsonNode(metadataNode));
      }
      JsonNode propertiesNode = JsonHelper.checkObject(resourceJsonNode, ResourceKey.Properties.toString());
      if (propertiesNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(propertiesNode);
        resourceInfo.setPropertiesJson(JsonHelper.getStringFromJsonNode(propertiesNode));
      }
      JsonNode updatePolicyNode = JsonHelper.checkObject(resourceJsonNode, ResourceKey.UpdatePolicy.toString());
      if (propertiesNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(propertiesNode);
        resourceInfo.setUpdatePolicyJson(JsonHelper.getStringFromJsonNode(updatePolicyNode));
      }
      resourceInfo.setLogicalResourceId(resourceKey);
      resourceDependencyCrawl(resourceKey, metadataNode, resourceDependencies, template, unresolvedResourceDependencies);
      resourceDependencyCrawl(resourceKey, propertiesNode, resourceDependencies, template, unresolvedResourceDependencies);
      resourceDependencyCrawl(resourceKey, updatePolicyNode, resourceDependencies, template, unresolvedResourceDependencies);
      String deletionPolicy = JsonHelper.getString(resourceJsonNode, ResourceKey.DeletionPolicy.toString());
      if (deletionPolicy != null) {
        if (!DeletionPolicyValues.Delete.toString().equals(deletionPolicy)
          && !DeletionPolicyValues.Retain.equals(deletionPolicy)
          && !DeletionPolicyValues.Snapshot.equals(deletionPolicy)) {
          throw new ValidationErrorException("Template format error: Unrecognized DeletionPolicy " + deletionPolicy +
            " for resource " + resourceKey);
        }
        if (DeletionPolicyValues.Snapshot.equals(deletionPolicy) && !resourceInfo.supportsSnapshot()) {
          throw new ValidationErrorException("Template error: resource type " + resourceInfo.getType() + " does not support deletion policy Snapshot");
        }
        resourceInfo.setDeletionPolicy(deletionPolicy);
      }
      String conditionKey = JsonHelper.getString(resourceJsonNode, ResourceKey.Condition.toString());
      if (conditionKey != null) {
        if (!template.getConditionMap().containsKey(conditionKey)) {
          throw new ValidationErrorException("Template format error: Condition " + conditionKey + "  is not defined.");
        }
        resourceInfo.setAllowedByCondition(template.getConditionMap().get(conditionKey));
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
      if (template.getResourceMap().containsKey(refName)) {
        resourceDependencies.addDependency(resourceKey, refName);
      } else if (!template.getParameterMap().containsKey(refName) &&
        !template.getPseudoParameterMap().containsKey(refName)) {
        unresolvedResourceDependencies.add(refName);
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
        ResourceInfo resourceInfo = template.getResourceMap().get(refName);
        if (resourceInfo.canCheckAttributes() && !ResourceAttributeResolver.resourceHasAttribute(resourceInfo, attName)) {
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
    JsonNode outputsJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Outputs.toString());
    if (outputsJsonNode != null) {
      List<String> outputKeys = Lists.newArrayList(outputsJsonNode.fieldNames());
      for (String outputKey: outputKeys) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(outputsJsonNode.get(outputKey));
        template.getOutputJsonMap().put(outputKey, JsonHelper.getStringFromJsonNode(outputsJsonNode.get(outputKey)));
      }
    }
  }
}

