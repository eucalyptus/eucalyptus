package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.json.*;

import java.io.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

  public static void main(String[] args) throws Exception {
    List<Parameter> parameters = Lists.newArrayList();
    Parameter parameter = new Parameter();
    parameter.setParameterKey("DBPort2");
    parameter.setParameterValue("99999");
    parameters.add(parameter);
    new TemplateParser().parse(readTemplateFromFile(new File("/home/ethomas/Downloads/cf.json")), parameters);
  }

  public Template parse(String templateBody, List<com.eucalyptus.cloudformation.Parameter> userParameters) throws ValidationErrorException {
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
      return template;
    } catch (JSONException ex) {
      throw new ValidationErrorException(ex.getMessage());
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
        if (description != null && constraintDescription.length() > 4000) {
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
}
