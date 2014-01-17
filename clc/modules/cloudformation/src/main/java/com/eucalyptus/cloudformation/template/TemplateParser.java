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

import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.AWSEC2Instance;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

  private enum ValidParameterKey {
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

  private static final String[] validTemplateVersions = new String[] {"2010-09-09"};
  private static final String DEFAULT_TEMPLATE_VERSION = "2010-09-09";

  public Template parse(String templateBody, List<Parameter> userParameters) throws ValidationErrorException {
    Map<String, String> paramMap = Maps.newHashMap();
    try {
      Template template = new Template();
      JSON templateJSON = JSONSerializer.toJSON(templateBody);
      if (!(templateJSON instanceof JSONObject)) {
        throw new ValidationErrorException("Template body is not a JSONObject");
      }
      JSONObject templateJSONObject = (JSONObject) templateJSON;
      parseValidTopLevelKeys(templateJSONObject);
      parseVersion(template, templateJSONObject);
      parseDescription(template, templateJSONObject);
      parseParameters(userParameters, paramMap, template, templateJSONObject);
      parseResources(template, templateJSONObject);
      return template;
    } catch (JSONException ex) {
      throw new ValidationErrorException(ex.getMessage());
    }
  }

  public void reevaluateResources(Template template) {



  }

  private void parseResources(Template template, JSONObject templateJSONObject) throws ValidationErrorException {
    JSONObject resourcesJSONObject = getJSONObject(templateJSONObject,
      TemplateSection.Resources.toString(),
      "Template format error: Any Resources member must be a JSON object.");
    if (resourcesJSONObject == null || resourcesJSONObject.keySet().isEmpty()) {
      throw new ValidationErrorException("At least one Resources member must be defined.");
    }
    Set<String> resourceKeys = (Set<String>) resourcesJSONObject.keySet();
    for (String resourceKey: resourceKeys) {
       Object resourceObject = resourcesJSONObject.get(resourceKey);
       if (!(resourceObject instanceof JSONObject)) {
         throw new ValidationErrorException("Template format error: Any Resources member must be a JSON object.");
       }
     }
    // make sure no duplicates betwen parameters and resources
    Set<String> commonParametersAndResources = Sets.intersection(resourceKeys, template.getReferenceMap().keySet());
    if (!commonParametersAndResources.isEmpty()) {
      throw new ValidationErrorException("Template error: all resources and parameters must have unique names. Common name(s):"+commonParametersAndResources);
    }
    for (String resourceKey: resourceKeys) {
      Template.Reference reference = new Template.Reference();
      reference.setReady(false);
      reference.setReferenceName(resourceKey);
      reference.setReferenceValue(null);
      reference.setReferenceType(Template.ReferenceType.Resource);
      template.getReferenceMap().put(resourceKey, reference);
    }
    Table<String, String, Boolean> dependencies = HashBasedTable.create();
    // now evaluate references if possible
    for (String resourceKey: resourceKeys) {
      List<String> resourceReferences = Lists.newArrayList();
      findResourceReferencesAndEvaluateOthers(resourceReferences, template, resourcesJSONObject.getJSONObject(resourceKey));
      for (String reference: resourceReferences) {
        dependencies.put(reference, resourceKey, Boolean.TRUE);
      }
    }
    List<String> sortedResourceKeys = topologicallySortResources(resourceKeys, dependencies);
    for (String key: sortedResourceKeys) {
      JSONObject resourceJSONObject = resourcesJSONObject.getJSONObject(key);
      String type = getString(resourceJSONObject, "Type");
      if (type == null) {
        throw new ValidationErrorException("Type is a required property of Resource");
      }
      if ("AWS::EC2::Instance".equals(type)) {
        AWSEC2Instance instance = new AWSEC2Instance();
        instance.setType(type);
        instance.setLogicalResourceId(key);
        if (resourceJSONObject.containsKey("Metadata") && resourceJSONObject.get("Metadata") instanceof JSONObject) {
          // TODO: type check metadata, not just ignore it...
          instance.setMetadataJSON(resourceJSONObject.getJSONObject("Metadata"));
        }
        if (resourceJSONObject.containsKey("Properties") && resourceJSONObject.get("Properties") instanceof JSONObject) {
          // TODO: type check metadata, not just ignore it...
          instance.setPropertiesJSON(resourceJSONObject.getJSONObject("Properties"));
        }
        template.getResourceList().add(instance);
      } else {
        throw new ValidationErrorException("Unsupported resource type " + type);
      }
    }
  }

  private void findResourceReferencesAndEvaluateOthers(List<String> resourceReferences, Template template, JSONArray parentArray) throws ValidationErrorException {
    for (int i=0;i<parentArray.size();i++) {
      Object jsonArrayIndexObj = parentArray.get(i);
      if (jsonArrayIndexObj instanceof JSONArray) {
        findResourceReferencesAndEvaluateOthers(resourceReferences, template, (JSONArray) jsonArrayIndexObj);
      } else if (jsonArrayIndexObj instanceof JSONObject) {
        JSONObject jsonObject = (JSONObject) jsonArrayIndexObj;
        if (jsonObject.keySet().size() == 1 && jsonObject.containsKey("Ref")) {
          Object refKeyObj = jsonObject.get("Ref");
          if (!(refKeyObj instanceof String)) {
            throw new ValidationErrorException("All References must be of type string");
          }
          String refKey = (String) refKeyObj;
          if (!template.getReferenceMap().containsKey(refKey)) {
            throw new ValidationErrorException("Undefined reference " + refKey);
          }
          Template.Reference reference = template.getReferenceMap().get(refKey);
          if (reference.getReferenceType() == Template.ReferenceType.Resource) {
            resourceReferences.add(refKey);
          } else {
            parentArray.set(i, reference.getReferenceValue());
          }
        } else {
          findResourceReferencesAndEvaluateOthers(resourceReferences, template, jsonObject);
        }
      } else {
        // other types don't evaluate
      }
    }
  }

  private void findResourceReferencesAndEvaluateOthers(List<String> resourceReferences, Template template, JSONObject parentObject) throws ValidationErrorException {
    for (Map.Entry entry: (Set<Map.Entry>) parentObject.entrySet()) {
      Object entryValue = entry.getValue();
      if (entryValue instanceof JSONObject) {
        JSONObject jsonObject = (JSONObject) entryValue;
        if (jsonObject.keySet().size() == 1 && jsonObject.containsKey("Ref")) {
          Object refKeyObj = jsonObject.get("Ref");
          System.out.println(refKeyObj);
          System.out.println(refKeyObj.getClass());
          if (!(refKeyObj instanceof String)) {
            throw new ValidationErrorException("All References must be of type string");
          }
          String refKey = (String) refKeyObj;
          if (!template.getReferenceMap().containsKey(refKey)) {
            throw new ValidationErrorException("Undefined reference " + refKey);
          }
          Template.Reference reference = template.getReferenceMap().get(refKey);
          if (reference.getReferenceType() == Template.ReferenceType.Resource) {
            resourceReferences.add(refKey);
          } else {
            entry.setValue(reference.getReferenceValue());
          }
        } else {
          findResourceReferencesAndEvaluateOthers(resourceReferences, template, jsonObject);
        }
      } else if (entryValue instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray) entryValue;
        findResourceReferencesAndEvaluateOthers(resourceReferences, template, jsonArray);
      } else {
        ; // other types don't evaluate
      }
   }
  }

  private void reevaluateResourceReferences(List<String> resourceReferences, Template template, JSONArray parentArray) throws ValidationErrorException {
    for (int i=0;i<parentArray.size();i++) {
      Object jsonArrayIndexObj = parentArray.get(i);
      if (jsonArrayIndexObj instanceof JSONArray) {
        findResourceReferencesAndEvaluateOthers(resourceReferences, template, (JSONArray) jsonArrayIndexObj);
      } else if (jsonArrayIndexObj instanceof JSONObject) {
        JSONObject jsonObject = (JSONObject) jsonArrayIndexObj;
        if (jsonObject.keySet().size() == 1 && jsonObject.containsKey("Ref")) {
          Object refKeyObj = jsonObject.get("Ref");
          if (!(refKeyObj instanceof String)) {
            throw new ValidationErrorException("All References must be of type string");
          }
          String refKey = (String) refKeyObj;
          if (!template.getReferenceMap().containsKey(refKey)) {
            throw new ValidationErrorException("Undefined reference " + refKey);
          }
          Template.Reference reference = template.getReferenceMap().get(refKey);
          if (reference.isReady()) {
            parentArray.set(i, reference.getReferenceValue());
          }
        } else {
          reevaluateResourceReferences(resourceReferences, template, jsonObject);
        }
      } else {
        // other types don't evaluate
      }
    }
  }

  private void reevaluateResourceReferences(List<String> resourceReferences, Template template, JSONObject parentObject) throws ValidationErrorException {
    for (Map.Entry entry: (Set<Map.Entry>) parentObject.entrySet()) {
      Object entryValue = entry.getValue();
      if (entryValue instanceof JSONObject) {
        JSONObject jsonObject = (JSONObject) entryValue;
        if (jsonObject.keySet().size() == 1 && jsonObject.containsKey("Ref")) {
          Object refKeyObj = jsonObject.get("Ref");
          System.out.println(refKeyObj);
          System.out.println(refKeyObj.getClass());
          if (!(refKeyObj instanceof String)) {
            throw new ValidationErrorException("All References must be of type string");
          }
          String refKey = (String) refKeyObj;
          if (!template.getReferenceMap().containsKey(refKey)) {
            throw new ValidationErrorException("Undefined reference " + refKey);
          }
          Template.Reference reference = template.getReferenceMap().get(refKey);
          if (reference.isReady()) {
            entry.setValue(reference.getReferenceValue());
          }
        } else {
          reevaluateResourceReferences(resourceReferences, template, jsonObject);
        }
      } else if (entryValue instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray) entryValue;
        findResourceReferencesAndEvaluateOthers(resourceReferences, template, jsonArray);
      } else {
        ; // other types don't evaluate
      }
    }
  }



  private void parseParameters(List<Parameter> userParameters, Map<String, String> paramMap, Template template, JSONObject templateJSONObject) throws ValidationErrorException {
    if (userParameters != null) {
      for (Parameter userParameter: userParameters) {
        paramMap.put(userParameter.getParameterKey(), userParameter.getParameterValue());
      }
    }
    JSONObject parametersJSONObject = getJSONObject(templateJSONObject,
      TemplateSection.Parameters.toString(),
      "Template format error: Any Parameters member must be a JSON object.");
    if (parametersJSONObject != null) {
      Set<String> parameterKeys = (Set<String>) parametersJSONObject.keySet();
      Set<String> noValueParameters = Sets.newHashSet();
      for (String parameterKey: parameterKeys) {
        Object parameterObject = parametersJSONObject.get(parameterKey);
        if (!(parameterObject instanceof JSONObject)) {
          throw new ValidationErrorException("Template format error: Any Parameters member must be a JSON object.");
        }
        JSONObject parameterJSONObject = (JSONObject) parameterObject;
        Set<String> tempParameterKeys = Sets.newHashSet((Set<String>) parameterJSONObject.keySet());
        for (ValidParameterKey section: ValidParameterKey.values()) {
          tempParameterKeys.remove(section.toString());
        }
        if (!tempParameterKeys.isEmpty()) {
          throw new ValidationErrorException("Invalid template parameter property or properties " + tempParameterKeys.toString());
        }

        Template.Parameter parameter = new Template.Parameter();

        String typeStr = getString(parameterJSONObject, ValidParameterKey.Type.toString());
        if (typeStr == null) {
          throw new ValidationErrorException("Template format error: Every Parameters object must contain a Type member.");
        }
        Template.ParameterType parameterType = null;
        try {
          parameterType = Template.ParameterType.valueOf(typeStr);
        } catch (Exception ex) {
          throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + typeStr);
        }

        JSONArray allowedValuesJSONArray = getJSONArray(parameterJSONObject, ValidParameterKey.AllowedValues.toString());
        if (allowedValuesJSONArray != null) {
          String[] allowedValues = new String[allowedValuesJSONArray.size()];
          for (int index=0;index<allowedValues.length; index++) {
            Object allowedValueObject = allowedValuesJSONArray.get(index);
            if (allowedValueObject == null || !(allowedValueObject instanceof String)) {
              throw new ValidationErrorException("Template format error: Every AllowedValues value must be a string.");
            }
            allowedValues[index] = (String) allowedValueObject;
          }
          parameter.setAllowedValues(allowedValues);
        }
        parameter.setAllowedPattern(getString(parameterJSONObject, ValidParameterKey.AllowedPattern.toString()));
        String constraintDescription = getString(parameterJSONObject, ValidParameterKey.ConstraintDescription.toString());
        if (constraintDescription != null && constraintDescription.length() > 4000) {
          throw new ValidationErrorException("Template format error: ConstraintDescription must be no longer than 4000 characters.");
        }
        parameter.setConstraintDescription(constraintDescription);
        parameter.setDefaultValue(getString(parameterJSONObject, ValidParameterKey.Default.toString()));
        String description = getString(parameterJSONObject, ValidParameterKey.Description.toString());
        if (description != null && description.length() > 4000) {
          throw new ValidationErrorException("Template format error: Description must be no longer than 4000 characters.");
        }
        parameter.setDescription(description);
        parameter.setMaxLength(getDouble(parameterJSONObject, ValidParameterKey.MaxLength.toString()));
        parameter.setMinLength(getDouble(parameterJSONObject, ValidParameterKey.MinLength.toString()));
        parameter.setMaxValue(getDouble(parameterJSONObject, ValidParameterKey.MaxValue.toString()));
        parameter.setMinValue(getDouble(parameterJSONObject, ValidParameterKey.MinValue.toString()));
        parameter.setDefaultValue(getString(parameterJSONObject, ValidParameterKey.Default.toString()));
        parameter.setNoEcho("true".equalsIgnoreCase(getString(parameterJSONObject, ValidParameterKey.NoEcho.toString())));
        parameter.setParameterKey(parameterKey);
        parameter.setType(parameterType);
        parameter.setParameterValue(paramMap.get(parameterKey) != null ? paramMap.get(parameterKey) : parameter.getDefaultValue());
        if (parameter.getParameterValue() == null) {
          noValueParameters.add(parameterKey);
          continue;
        }
        if (parameter.getAllowedValues() != null
          && !Arrays.asList(parameter.getAllowedValues()).contains(parameter.getParameterValue())) {
          throw new ValidationErrorException(
            parameter.getConstraintDescription() != null ?
              parameter.getConstraintDescription() :
              "Template error: Parameter '" + parameterKey + "' must be one of AllowedValues"
          );
        }
        switch(parameterType) {
          case Number:
            parseNumberParameter(parameterKey, parameter);
            break;
          case String:
            parseStringParameter(parameterKey, parameter);
            break;
          case CommaDelimitedList:
            parseCommaDelimitedListParameter(parameterKey, parameter);
            break;
          default:
            throw new ValidationErrorException("Template format error: Unrecognized parameter type: " + typeStr);
        }
        template.addParameter(parameter);
        if (!noValueParameters.isEmpty()) {
          throw new ValidationErrorException("Parameters: " + noValueParameters + " must have values");
        }
        Template.Reference reference = new Template.Reference();
        reference.setReady(true);
        reference.setReferenceName(parameter.getParameterKey());
        reference.setReferenceValue(parameter.getParameterValue());
        reference.setReferenceType(Template.ReferenceType.Parameter);
        template.getReferenceMap().put(parameter.getParameterKey(), reference);
      }
    }
    // one last sanity check
    Set<String> userParamKeys = Sets.newHashSet();
    Set<String> templateParamKeys = Sets.newHashSet();
    if (userParameters != null) {
      userParamKeys.addAll(paramMap.keySet());
    }
    if (parametersJSONObject != null) {
      templateParamKeys.addAll((Set<String>)parametersJSONObject.keySet());
    }
    userParamKeys.removeAll(templateParamKeys);
    if (!userParamKeys.isEmpty()) {
      throw new ValidationErrorException("Parameters: " + userParamKeys + " do not exist in the template");
    }
  }

  private void parseCommaDelimitedListParameter(String parameterKey, Template.Parameter parameter) throws ValidationErrorException {
    if (parameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinLength must be on a parameter of type String");
    }
    if (parameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MaxLength must be on a parameter of type String");
    }
    if (parameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' AllowedPattern must be on a parameter of type String");
    }
    if (parameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinValue must be on a parameter of type Number");
    }
    if (parameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MaxValue must be on a parameter of type Number");
    }
    // TODO: do something with it?
  }

  private void parseStringParameter(String parameterKey, Template.Parameter parameter) throws ValidationErrorException {
    // boot out the unallowed parameters
    if (parameter.getMinValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinValue must be on a parameter of type Number");
    }
    if (parameter.getMaxValue() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MaxValue must be on a parameter of type Number");
    }
    if (parameter.getMaxLength() != null && parameter.getMinLength() != null && parameter.getMaxLength() < parameter.getMinLength()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinLength must be less than MaxLength.");
    }
    if (parameter.getMinLength() != null && parameter.getMinLength() > parameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at least " + parameter.getMinLength() + " characters"
      );
    }
    if (parameter.getMaxLength() != null && parameter.getMaxLength() < parameter.getParameterValue().length()) {
      throw new ValidationErrorException(
        parameter.getConstraintDescription() != null ?
          parameter.getConstraintDescription() :
          "Template error: Parameter '" + parameterKey + "' must contain at most " + parameter.getMaxLength() + " characters"
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
        throw new ValidationErrorException("Parameter '"+parameterKey+"' AllowedPattern must be a valid regular expression.");
      }
    }
  }

  private void parseNumberParameter(String parameterKey, Template.Parameter parameter) throws ValidationErrorException {
    // boot out the unallowed parameters
    if (parameter.getMinLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinLength must be on a parameter of type String");
    }
    if (parameter.getMaxLength() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MaxLength must be on a parameter of type String");
    }
    if (parameter.getAllowedPattern() != null) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' AllowedPattern must be on a parameter of type String");
    }
    if (parameter.getMaxValue() != null && parameter.getMinValue() != null && parameter.getMaxValue() < parameter.getMinValue()) {
      throw new ValidationErrorException("Template error: Parameter '" + parameterKey + "' MinValue must be less than MaxValue.");
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
          "Template error: Parameter '" + parameterKey + "' must be a number not greater than " + parameter.getMaxValue()
      );
    }
  }

  private JSONArray getJSONArray(JSONObject jsonParentObject, String key) throws ValidationErrorException {
    return getJSONArray(jsonParentObject, key, "Template format error: Every " + key + " member must be a list.");
  }

  private JSONArray getJSONArray(JSONObject jsonParentObject, String key, String errorMessage) throws ValidationErrorException {
    if (!jsonParentObject.containsKey(key)) {
      return null;
    }
    if (jsonParentObject.get(key) instanceof JSONArray) {
      return (JSONArray) jsonParentObject.get(key);
    }
    throw new ValidationErrorException(errorMessage);
  }

  private JSONObject getJSONObject(JSONObject jsonParentObject, String key) throws ValidationErrorException {
    return getJSONObject(jsonParentObject, key, "Template format error: Every " + key + " member must be a JSON Object.");
  }

  private JSONObject getJSONObject(JSONObject jsonParentObject, String key, String errorMessage) throws ValidationErrorException {
    if (!jsonParentObject.containsKey(key)) {
      return null;
    }
    if (jsonParentObject.get(key) instanceof JSONObject) {
      return (JSONObject) jsonParentObject.get(key);
    }
    throw new ValidationErrorException(errorMessage);
  }

  private Double getDouble(JSONObject jsonParentObject, String key) throws ValidationErrorException {
    return getDouble(jsonParentObject, key, "Template format error: Every " + key + " member must be a number.");
  }
  private Double getDouble(JSONObject jsonParentObject, String key, String errorMesssage) throws ValidationErrorException {
    if (!jsonParentObject.containsKey(key)) {
      return null;
    }
    if (jsonParentObject.get(key) instanceof String) {
      try {
        return Double.valueOf((String) jsonParentObject.get(key));
      } catch (NumberFormatException ex) {
        throw new ValidationErrorException(errorMesssage);
      }
    }
    throw new ValidationErrorException(errorMesssage);
  }

  private String getString(JSONObject jsonParentObject, String key) throws ValidationErrorException {
    return getString(jsonParentObject, key, "Template format error: Every " + key + " member must be a string.");
  }
  private String getString(JSONObject jsonParentObject, String key, String errorIfNotString) throws ValidationErrorException {
    if (!jsonParentObject.containsKey(key)) {
      return null;
    }
    if (jsonParentObject.get(key) instanceof String) {
      return (String) jsonParentObject.get(key);
    }
    throw new ValidationErrorException(errorIfNotString);
  }


  private void parseDescription(Template template, JSONObject templateJSONObject) throws ValidationErrorException {
    String description = getString(templateJSONObject, TemplateSection.Description.toString());
    if (description != null) {
      if (description.length() > 4000) {
        throw new ValidationErrorException("Template format error: Description must be no longer than 4000 characters.");
      }
    }
    template.setDescription(description);

  }

  private void parseValidTopLevelKeys(JSONObject templateJSONObject) throws ValidationErrorException {
    Set<String> tempTopLevelKeys = Sets.newHashSet((Set<String>) templateJSONObject.keySet());
    for (TemplateSection section: TemplateSection.values()) {
      tempTopLevelKeys.remove(section.toString());
    }
    if (!tempTopLevelKeys.isEmpty()) {
      throw new ValidationErrorException("Invalid template property or properties " + tempTopLevelKeys.toString());
    }
  }

  private void parseVersion(Template template, JSONObject templateJSONObject) throws ValidationErrorException {
    String templateFormatVersion = getString(templateJSONObject,
      TemplateSection.AWSTemplateFormatVersion.toString(),
      "Template format error: unsupported value for AWSTemplateFormatVersion.  No such version.");
    if (templateFormatVersion != null) {
      if (!Arrays.asList(validTemplateVersions).contains(templateFormatVersion)) {
        throw new ValidationErrorException("Template format error: unsupported value for AWSTemplateFormatVersion.");
      }
      template.setTemplateFormatVersion(templateFormatVersion);
    } else {
      template.setTemplateFormatVersion(DEFAULT_TEMPLATE_VERSION);
    }
  }


  static String readTemplateFromFile(File f) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    BufferedReader in = new BufferedReader(new FileReader(f));
    try {
      String line = null;
      while ((line = in.readLine()) != null) {
        stringBuilder.append(line + "\n");
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }
    return stringBuilder.toString();
  }

  public static List<String> topologicallySortResources(Collection<String> nodes, Table<String, String, Boolean> edgeTable) throws ValidationErrorException {
    // construct a copy just not to erase the graph
    Table<String, String, Boolean> clonedEdgeTable = HashBasedTable.create(edgeTable);
    List<String> sortedElements = Lists.newArrayList();
    Set<String> nodesWithNoIncomingEdges = Sets.newHashSet();
    for (String node: nodes) {
      if (clonedEdgeTable.column(node).isEmpty()) { // no edges TO this node
        nodesWithNoIncomingEdges.add(node);
      }
    }
    while (!nodesWithNoIncomingEdges.isEmpty()) {
      String internalNode = nodesWithNoIncomingEdges.iterator().next();
      nodesWithNoIncomingEdges.remove(internalNode);
      sortedElements.add(internalNode);
      // find all "next" edges...
      Set<String> destinationsFromInternalNode = Sets.newHashSet(clonedEdgeTable.row(internalNode).keySet());
      for (String destinationNode: destinationsFromInternalNode) {
        clonedEdgeTable.remove(internalNode, destinationNode);
        if (clonedEdgeTable.column(destinationNode).isEmpty()) {
          nodesWithNoIncomingEdges.add(destinationNode);
        }
      }
    }
    if (clonedEdgeTable.isEmpty()) {
      return sortedElements;
    } else {
      throw new ValidationErrorException("One or more cyclic dependencies exist in Resource References");
    }
  }
}
