/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import com.eucalyptus.cloudformation.*;
import com.eucalyptus.cloudformation.common.msgs.GetTemplateSummaryResult;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.cloudformation.common.msgs.ParameterConstraints;
import com.eucalyptus.cloudformation.common.msgs.ParameterDeclaration;
import com.eucalyptus.cloudformation.common.msgs.ParameterDeclarations;
import com.eucalyptus.cloudformation.common.msgs.ResourceList;
import com.eucalyptus.cloudformation.common.msgs.TemplateParameter;
import com.eucalyptus.cloudformation.common.msgs.TemplateParameters;
import com.eucalyptus.cloudformation.common.msgs.ValidateTemplateResult;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.cloudformation.template.dependencies.CyclicDependencyException;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.eucalyptus.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;


/**
 * Created by ethomas on 12/10/13.
 */
public class TemplateParser {
  private static final Logger LOG = Logger.getLogger(TemplateParser.class);
  public TemplateParser() {
  }

  public enum Capabilities {
    CAPABILITY_IAM,
    CAPABILITY_NAMED_IAM
  }

  private enum TemplateSection {
    Metadata,
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
    Type,
    Properties,
    DeletionPolicy,
    Description,
    DependsOn,
    Metadata,
    UpdatePolicy,
    CreationPolicy,
    Condition
  }

  private enum DeletionPolicyValues {
    Delete,
    Retain,
    Snapshot
  }

  private enum OutputKey {
    Description,
    Condition,
    Value
  };

  public static final String AWS_ACCOUNT_ID = "AWS::AccountId";
  public static final String AWS_NOTIFICATION_ARNS = "AWS::NotificationARNs";
  public static final String AWS_NO_VALUE = "AWS::NoValue";
  public static final String AWS_REGION = "AWS::Region";
  public static final String AWS_STACK_ID = "AWS::StackId";
  public static final String AWS_STACK_NAME = "AWS::StackName";

  private static final String DEFAULT_TEMPLATE_VERSION = "2010-09-09";
  private static final String[] validTemplateVersions = new String[] {DEFAULT_TEMPLATE_VERSION};

  public Template parse(String templateBody, List<Parameter> userParameters, List<String> capabilities, PseudoParameterValues pseudoParameterValues, String effectiveUserId, boolean enforceStrictResourceProperties) throws CloudFormationException {
    Template template = new Template();
    template.setResourceInfoMap(Maps.<String, ResourceInfo>newLinkedHashMap());
    JsonNode templateJsonNode;
    try {
      templateJsonNode = Json.parse( templateBody );
    } catch (IOException ex) {
      throw new ValidationErrorException(ex.getMessage());
    }
    if (!templateJsonNode.isObject()) {
      throw new ValidationErrorException("Template body is not a JSON object");
    }
    template.setTemplateBody(templateBody);
    addPseudoParameters(template, pseudoParameterValues);
    buildResourceMap(template, templateJsonNode, enforceStrictResourceProperties);
    parseValidTopLevelKeys(templateJsonNode);
    parseVersion(template, templateJsonNode);
    parseMetadata(template, templateJsonNode);
    parseDescription(template, templateJsonNode);
    parseMappings(template, templateJsonNode);
    ParameterParser.parseParameters(template, templateJsonNode, userParameters, false);
    parseConditions(template, templateJsonNode, false, effectiveUserId);
    parseResources(template, templateJsonNode, false);

    Map<String, JsonNode> resourcePropertyMap = createResourcePropertiesMap(templateJsonNode);
    Set<String> requiredCapabilities = Sets.newHashSet();

    capabilities = addIAMCapabilityIfNamed(capabilities);

    for (Map.Entry<String, ResourceInfo> resourceInfoEntry: template.getResourceInfoMap().entrySet()) {
      String resourceName = resourceInfoEntry.getKey();
      ResourceInfo resourceInfo = resourceInfoEntry.getValue();
      JsonNode resourcePropertiesJson = resourcePropertyMap.get(resourceName);
      String resourceType = resourceInfo.getType();
      Collection resourceRequiredCapabilities = resourceInfo.getRequiredCapabilities(resourcePropertiesJson);
      if (resourceRequiredCapabilities != null && !resourceRequiredCapabilities.isEmpty()) {
        requiredCapabilities.addAll(resourceRequiredCapabilities);
      }
    }

    requiredCapabilities = consolidateIAMCapabilities(requiredCapabilities);

    Set<String> missingRequiredCapabilities = Sets.newLinkedHashSet();
    if (!requiredCapabilities.isEmpty()) {
      for (String requiredCapability:requiredCapabilities) {
        if (capabilities == null || !capabilities.contains(requiredCapability)) {
          missingRequiredCapabilities.add(requiredCapability);
        }
      }
    }
    if (!missingRequiredCapabilities.isEmpty()) {
      throw new InsufficientCapabilitiesException("Required capabilities:" + missingRequiredCapabilities);
    }
    parseOutputs(template, templateJsonNode);
    return template;
  }

  private List<String> addIAMCapabilityIfNamed(List<String> capabilities) {
    List<String> newCapabilities = Lists.newArrayList();
    if (capabilities != null) {
      newCapabilities.addAll(capabilities);
    }
    // CAPABILITY_NAMED_IAM also gives CAPABILITY_IAM capability
    if (newCapabilities.contains(Capabilities.CAPABILITY_NAMED_IAM.toString()) &&
      !newCapabilities.contains(Capabilities.CAPABILITY_IAM.toString())) {
      newCapabilities.add(Capabilities.CAPABILITY_IAM.toString());
    }
    return newCapabilities;
  }

  private void parseMetadata(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    JsonNode metadataResourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Metadata.toString());
    template.setMetadataJSON(JsonHelper.getStringFromJsonNode(metadataResourcesJsonNode));
  }

  public ValidateTemplateResult validateTemplate( String templateBody, List<Parameter> userParameters, PseudoParameterValues pseudoParameterValues, String effectiveUserId, boolean enforceStrictResourceProperties) throws CloudFormationException {
    GetTemplateSummaryResult getTemplateSummaryResult = getTemplateSummary(templateBody, userParameters, pseudoParameterValues, effectiveUserId, enforceStrictResourceProperties);
    ValidateTemplateResult validateTemplateResult = new ValidateTemplateResult();
    validateTemplateResult.setDescription(getTemplateSummaryResult.getDescription());
    validateTemplateResult.setCapabilities(getTemplateSummaryResult.getCapabilities());
    validateTemplateResult.setCapabilitiesReason(getTemplateSummaryResult.getCapabilitiesReason());

    TemplateParameters templateParameters = new TemplateParameters();
    if (getTemplateSummaryResult.getParameters() != null && getTemplateSummaryResult.getParameters().getMember() != null) {
      templateParameters.setMember(Lists.<TemplateParameter>newArrayList());
      for (ParameterDeclaration parameterDeclaration: getTemplateSummaryResult.getParameters().getMember()) {
        TemplateParameter templateParameter = new TemplateParameter();
        templateParameter.setDefaultValue(parameterDeclaration.getDefaultValue());
        templateParameter.setDescription(parameterDeclaration.getDescription());
        templateParameter.setNoEcho(parameterDeclaration.getNoEcho());
        templateParameter.setParameterKey(parameterDeclaration.getParameterKey());
        templateParameters.getMember().add(templateParameter);
      }
    }
    validateTemplateResult.setParameters(templateParameters);
    return validateTemplateResult;
  }

  public GetTemplateSummaryResult getTemplateSummary(String templateBody, List<Parameter> userParameters, PseudoParameterValues pseudoParameterValues, String effectiveUserId, boolean enforceStrictResourceProperties) throws CloudFormationException {
    Template template = new Template();
    template.setResourceInfoMap(Maps.<String, ResourceInfo>newLinkedHashMap());
    JsonNode templateJsonNode;
    try {
      templateJsonNode = Json.parse(templateBody);
    } catch (IOException ex) {
      throw new ValidationErrorException(ex.getMessage());
    }
    if (!templateJsonNode.isObject()) {
      throw new ValidationErrorException("Template body is not a JSON object");
    }
    template.setTemplateBody(templateBody);
    addPseudoParameters(template, pseudoParameterValues);
    buildResourceMap(template, templateJsonNode, enforceStrictResourceProperties);
    parseValidTopLevelKeys(templateJsonNode);
    parseVersion(template, templateJsonNode);
    parseDescription(template, templateJsonNode);
    parseMetadata(template, templateJsonNode);
    parseMappings(template, templateJsonNode);
    ParameterParser.parseParameters(template, templateJsonNode, userParameters, true);
    parseConditions(template, templateJsonNode, true, effectiveUserId);
    parseResources(template, templateJsonNode, true);
    parseOutputs(template, templateJsonNode);

    Set<String> capabilitiesResourceTypes = Sets.newLinkedHashSet();
    Set<String> requiredCapabilities = Sets.newLinkedHashSet();
    Set<String> resourceTypes = Sets.newLinkedHashSet();
    Map<String, JsonNode> resourcePropertyMap = createResourcePropertiesMap(templateJsonNode);


    for (Map.Entry<String, ResourceInfo> resourceInfoEntry: template.getResourceInfoMap().entrySet()) {
      String resourceName = resourceInfoEntry.getKey();
      ResourceInfo resourceInfo = resourceInfoEntry.getValue();
      JsonNode resourcePropertiesJson = resourcePropertyMap.get(resourceName);
      String resourceType = resourceInfo.getType();
      resourceTypes.add(resourceType);
      Collection resourceRequiredCapabilities = resourceInfo.getRequiredCapabilities(resourcePropertiesJson);
       if (resourceRequiredCapabilities != null && !resourceRequiredCapabilities.isEmpty()) {
        requiredCapabilities.addAll(resourceRequiredCapabilities);
        capabilitiesResourceTypes.add(resourceType);
      }
    }

    requiredCapabilities = consolidateIAMCapabilities(requiredCapabilities);

    GetTemplateSummaryResult getTemplateSummaryResult = new GetTemplateSummaryResult();
    getTemplateSummaryResult.setDescription(template.getDescription());
    getTemplateSummaryResult.setCapabilities(new ResourceList());
    getTemplateSummaryResult.getCapabilities().setMember(Lists.newArrayList(requiredCapabilities));
    if (!requiredCapabilities.isEmpty()) {
      getTemplateSummaryResult.setCapabilitiesReason("The following resource(s) require capabilities: " + capabilitiesResourceTypes);
    }
    getTemplateSummaryResult.setParameters(new ParameterDeclarations());
    getTemplateSummaryResult.getParameters().setMember(template.getParameterDeclarations());
    getTemplateSummaryResult.setMetadata(template.getMetadataJSON());
    getTemplateSummaryResult.setResourceTypes(new ResourceList());
    getTemplateSummaryResult.getResourceTypes().setMember(Lists.newArrayList(resourceTypes));
    return getTemplateSummaryResult;
  }

  private Set<String> consolidateIAMCapabilities(Set<String> capabilities) {
    Set<String> newCapabilities = Sets.newHashSet();
    if (capabilities != null) {
      newCapabilities.addAll(capabilities);
    }
    // Hack: CAPABILITY_NAMED_IAM is a stronger condition than CAPABILITY_IAM, so remove the latter requirement if
    // the former is found
    if (newCapabilities.contains(Capabilities.CAPABILITY_NAMED_IAM.toString())) {
      newCapabilities.remove(Capabilities.CAPABILITY_IAM.toString());
    }
    return newCapabilities;
  }

  private Map<String, JsonNode> createResourcePropertiesMap(JsonNode templateJsonNode) throws CloudFormationException {
    Map<String, JsonNode> map = Maps.newHashMap();
    JsonNode resourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    for (String name: Lists.newArrayList(resourcesJsonNode.fieldNames())) {
      map.put(name, resourcesJsonNode.get(name).get(ResourceKey.Properties.toString()));
    }
    return map;
  }


  private void addPseudoParameters(Template template, PseudoParameterValues pseudoParameterValues) throws CloudFormationException {
    // The reason all of this json wrapping is going around is because evaluating a pseudoparameter needs to result in a json node (as it could be an array or string)
    Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
    pseudoParameterMap.put(AWS_ACCOUNT_ID, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getAccountId())));

    ArrayNode notificationsArnNode = JsonHelper.createArrayNode( );
    if (pseudoParameterValues.getNotificationArns() != null) {
      for (String notificationArn: pseudoParameterValues.getNotificationArns()) {
        notificationsArnNode.add(notificationArn);
      }
    }
    pseudoParameterMap.put(AWS_NOTIFICATION_ARNS, JsonHelper.getStringFromJsonNode(notificationsArnNode));

    ObjectNode noValueNode = JsonHelper.createObjectNode( );
    noValueNode.put(FunctionEvaluation.REF_STR, AWS_NO_VALUE);
    pseudoParameterMap.put(AWS_NO_VALUE, JsonHelper.getStringFromJsonNode(noValueNode));

    pseudoParameterMap.put(AWS_REGION, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getRegion())));

    pseudoParameterMap.put(AWS_STACK_ID, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getStackId())));

    pseudoParameterMap.put(AWS_STACK_NAME, JsonHelper.getStringFromJsonNode(new TextNode(pseudoParameterValues.getStackName())));
    template.setPseudoParameterMap(pseudoParameterMap);
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
    if (description.getBytes().length > Limits.TEMPLATE_DESCRIPTION_MAX_LENGTH_BYTES) {
      throw new ValidationErrorException("Template format error: " + TemplateSection.Description + " must "
        + "be no longer than " + Limits.TEMPLATE_DESCRIPTION_MAX_LENGTH_BYTES + " bytes");
    }
    template.setDescription(description);
  }


  private void parseMappings(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    Map<String, Map<String, Map<String, String>>> mapping = template.getMapping();
    JsonNode mappingsJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Mappings.toString());
    if (mappingsJsonNode == null) return;
    for (String mapName: Lists.newArrayList(mappingsJsonNode.fieldNames())) {
      if (mapName.length() > Limits.MAPPING_NAME_MAX_LENGTH_CHARS) {
        throw new ValidationErrorException("Mapping name " + mapName + " exceeds the maximum number of allowed characters (" + Limits.MAPPING_NAME_MAX_LENGTH_CHARS + ")");
      }
      JsonNode mappingJsonNode = JsonHelper.checkObject(mappingsJsonNode, mapName, "Every "
        + TemplateSection.Mappings + " member " + mapName + " must be a map");

      for (String mapKey: Lists.newArrayList(mappingJsonNode.fieldNames())) {
        if (mapKey.length() > Limits.MAPPING_NAME_MAX_LENGTH_CHARS) {
          throw new ValidationErrorException("Mapping key " + mapKey + " exceeds the maximum number of allowed characters (" + Limits.MAPPING_NAME_MAX_LENGTH_CHARS + ")");
        }
        JsonNode attributesJsonNode = JsonHelper.checkObject(mappingJsonNode, mapKey, "Every "
          + TemplateSection.Mappings + " member " + mapKey + " must be a map");
        if (Lists.newArrayList(attributesJsonNode.fieldNames()).size() > Limits.MAX_ATTRIBUTES_PER_MAPPING) {
          throw new ValidationErrorException("Mapping with key " + mapKey + " has more than " + Limits.MAX_ATTRIBUTES_PER_MAPPING + ", the max allowed.");
        }
        for (String attribute: Lists.newArrayList(attributesJsonNode.fieldNames())) {
          if (attribute.length() > Limits.MAPPING_NAME_MAX_LENGTH_CHARS) {
            throw new ValidationErrorException("Attribute " + attribute + " exceeds the maximum number of allowed characters (" + Limits.MAPPING_NAME_MAX_LENGTH_CHARS + ")");
          }
          JsonNode valueJsonNode = JsonHelper.checkStringOrArray(attributesJsonNode, attribute, "Every "
            + TemplateSection.Mappings + " attribute must be a String or a List.");
          if (!mapping.containsKey(mapName)) {
            mapping.put(mapName, Maps.<String, Map<String, String>>newLinkedHashMap());
          }
          if (!mapping.get(mapName).containsKey(mapKey)) {
            mapping.get(mapName).put(mapKey, Maps.<String, String>newLinkedHashMap());
          }
          mapping.get(mapName).get(mapKey).put(attribute, JsonHelper.getStringFromJsonNode(valueJsonNode));
        }
      }
    }
    if (mapping.keySet().size() > Limits.MAX_MAPPINGS_PER_TEMPLATE) {
      throw new ValidationErrorException("Mappings exceed maximum allowed of " + Limits.MAX_MAPPINGS_PER_TEMPLATE + " mappings per template");
    }
    template.setMapping(mapping);
  }

  static class ParameterParser {

    static void parseParameters(Template template, JsonNode templateJsonNode,
                                List<com.eucalyptus.cloudformation.common.msgs.Parameter> userParameters,
                                boolean onlyEvaluateTemplate) throws CloudFormationException {
      JsonNode parametersJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateParser.TemplateSection.Parameters.toString());
      if (parametersJsonNode != null) {
        Map<String, String> userParameterMap = Maps.newHashMap();
        if (userParameters != null) {
          for ( com.eucalyptus.cloudformation.common.msgs.Parameter userParameter : userParameters) {
            userParameterMap.put(userParameter.getParameterKey(), userParameter.getParameterValue());
          }
        }
        List<Parameter> parameterList = Lists.newArrayList();
        for (String parameterKey : Lists.newArrayList(parametersJsonNode.fieldNames())) {
          JsonNode parameterJsonNode = JsonHelper.checkObject(parametersJsonNode, parameterKey, "Any "
            + TemplateParser.TemplateSection.Parameters + " member must be a JSON object.");
          Parameter parameter = parseParameter(parameterKey, parameterJsonNode, userParameterMap);
          parameterList.add(parameter);
        }
        // construct new objects and check non-existent values
        List<String> noValueParameters = Lists.newArrayList();
        if (parameterList.size() > Limits.MAX_PARAMETERS_PER_TEMPLATE) {
          throw new ValidationErrorException("Too many parameters in the template.  Max of " + Limits.MAX_PARAMETERS_PER_TEMPLATE + " allowed.");
        }
        for (Parameter parameter : parameterList) {
          if (parameter.getParameter().getKey().length() > Limits.PARAMETER_NAME_MAX_LENGTH_CHARS) {
             throw new ValidationErrorException("Parameter " + parameter.getParameter().getKey() +
             " exceeds the maximum parameter name length of " + Limits.PARAMETER_NAME_MAX_LENGTH_CHARS + " characters.");
          }
          if (parameter.getParameter().getStringValue() != null && parameter.getParameter().getStringValue().getBytes().length > Limits.PARAMETER_VALUE_MAX_LENGTH_BYTES) {
            throw new ValidationErrorException("Parameter " + parameter.getParameter().getKey() +
              " exceeds the maximum parameter value length of " + Limits.PARAMETER_VALUE_MAX_LENGTH_BYTES + " bytes.");
          }
          template.getParameters().add(parameter.getParameter());
          template.getParameterDeclarations().add(parameter.getParameterDeclaration());
          if (parameter.getParameter().getStringValue() == null) {
            noValueParameters.add(parameter.getParameter().getKey());
          }
        }
        if (!noValueParameters.isEmpty() && !onlyEvaluateTemplate) {
          throw new ValidationErrorException("Parameters: " + noValueParameters + " must have values");
        }
        // check user supplied values actually match parameters
        Set<String> userParamKeys = Sets.newHashSet();
        Set<String> templateParamKeys = Sets.newHashSet();
        if (userParameterMap != null) {
          userParamKeys.addAll(userParameterMap.keySet());
        }
        if (parametersJsonNode != null) {
          templateParamKeys.addAll(Sets.newHashSet(parametersJsonNode.fieldNames()));
        }
        userParamKeys.removeAll(templateParamKeys);
        if (!userParamKeys.isEmpty()) {
          throw new ValidationErrorException("Parameters: " + userParamKeys + " do not exist in the template");
        }

      }
    }

    private static Parameter parseParameter(String parameterName, JsonNode parameterJsonNode, Map<String, String> userParameterMap) throws CloudFormationException {
      validateParameterKeys(parameterJsonNode);
      String actualParameterTypeStr = JsonHelper.getString(parameterJsonNode, ParameterKey.Type.toString());
      ParameterType actualParameterType = parseType(actualParameterTypeStr);
      ParameterType parsedIndividualType = null; // String or Number, or CommaDelimitedList.  Other list cases, what the individual elements are.
      boolean isList = false;
      switch (actualParameterType) {
        // intentionally grouping case statements here
        case String:
        case AWS_EC2_AvailabilityZone_Name:
        case AWS_EC2_Image_Id:
        case AWS_EC2_Instance_Id:
        case AWS_EC2_KeyPair_KeyName:
        case AWS_EC2_SecurityGroup_Id:
        case AWS_EC2_SecurityGroup_GroupName:
        case AWS_EC2_Subnet_Id:
        case AWS_EC2_Volume_Id:
        case AWS_EC2_VPC_Id:
          parsedIndividualType = ParameterType.String;
          isList = false;
          break;
        case Number:
          parsedIndividualType = ParameterType.Number;
          isList = false;
          break;
        case List_Number:
          parsedIndividualType = ParameterType.Number;
          isList = true;
          break;
        case List_String:
        case List_AWS_EC2_AvailabilityZone_Name:
        case List_AWS_EC2_Image_Id:
        case List_AWS_EC2_Instance_Id:
        case List_AWS_EC2_KeyPair_KeyName:
        case List_AWS_EC2_SecurityGroup_Id:
        case List_AWS_EC2_SecurityGroup_GroupName:
        case List_AWS_EC2_Subnet_Id:
        case List_AWS_EC2_Volume_Id:
        case List_AWS_EC2_VPC_Id:
          parsedIndividualType = ParameterType.String;
          isList = true;
          break;
        case CommaDelimitedList: // strangely enough this is grandfathered in AWS.  Many parameters which work on List<String> individually do not work here, so this is
          // treated separately for AWS compatibility
          parsedIndividualType = ParameterType.CommaDelimitedList;
          isList = false; // this SHOULD be false, I know it doesn't seem it, but it is a grandfathered case.
          break;
        default:
          throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + actualParameterType +".  Valid values are " + Arrays.toString(ParameterType.displayValues()));
      }

      String[] allowedValues = parseAllowedValues(parameterJsonNode); // type not needed
      String allowedPattern = parseAllowedPattern(parameterName, parameterJsonNode, parsedIndividualType);
      String constraintDescription = parseConstraintDescription(parameterName, parameterJsonNode); // type not needed
      String description = parseDescription(parameterName, parameterJsonNode); // type not needed

      Double maxLength = parseMaxLength(parameterName, parameterJsonNode, parsedIndividualType);
      Double minLength = parseMinLength(parameterName, parameterJsonNode, parsedIndividualType);
      if (maxLength != null && minLength != null && maxLength < minLength) {
        throw new ValidationErrorException("Template error: Parameter '" + parameterName + "' " + ParameterKey.MinLength
          + " must be less than " + ParameterKey.MaxLength + ".");
      }

      Double maxValue = parseMaxValue(parameterName, parameterJsonNode, parsedIndividualType);
      Double minValue = parseMinValue(parameterName, parameterJsonNode, parsedIndividualType);
      if (maxValue != null && minValue != null && maxValue < minValue) {
        throw new ValidationErrorException("Template error: Parameter '" + parameterName + "' " + ParameterKey.MinValue
          + " must be less than " + ParameterKey.MaxValue + ".");
      }

      List<String> valuesToCheck = Lists.newArrayList();
      String defaultValue = JsonHelper.getString(parameterJsonNode, ParameterKey.Default.toString()); // could be null
      String userDefinedValue = userParameterMap.get(parameterName); // could be null
      if (isList) {
        valuesToCheck.addAll(splitAndTrimCSVString(defaultValue));
        valuesToCheck.addAll(splitAndTrimCSVString(userDefinedValue));
      } else {
        valuesToCheck.add(defaultValue);
        valuesToCheck.add(userDefinedValue);
      }
      boolean noEcho = "true".equalsIgnoreCase(JsonHelper.getString(parameterJsonNode, ParameterKey.NoEcho.toString()));
      // now check any values that exist
      for (String value : valuesToCheck) {
        if (value != null) {
          checkAllowedValues(parameterName, value, allowedValues, constraintDescription);
        }
        switch (parsedIndividualType) {
          case String:
            if (value != null) {
              parseStringParameter(parameterName, value, allowedPattern, minLength, maxLength, constraintDescription);
            }
            break;
          case Number:
            if (value != null) {
              parseNumberParameter(parameterName, value, minValue, maxValue, constraintDescription);
            }
            break;
          case CommaDelimitedList:
            break; // currently nothing to check here
          default:
            throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + parsedIndividualType);
        }
      }

      String stringValue = null;
      if (defaultValue != null) stringValue = defaultValue;
      if (userDefinedValue != null) stringValue = userDefinedValue;
      JsonNode jsonValueNode = null;
      if (stringValue != null) {
        if (isList || actualParameterType == ParameterType.CommaDelimitedList) {
          ArrayNode arrayNode = JsonHelper.createArrayNode( );
          for (String s: splitAndTrimCSVString(stringValue)) {
            arrayNode.add(s);
          }
          jsonValueNode = arrayNode;
        } else {
          jsonValueNode = new TextNode(stringValue);
        }
      }

      StackEntity.Parameter parameter = new StackEntity.Parameter();
      parameter.setKey(parameterName);
      parameter.setNoEcho(noEcho);
      parameter.setJsonValue(JsonHelper.getStringFromJsonNode(jsonValueNode));
      parameter.setStringValue(stringValue);

      ParameterDeclaration parameterDeclaration = new ParameterDeclaration();
      parameterDeclaration.setDescription(description);
      parameterDeclaration.setDefaultValue(defaultValue);
      parameterDeclaration.setNoEcho(noEcho);
      parameterDeclaration.setParameterKey(parameterName);
      if (allowedValues!=null) {
        ParameterConstraints parameterConstraints = new ParameterConstraints();
        ResourceList allowedValuesResourceList = new ResourceList();
        allowedValuesResourceList.setMember(Lists.newArrayList(allowedValues));
        parameterConstraints.setAllowedValues(allowedValuesResourceList);
        parameterDeclaration.setParameterConstraints(parameterConstraints);
      }
      parameterDeclaration.setParameterType(actualParameterTypeStr);
      return new Parameter(parameter, parameterDeclaration);
    }

    private static Collection<String> splitAndTrimCSVString(String stringValue) {
      List<String> retVal = Lists.newArrayList();
      if (stringValue == null) {
        retVal.add(stringValue);
      } else {
        // this is a special case.  StringTokenizers will often ignore ,, cases
        StringBuilder currVal = new StringBuilder();
        char[] cArray = stringValue.toCharArray();
        for (int i = 0; i < cArray.length; i++) {
          if (cArray[i] == ',') {
            retVal.add(currVal.toString().trim());
            currVal.setLength(0); // reset
          } else {
            currVal.append(cArray[i]);
          }
        }
        retVal.add(currVal.toString().trim());
      }
      return retVal;
    }

    private static void parseNumberParameter(String parameterName, String value, Double minValue, Double maxValue, String constraintDescription) throws ValidationErrorException {
      String constraintErrorMessage = null;
      Double valueDouble = null;
      try {
        valueDouble = Double.parseDouble(value);
      } catch (NumberFormatException ex) {
        constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must be a number";
      }
      if (constraintErrorMessage == null && minValue != null && minValue > valueDouble) {
        constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must be a number not less than " + minValue;
      }
      if (constraintErrorMessage == null && maxValue != null && maxValue < valueDouble) {
        constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must be a number not greater than " + maxValue;
      }
      if (constraintErrorMessage != null && constraintDescription != null) {
        constraintErrorMessage = "Parameter '" + parameterName + "' failed to satisfy constraint: " + constraintDescription;
      }
      if (constraintErrorMessage != null) {
        throw new ValidationErrorException(constraintErrorMessage);
      }
    }

    private static void parseStringParameter(String parameterName, String value, String allowedPattern, Double minLength, Double maxLength, String constraintDescription) throws ValidationErrorException {
      String constraintErrorMessage = null;
      if (minLength != null && minLength > value.length()) {
        constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must contain at least " + minLength + " characters";
      }
      if (constraintErrorMessage == null && maxLength != null && maxLength < value.length()) {
        constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must contain at most " + maxLength + " characters";
      }
      if (constraintErrorMessage == null && allowedPattern != null) {
        try {
          if (!value.matches(allowedPattern)) {
            constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must match pattern " + allowedPattern;
          }
        } catch (PatternSyntaxException ex) {
          // not a constraint violation
          throw new ValidationErrorException("Parameter '" + parameterName + "' " + ParameterKey.AllowedPattern
            + " must be a valid regular expression.");
        }
      }
      if (constraintErrorMessage != null && constraintDescription != null) {
        constraintErrorMessage = "Parameter '" + parameterName + "' failed to satisfy constraint: " + constraintDescription;
      }
      if (constraintErrorMessage != null) {
        throw new ValidationErrorException(constraintErrorMessage);
      }
    }

    private static void checkAllowedValues(String parameterName,
                                           String value,
                                           String[] allowedValues,
                                           String constraintDescription) throws ValidationErrorException {
      if (allowedValues != null) {
        if (!Arrays.asList(allowedValues).contains(value)) {
          String constraintErrorMessage = "Template error: Parameter '" + parameterName + "' must be one of " + ParameterKey.AllowedValues;
          if (constraintDescription != null) {
            constraintErrorMessage = "Parameter '" + parameterName + "' failed to satisfy constraint: " + constraintDescription;
          }
          throw new ValidationErrorException(constraintErrorMessage);
        }
      }
    }

    private static Double parseMinValue(String parameterName, JsonNode parameterJsonNode, ParameterType type) throws CloudFormationException {
      Double minValue = JsonHelper.getDouble(parameterJsonNode, ParameterKey.MinValue.toString()); // null ok
      checkParameterType(minValue, parameterName, ParameterKey.MinValue, type, ParameterType.Number);
      return minValue;
    }

    private static Double parseMaxValue(String parameterName, JsonNode parameterJsonNode, ParameterType type) throws CloudFormationException {
      Double maxValue = JsonHelper.getDouble(parameterJsonNode, ParameterKey.MaxValue.toString()); // null ok
      checkParameterType(maxValue, parameterName, ParameterKey.MaxValue, type, ParameterType.Number);
      return maxValue;
    }

    private static Double parseMinLength(String parameterName, JsonNode parameterJsonNode, ParameterType type) throws CloudFormationException {
      Double minLength = JsonHelper.getDouble(parameterJsonNode, ParameterKey.MinLength.toString()); // null ok
      checkParameterType(minLength, parameterName, ParameterKey.MinLength, type, ParameterType.String);
      return minLength;
    }

    private static Double parseMaxLength(String parameterName, JsonNode parameterJsonNode, ParameterType type) throws CloudFormationException {
      Double maxLength = JsonHelper.getDouble(parameterJsonNode, ParameterKey.MaxLength.toString()); // null ok
      checkParameterType(maxLength, parameterName, ParameterKey.MaxLength, type, ParameterType.String);
      return maxLength;
    }

    private static String parseConstraintDescription(String parameterName, JsonNode parameterJsonNode) throws CloudFormationException {
      String constraintDescription = JsonHelper.getString(parameterJsonNode,
        ParameterKey.ConstraintDescription.toString());
      // Strangely no length constraints here
      return constraintDescription;
    }

    private static String parseDescription(String parameterName, JsonNode parameterJsonNode) throws CloudFormationException {
      String description = JsonHelper.getString(parameterJsonNode,
        ParameterKey.Description.toString());
      // Strangely no length constraints here (in practice, documentation is wrong currently)
      return description;
    }

    private static String parseAllowedPattern(String parameterName, JsonNode parameterJsonNode, ParameterType type) throws CloudFormationException {
      String allowedPattern = JsonHelper.getString(parameterJsonNode, ParameterKey.AllowedPattern.toString()); // null ok
      checkParameterType(allowedPattern, parameterName, ParameterKey.AllowedPattern, type, ParameterType.String);
      return allowedPattern;
    }

    private static void checkParameterType(Object value, String name, ParameterKey key, ParameterType valueType, ParameterType requiredType)
      throws ValidationErrorException {
      if (value != null && valueType != requiredType) {
        throw new ValidationErrorException("Template error: Parameter '" + name + "' " + key +
          " must be on a parameter of type " + requiredType);
      }
    }

    private static void validateParameterKeys(JsonNode parameterJsonNode) throws ValidationErrorException {
      Set<String> tempParameterKeys = Sets.newHashSet(parameterJsonNode.fieldNames());
      for (ParameterKey validParameterKey : ParameterKey.values()) {
        tempParameterKeys.remove(validParameterKey.toString());
      }
      if (!tempParameterKeys.isEmpty()) {
        throw new ValidationErrorException("Invalid template parameter property or properties " + tempParameterKeys);
      }
    }

    private static String[] parseAllowedValues(JsonNode parameterJsonNode) throws CloudFormationException {
      String[] allowedValues = null;
      JsonNode allowedValuesJsonNode = JsonHelper.checkArray(parameterJsonNode, ParameterKey.AllowedValues.toString());
      if (allowedValuesJsonNode != null) {
        allowedValues = new String[allowedValuesJsonNode.size()];
        for (int index = 0; index < allowedValues.length; index++) {
          String errorMsg = "Every " + ParameterKey.AllowedValues + "value must be a string.";
          String allowedValue = JsonHelper.getString(allowedValuesJsonNode, index, errorMsg);
          if (allowedValue == null) {
            throw new ValidationErrorException("Template format error: " + errorMsg);
          }
          allowedValues[index] = allowedValue;
        }
      }
      return allowedValues;
    }

    private static ParameterType parseType(JsonNode parameterJsonNode) throws CloudFormationException {
      return parseType(JsonHelper.getString(parameterJsonNode, ParameterKey.Type.toString()));

    }
    private static ParameterType parseType(String typeStr) throws CloudFormationException {
      if (typeStr == null) {
        throw new ValidationErrorException("Template format error: Every " + TemplateParser.TemplateSection.Parameters + " object "
          + "must contain a " + ParameterKey.Type + " member.");
      }
      ParameterType type = null;
      try {
        type = ParameterType.displayValueOf(typeStr);
      } catch (IllegalArgumentException ex) {
        throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + typeStr +".  Valid values are " + Arrays.toString(ParameterType.displayValues()));
      }
      return type;
    }

    private static class Parameter {
      private StackEntity.Parameter stackEntityParameter;
      private ParameterDeclaration parameterDeclaration;

      private Parameter(StackEntity.Parameter stackEntityParameter, ParameterDeclaration parameterDeclaration) {
        this.stackEntityParameter = stackEntityParameter;
        this.parameterDeclaration = parameterDeclaration;
      }

      StackEntity.Parameter getParameter() {
        return stackEntityParameter;
      }

      ParameterDeclaration getParameterDeclaration() { return parameterDeclaration; }
    }
  }

  private void parseConditions(Template template, JsonNode templateJsonNode, boolean onlyEvaluateTemplate, String effectiveUserId) throws CloudFormationException {
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
        // Don't like to have to roll/unroll condition map like this but evaluateFunctions is used post-map a lot
        Map<String, Boolean> conditionMap = template.getConditionMap();
        // just put a placeholder in if evaluating
        if (onlyEvaluateTemplate) {
          conditionMap.put(conditionName, Boolean.FALSE);
        } else {
          conditionMap.put(conditionName, FunctionEvaluation.evaluateBoolean(FunctionEvaluation.evaluateFunctions(conditionJsonNode, template, effectiveUserId)));
        }
        template.setConditionMap(conditionMap);
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
      String conditionName = currentNode.get(FunctionEvaluation.FN_IF).get(0).asText();
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
      String conditionName = currentNode.get(FunctionEvaluation.CONDITION_STR).asText();
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
      String refName = currentNode.get(FunctionEvaluation.REF_STR).asText();
      // it's ok if it's a psueodparameter or a parameter, but not a resource, or doesn't exist
      Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
      Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
      if (!pseudoParameterMap.containsKey(refName) && !parameterMap.containsKey(refName)) {
        resourceReferences.add(refName);
      }
      return;
    }

    // Check "Fn::GetAtt" (make sure not resource or attribute)
    IntrinsicFunction.MatchResult fnAttMatcher = IntrinsicFunctions.GET_ATT.evaluateMatch(currentNode);
    if (fnAttMatcher.isMatch()) {
      IntrinsicFunctions.GET_ATT.validateArgTypesWherePossible(fnAttMatcher);
      // we have a match against a "ref"...
      String refName = currentNode.get(FunctionEvaluation.FN_GET_ATT).get(0).asText();
      String attName = currentNode.get(FunctionEvaluation.FN_GET_ATT).get(1).asText();
      // Not sure why, but AWS validates attribute types even in Conditions
      if (template.getResourceInfoMap().containsKey(refName)) {
        ResourceInfo resourceInfo = template.getResourceInfoMap().get(refName);
        if (!resourceInfo.isAttributeAllowed(attName)) {
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
  private void buildResourceMap(Template template, JsonNode templateJsonNode, boolean enforceStrictResourceProperties) throws CloudFormationException {
    // This is only done before everything else because Fn::GetAtt needs resource info to determine if it is a
    // "good" fit, which is done at "compile time"...
    JsonNode resourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    if (resourcesJsonNode == null || resourcesJsonNode.size() == 0) {
      throw new ValidationErrorException("At least one " + TemplateSection.Resources + " member must be defined.");
    }
    List<String> resourceNames = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
    String accountId = JsonHelper.getJsonNodeFromString(pseudoParameterMap.get(AWS_ACCOUNT_ID)).asText();
    for (String resourceName: resourceNames) {
      JsonNode resourceJsonNode = resourcesJsonNode.get(resourceName);
      if (!(resourceJsonNode.isObject())) {
        throw new ValidationErrorException("Template format error: Any Resources member must be a JSON object.");
      }
      String type = JsonHelper.getString(resourceJsonNode, ResourceKey.Type.toString());
      if (type == null) {
        throw new ValidationErrorException("Type is a required property of Resource");
      }
      ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(type);
      if (resourceInfo == null) {
        throw new ValidationErrorException("Unknown resource type " + type);
      }
      resourceInfo.setAccountId(accountId);

      template.getResourceInfoMap().put(resourceName, resourceInfo);

      Set<String> tempResourceKeys = Sets.newHashSet(resourceJsonNode.fieldNames());
      for (ResourceKey resourceKey: ResourceKey.values()) {
        tempResourceKeys.remove(resourceKey.toString());
      }
      if (!tempResourceKeys.isEmpty() && enforceStrictResourceProperties) {
        throw new ValidationErrorException("Invalid resource property or properties " + tempResourceKeys);
      }

    }
  }

  private void parseResources(Template template, JsonNode templateJsonNode, boolean onlyValidateTemplate) throws CloudFormationException {
    Map<String, Boolean> conditionMap = template.getConditionMap();
    Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
    Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
    JsonNode resourcesJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Resources.toString());
    List<String> resourceKeys = (List<String>) Lists.newArrayList(resourcesJsonNode.fieldNames());
    // make sure no duplicates betwen parameters and resources
    Set<String> commonParametersAndResources = Sets.intersection(Sets.newHashSet(resourceKeys),
      Sets.union(parameterMap.keySet(), pseudoParameterMap.keySet()));
    if (!commonParametersAndResources.isEmpty()) {
      throw new ValidationErrorException("Template error: all resources and parameters must have unique names. " +
        "Common name(s):"+commonParametersAndResources);
    }

    DependencyManager resourceDependencies = new DependencyManager();
    if (resourceKeys.size() > Limits.MAX_RESOURCES_PER_TEMPLATE) {
      throw new ValidationErrorException("Too many resources in the template.  Max allowed is " + Limits.MAX_RESOURCES_PER_TEMPLATE + ".");
    }
    for (String resourceKey: resourceKeys) {
      if (resourceKey != null && resourceKey.length() > Limits.RESOURCE_NAME_MAX_LENGTH_CHARS) {
        throw new ValidationErrorException("Resource name " + resourceKey + " exceeds the maximum resource name length of " + Limits.RESOURCE_NAME_MAX_LENGTH_CHARS + " characters.");
      }
      if (!resourceKey.matches("^[\\p{Alnum}]*$")) {
        throw new ValidationErrorException("Resource name " + resourceKey + " must be alphanumeric.");
      }
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
            if (dependsOnJsonNode.get(i) != null && dependsOnJsonNode.get(i).isValueNode()) {
              String dependeningOnResourceName = dependsOnJsonNode.get(i).asText();
              if (!template.getResourceInfoMap().containsKey(dependeningOnResourceName)) {
                unresolvedResourceDependencies.add(dependeningOnResourceName);
              } else {
                resourceDependencies.addDependency(resourceKey, dependeningOnResourceName);
              }
            } else {
              throw new ValidationErrorException("Template format error: Every DependsOn value must be a string.");
            }
          }
        } else if (dependsOnJsonNode.isValueNode()) {
          String dependeningOnResourceName = dependsOnJsonNode.asText();
          if (!template.getResourceInfoMap().containsKey(dependeningOnResourceName)) {
            unresolvedResourceDependencies.add(dependeningOnResourceName);
          } else {
            resourceDependencies.addDependency(resourceKey, dependeningOnResourceName);
          }
        } else {
          throw new ValidationErrorException("Template format error: DependsOn must be a string or list of strings.");
        }
      }
      ResourceInfo resourceInfo = template.getResourceInfoMap().get(resourceKey);
      String description = JsonHelper.getString(resourceJsonNode, ResourceKey.Description.toString());
      if (description != null && description.length() > 4000) {
        throw new ValidationErrorException("Template format error: " + ResourceKey.Description + " must be no "
          + "longer than 4000 characters.");
      }
      resourceInfo.setDescription(description);

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
      JsonNode creationPolicyNode = JsonHelper.checkObject(resourceJsonNode, ResourceKey.CreationPolicy.toString());
      if (propertiesNode != null) {
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(propertiesNode);
        resourceInfo.setCreationPolicyJson(JsonHelper.getStringFromJsonNode(creationPolicyNode));
      }
      resourceInfo.setLogicalResourceId(resourceKey);
      resourceDependencyCrawl(resourceKey, metadataNode, resourceDependencies, template, unresolvedResourceDependencies, !onlyValidateTemplate);
      resourceDependencyCrawl(resourceKey, propertiesNode, resourceDependencies, template, unresolvedResourceDependencies, !onlyValidateTemplate);
      resourceDependencyCrawl(resourceKey, updatePolicyNode, resourceDependencies, template, unresolvedResourceDependencies, !onlyValidateTemplate);
      resourceDependencyCrawl(resourceKey, creationPolicyNode, resourceDependencies, template, unresolvedResourceDependencies, !onlyValidateTemplate);
      String deletionPolicy = JsonHelper.getString(resourceJsonNode, ResourceKey.DeletionPolicy.toString());
      if (deletionPolicy != null) {
        if (!DeletionPolicyValues.Delete.toString().equals(deletionPolicy)
          && !DeletionPolicyValues.Retain.toString().equals(deletionPolicy)
          && !DeletionPolicyValues.Snapshot.toString().equals(deletionPolicy)) {
          throw new ValidationErrorException("Template format error: Unrecognized DeletionPolicy " + deletionPolicy +
            " for resource " + resourceKey);
        }
        if (DeletionPolicyValues.Snapshot.toString().equals(deletionPolicy) && !resourceInfo.supportsSnapshot()) {
          throw new ValidationErrorException("Template error: resource type " + resourceInfo.getType() + " does not support deletion policy Snapshot");
        }
        resourceInfo.setDeletionPolicy(deletionPolicy);
      }
      String conditionKey = JsonHelper.getString(resourceJsonNode, ResourceKey.Condition.toString());
      if (conditionKey != null) {
        if (!conditionMap.containsKey(conditionKey)) {
          throw new ValidationErrorException("Template format error: Condition " + conditionKey + "  is not defined.");
        }
        resourceInfo.setAllowedByCondition((onlyValidateTemplate ? Boolean.TRUE : conditionMap.get(conditionKey)));
      } else {
        resourceInfo.setAllowedByCondition(Boolean.TRUE);
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
    template.setResourceDependencyManager(resourceDependencies);
  }

  private void resourceDependencyCrawl(String resourceKey, JsonNode jsonNode,
                                       DependencyManager resourceDependencies, Template template,
                                       Set<String> unresolvedResourceDependencies, boolean onLiveBranch)
    throws CloudFormationException {
    Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
    Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
    if (jsonNode == null) {
      return;
    }
    if (jsonNode.isArray()) {
      for (int i=0;i<jsonNode.size();i++) {
        resourceDependencyCrawl(resourceKey, jsonNode.get(i), resourceDependencies, template, unresolvedResourceDependencies, onLiveBranch);
      }
    }

    // Now we are dealing with an object, perhaps a function
    // Check "If" (only track dependencies against true branch
    IntrinsicFunction.MatchResult fnIfMatcher = IntrinsicFunctions.IF.evaluateMatch(jsonNode);
    if (fnIfMatcher.isMatch()) {
      IntrinsicFunctions.IF.validateArgTypesWherePossible(fnIfMatcher);
      // We know from validate this is an array of 3 elements
      JsonNode keyJsonNode = jsonNode.get(FunctionEvaluation.FN_IF);

      String key = keyJsonNode.get(0).asText();
      Map<String, Boolean> conditionMap = template.getConditionMap();

      if (!conditionMap.containsKey(key)) {
        throw new ValidationErrorException("Template error: unresolved condition dependency: " + key);
      };
      boolean booleanValue = template.getConditionMap().get(key);
      // AWS has weird behavior that on an Fn::If, undefined Ref values will be detected on branches that are not taken,
      // but circular dependencies won't care (as the branch won't be taken)
      resourceDependencyCrawl(resourceKey, keyJsonNode.get(1), resourceDependencies, template, unresolvedResourceDependencies, onLiveBranch && booleanValue);
      resourceDependencyCrawl(resourceKey, keyJsonNode.get(2), resourceDependencies, template, unresolvedResourceDependencies, onLiveBranch && (!booleanValue));
      return;
    }

    // Check "Ref" (only make sure not resource)
    IntrinsicFunction.MatchResult refMatcher = IntrinsicFunctions.REF.evaluateMatch(jsonNode);
    if (refMatcher.isMatch()) {
      IntrinsicFunctions.REF.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.REF_STR).asText();
      if (template.getResourceInfoMap().containsKey(refName)) {
        if (onLiveBranch) { // the onLiveBranch will add a dependency only if the condition is true
          resourceDependencies.addDependency(resourceKey, refName);
        }
      } else if (!parameterMap.containsKey(refName) &&
        !pseudoParameterMap.containsKey(refName)) {
        unresolvedResourceDependencies.add(refName);
      }
      return;
    }

    // Check "Fn::GetAtt" (make sure not resource or attribute)
    IntrinsicFunction.MatchResult fnAttMatcher = IntrinsicFunctions.GET_ATT.evaluateMatch(jsonNode);
    if (fnAttMatcher.isMatch()) {
      IntrinsicFunctions.GET_ATT.validateArgTypesWherePossible(fnAttMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(0).asText();
      String attName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(1).asText();
      // Not sure why, but AWS validates attribute types even in Conditions
      if (template.getResourceInfoMap().containsKey(refName)) {
        ResourceInfo resourceInfo = template.getResourceInfoMap().get(refName);
        if (!resourceInfo.isAttributeAllowed(attName)) {
          throw new ValidationErrorException("Template error: resource " + refName +
            " does not support attribute type " + attName + " in Fn::GetAtt");
        } else {
          if (onLiveBranch) { // the onLiveBranch will add a dependency only if the condition is true
            resourceDependencies.addDependency(resourceKey, refName);
          }
        }
      } else {
        // not a resource...
        throw new ValidationErrorException("Template error: instance of Fn::GetAtt references undefined resource "
          + refName);
      }
      return;
    }

    // Check "Fn::Sub" ... could be either resource or attribute
    IntrinsicFunction.MatchResult fnSubMatcher = IntrinsicFunctions.FN_SUB.evaluateMatch(jsonNode);
    if (fnSubMatcher.isMatch()) {
      IntrinsicFunctions.FN_SUB.validateArgTypesWherePossible(fnSubMatcher);
      // we have a match against a "sub"...
      JsonNode subNode = jsonNode.get(FunctionEvaluation.FN_SUB);
      Set<String> mappedVariableNames = Sets.newHashSet();
      String value;
      // either a value node or an array with 2 elements: value and mapping
      if (subNode.isValueNode()) {
        value = subNode.asText();
      } else {
        value = subNode.get(0).textValue();
        mappedVariableNames.addAll(Lists.newArrayList(subNode.get(1).fieldNames()));
      }
      Collection<String> variables = FnSubHelper.extractVariables(value);
      for (String variable: variables) {
        // first see if it is in the accompanying mapping... (or parameter/pseudo-parameter values)
        // TODO: fail if wrong type of parameter or pseudoparameter
        if (mappedVariableNames.contains(variable) || pseudoParameterMap.containsKey(variable) ||
          parameterMap.containsKey(variable)) {
        } else if (template.getResourceInfoMap().containsKey(variable)) { // check ref
          if (onLiveBranch) { // the onLiveBranch will add a dependency only if the condition is true
            resourceDependencies.addDependency(resourceKey, variable);
          }
        } else if (variable.contains(".")) {
          String refName = variable.substring(0, variable.indexOf("."));
          String attName = variable.substring(variable.indexOf(".") + 1);
          if (template.getResourceInfoMap().containsKey(refName)) {
            ResourceInfo resourceInfo = template.getResourceInfoMap().get(refName);
            if (!resourceInfo.isAttributeAllowed(attName)) {
              throw new ValidationErrorException("Template error: resource " + refName +
                " does not support attribute type " + attName + " in Fn::GetAtt");
            } else {
              if (onLiveBranch) { // the onLiveBranch will add a dependency only if the condition is true
                resourceDependencies.addDependency(resourceKey, refName);
              }
            }
          }
        } else {
          unresolvedResourceDependencies.add(variable);
        }
      }
      return;
    }

    // Now either just an object or a different function.  Either way, crawl in the innards
    List<String> fieldNames = Lists.newArrayList(jsonNode.fieldNames());
    for (String fieldName: fieldNames) {
      resourceDependencyCrawl(resourceKey, jsonNode.get(fieldName), resourceDependencies, template, unresolvedResourceDependencies, onLiveBranch);
    }
  }

  private void parseOutputs(Template template, JsonNode templateJsonNode) throws CloudFormationException {
    Map<String, Boolean> conditionMap = template.getConditionMap();
    ArrayList<StackEntity.Output> outputs = template.getWorkingOutputs();
    JsonNode outputsJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateSection.Outputs.toString());
    if (outputsJsonNode != null) {
      List<String> unresolvedResourceDependencies = Lists.newArrayList();
      List<String> outputKeys = Lists.newArrayList(outputsJsonNode.fieldNames());
      for (String outputKey: outputKeys) {
        if (outputKey.length() > Limits.OUTPUT_NAME_MAX_LENGTH_CHARS) {
          throw new ValidationErrorException("Output " + outputKey + " name exceeds the maximum length of " + Limits.OUTPUT_NAME_MAX_LENGTH_CHARS + " characters");
        }
        // TODO: we could create an output object, but would have to serialize it to pass to inputs anyway, so just
        // parse for now, fail on any errors, and reparse once evaluated.
        JsonNode outputJsonNode = outputsJsonNode.get(outputKey);
        validateValidResourcesInOutputs(outputKey, outputJsonNode, template, unresolvedResourceDependencies);
        Set<String> tempOutputKeys = Sets.newHashSet(outputJsonNode.fieldNames());
        for (OutputKey validOutputKey: OutputKey.values()) {
          tempOutputKeys.remove(validOutputKey.toString());
        }
        if (!tempOutputKeys.isEmpty()) {
          throw new ValidationErrorException("Invalid output property or properties " + tempOutputKeys);
        }
        String description = JsonHelper.getString(outputsJsonNode.get(outputKey), OutputKey.Description.toString());
        if (description != null && description.length() > 4000) {
          throw new ValidationErrorException("Template format error: " + OutputKey.Description + " must be no "
            + "longer than 4000 characters.");
        }
        String conditionKey = JsonHelper.getString(outputJsonNode, OutputKey.Condition.toString());
        if (conditionKey != null) {
          if (!conditionMap.containsKey(conditionKey)) {
            throw new ValidationErrorException("Template format error: Condition " + conditionKey + "  is not defined.");
          }
        }
        if (!outputJsonNode.has(OutputKey.Value.toString())) {
          throw new ValidationErrorException("Every Outputs member must contain a Value object");
        }
        FunctionEvaluation.validateNonConditionSectionArgTypesWherePossible(outputsJsonNode.get(outputKey));
        StackEntity.Output output = new StackEntity.Output();
        output.setKey(outputKey);
        JsonNode outputValueNode = outputJsonNode.get(OutputKey.Value.toString());
        boolean match = false;
        for (IntrinsicFunction intrinsicFunction: IntrinsicFunctions.values()) {
          IntrinsicFunction.MatchResult matchResult = intrinsicFunction.evaluateMatch(outputValueNode);
          if (matchResult.isMatch()) {
            match = true;
            break;
          }
        }
        if (!match) {
          if (outputValueNode.isObject()) {
            throw new ValidationErrorException("The Value field of every Outputs member must evaluate to a String and not a Map.");
          }

          if (outputValueNode.isArray()) {
            throw new ValidationErrorException("The Value field of every Outputs member must evaluate to a String and not a List.");
          }
        }

        output.setDescription(description);
        output.setJsonValue(JsonHelper.getStringFromJsonNode(outputJsonNode.get(OutputKey.Value.toString())));
        output.setReady(false);
        output.setAllowedByCondition(conditionMap.get(conditionKey) != Boolean.FALSE);
        outputs.add(output);
      }
      if (!unresolvedResourceDependencies.isEmpty()) {
        throw new ValidationErrorException("Template format error: Unresolved resource dependencies " + unresolvedResourceDependencies + " in the Outputs block of the template");
      }
      if (outputs.size() > Limits.MAX_OUTPUTS_PER_TEMPLATE) {
        throw new ValidationErrorException("Stack exceeds the maximum allowed number of outputs.("+Limits.MAX_OUTPUTS_PER_TEMPLATE+")");
      }
      template.setWorkingOutputs(outputs);
    }
  }

  private void validateValidResourcesInOutputs(String outputKey, JsonNode jsonNode, Template template, List<String> unresolvedResourceDependencies) throws CloudFormationException {
    Map<String, String> pseudoParameterMap = template.getPseudoParameterMap();
    Map<String, StackEntity.Parameter> parameterMap = template.getParameterMap();
    Map<String, ResourceInfo> resourceInfoMap = template.getResourceInfoMap();
    if (jsonNode == null) {
      return;
    }
    if (jsonNode.isArray()) {
      for (int i=0;i<jsonNode.size();i++) {
        validateValidResourcesInOutputs(outputKey, jsonNode.get(i), template, unresolvedResourceDependencies);
      }
    }

    // Now we are dealing with an object, perhaps a function
    // Check "If" (only track dependencies against true branch
    IntrinsicFunction.MatchResult fnIfMatcher = IntrinsicFunctions.IF.evaluateMatch(jsonNode);
    // Check "Ref" (only make sure not resource)
    IntrinsicFunction.MatchResult refMatcher = IntrinsicFunctions.REF.evaluateMatch(jsonNode);
    if (refMatcher.isMatch()) {
      IntrinsicFunctions.REF.validateArgTypesWherePossible(refMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.REF_STR).asText();
      if (!parameterMap.containsKey(refName) &&
        !pseudoParameterMap.containsKey(refName) && !resourceInfoMap.containsKey(refName)) {
        unresolvedResourceDependencies.add(refName);
      }
      return;
    }

    // Check "Fn::GetAtt" (make sure not resource or attribute)
    IntrinsicFunction.MatchResult fnAttMatcher = IntrinsicFunctions.GET_ATT.evaluateMatch(jsonNode);
    if (fnAttMatcher.isMatch()) {
      IntrinsicFunctions.GET_ATT.validateArgTypesWherePossible(fnAttMatcher);
      // we have a match against a "ref"...
      String refName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(0).asText();
      String attName = jsonNode.get(FunctionEvaluation.FN_GET_ATT).get(1).asText();
      // Not sure why, but AWS validates attribute types even in Conditions
      if (resourceInfoMap.containsKey(refName)) {
        ResourceInfo resourceInfo = resourceInfoMap.get(refName);
        if (!resourceInfo.isAttributeAllowed(attName)) {
          throw new ValidationErrorException("Template error: resource " + refName +
            " does not support attribute type " + attName + " in Fn::GetAtt");
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
      validateValidResourcesInOutputs(outputKey, jsonNode.get(fieldName), template, unresolvedResourceDependencies);
    }
  }

  public Map<String,ParameterType> getParameterTypeMap(String templateBody) throws CloudFormationException {
    Map<String, ParameterType> returnVal = Maps.newHashMap();
    JsonNode templateJsonNode;
    try {
      templateJsonNode = Json.parse( templateBody );
    } catch (IOException ex) {
      throw new ValidationErrorException(ex.getMessage());
    }
    if (!templateJsonNode.isObject()) {
      throw new ValidationErrorException("Template body is not a JSON object");
    }
    JsonNode parametersJsonNode = JsonHelper.checkObject(templateJsonNode, TemplateParser.TemplateSection.Parameters.toString());
    if (parametersJsonNode != null) {
      for (String parameterKey : Lists.newArrayList(parametersJsonNode.fieldNames())) {
        JsonNode parameterJsonNode = JsonHelper.checkObject(parametersJsonNode, parameterKey, "Any "
          + TemplateParser.TemplateSection.Parameters + " member must be a JSON object.");
        if (parameterJsonNode != null) {
          ParameterParser.validateParameterKeys(parameterJsonNode);
          returnVal.put(parameterKey, ParameterParser.parseType(parameterJsonNode));
        }
      }
    }
    return returnVal;
  }

}

