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
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 12/10/13.
 */
public class Template {

  public DependencyManager getResourceDependencyManager() {
    return resourceDependencyManager;
  }

  public void setResourceDependencyManager(DependencyManager resourceDependencyManager) {
    this.resourceDependencyManager = resourceDependencyManager;
  }

  private DependencyManager resourceDependencyManager = new DependencyManager();
  private String templateFormatVersion = "";
  private String description = "";
  private Map<String, Map<String, Map<String, String>>> mapping = Maps.newHashMap();
  private Map<String, List<String>> availabilityZoneMap = Maps.newHashMap();
  private Map<String, String> outputJsonMap = Maps.newLinkedHashMap();

  public Map<String, String> getOutputJsonMap() {
    return outputJsonMap;
  }

  public void setOutputJsonMap(Map<String, String> outputJsonMap) {
    this.outputJsonMap = outputJsonMap;
  }

  public Map<String, List<String>> getAvailabilityZoneMap() {
    return availabilityZoneMap;
  }

  public void setAvailabilityZoneMap(Map<String, List<String>> availabilityZoneMap) {
    this.availabilityZoneMap = availabilityZoneMap;
  }

  public Template() {
  }
  public enum ReferenceType {
    Parameter,
    PseudoParameter,
    Resource
  }

  public Map<String, Map<String, Map<String, String>>> getMapping() {
    return mapping;
  }

  public void setMapping(Map<String, Map<String, Map<String, String>>> mapping) {
    this.mapping = mapping;
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

  public Map<String, ResourceInfo> getResourceMap() {
    return resourceMap;
  }

  public void setResourceMap(Map<String, ResourceInfo> resourceMap) {
    this.resourceMap = resourceMap;
  }

  public Map<String, ResourceInfo> resourceMap = Maps.newHashMap();


  public Map<String, Reference> referenceMap = Maps.newHashMap();

  public Map<String, Reference> getReferenceMap() {
    return referenceMap;
  }

  public void setReferenceMap(Map<String, Reference> referenceMap) {
    this.referenceMap = referenceMap;
  }

  public Map<String, Boolean> conditionMap = Maps.newHashMap();

  public Map<String, Boolean> getConditionMap() {
    return conditionMap;
  }

  public void setConditionMap(Map<String, Boolean> conditionMap) {
    this.conditionMap = conditionMap;
  }

  public static class Reference {
    String referenceName;
    String referenceValueJson;
    ReferenceType referenceType;
    boolean isReady;

    public Reference() {
    }

    public String getReferenceName() {
      return referenceName;
    }

    public void setReferenceName(String referenceName) {
      this.referenceName = referenceName;
    }

    public String getReferenceValueJson() {
      return referenceValueJson;
    }

    public void setReferenceValueJson(String referenceValueJson) {
      this.referenceValueJson = referenceValueJson;
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

    @Override
    public String toString() {
      return "Reference{" +
        "referenceName='" + referenceName + '\'' +
        ", referenceValueJson='" + referenceValueJson + '\'' +
        ", referenceType=" + referenceType +
        ", isReady=" + isReady +
        '}';
    }
  }
}
