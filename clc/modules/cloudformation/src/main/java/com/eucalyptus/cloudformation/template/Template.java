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

import com.eucalyptus.cloudformation.resources.Resource;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 12/10/13.
 */
public class Template {

  private String templateFormatVersion = "";
  private String description = "";

  public Template() {
  }
  public enum ReferenceType {
    Parameter,
    PseudoParameter,
    Resource
  }
  public static class Reference {
    public String referenceName;
    public Object referenceValue;
    public ReferenceType referenceType;
    public boolean isReady;

    public Reference() {
    }

    public String getReferenceName() {
      return referenceName;
    }

    public void setReferenceName(String referenceName) {
      this.referenceName = referenceName;
    }

    public Object getReferenceValue() {
      return referenceValue;
    }

    public void setReferenceValue(Object referenceValue) {
      this.referenceValue = referenceValue;
    }

    public ReferenceType getReferenceType() {
      return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
      this.referenceType = referenceType;
    }

    public boolean isReady() {
      return isReady;
    }

    public void setReady(boolean isReady) {
      this.isReady = isReady;
    }
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

  public List<Resource> resourceList = Lists.newArrayList();

  public List<Resource> getResourceList() {
    return resourceList;
  }

  public void setResourceList(List<Resource> resourceList) {
    this.resourceList = resourceList;
  }

  public Map<String, Reference> referenceMap = Maps.newHashMap();

  public Map<String, Reference> getReferenceMap() {
    return referenceMap;
  }

  public void setReferenceMap(Map<String, Reference> referenceMap) {
    this.referenceMap = referenceMap;
  }

  public enum ParameterType {
    String,
    Number,
    CommaDelimitedList
  }

}
