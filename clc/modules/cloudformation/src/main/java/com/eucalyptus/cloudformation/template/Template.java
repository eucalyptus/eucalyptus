package com.eucalyptus.cloudformation.template;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by ethomas on 12/10/13.
 */
public class Template {

  private String templateFormatVersion = "";
  private String description = "";

  public Template() {
  }

  public String getTemplateFormatVersion() {
    return templateFormatVersion;
  }

  public void setTemplateFormatVersion(String templateFormatVersion) {
    this.templateFormatVersion = templateFormatVersion;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Parameter> getParameterList() {
    return parameterList;
  }

  public void addParameter(Parameter parameter) {
    parameterList.add(parameter);
  }

  public void setParameterList(List<Parameter> parameterList) {
    this.parameterList = parameterList;
  }

  private List<Parameter> parameterList = Lists.newArrayList();
  public static class Parameter {
    public Parameter() {
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


  }

  public enum ParameterType {
    String,
    Number,
    CommaDelimitedList
  }

}
