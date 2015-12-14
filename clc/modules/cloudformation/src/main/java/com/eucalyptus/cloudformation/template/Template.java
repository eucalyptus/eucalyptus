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

import com.eucalyptus.cloudformation.ParameterDeclaration;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;


/**
 * Created by ethomas on 12/10/13.
 */
public class Template {
  private Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
  private ArrayList<ParameterDeclaration> parameterDeclarations = Lists.newArrayList(); // used only for validate template/get template summary
  private String templateBody;


  // temporary representation of the top level metadata field (does not appear to need to be persisted as in body and not referenced otherwise for now)
  private String metadataJSON;


  // All the below are "object" forms of items in the StackEntity
  private String description;
  private Map<String, String> pseudoParameterMap = Maps.newLinkedHashMap();
  private String templateFormatVersion;
  private Map<String, Map<String, Map<String, String>>> mapping = Maps.newLinkedHashMap();
  private ArrayList<StackEntity.Parameter> parameters = Lists.newArrayList();
  private Map<String, Boolean> conditionMap = Maps.newLinkedHashMap();
  private DependencyManager resourceDependencyManager = new DependencyManager();
  private ArrayList<StackEntity.Output> workingOutputs = Lists.newArrayList();

  public Template() {
  }

  public Map<String, ResourceInfo> getResourceInfoMap() {
    return resourceInfoMap;
  }

  public void setResourceInfoMap(Map<String, ResourceInfo> resourceInfoMap) {
    this.resourceInfoMap = resourceInfoMap;
  }

  public ArrayList<ParameterDeclaration> getParameterDeclarations() {
    return parameterDeclarations;
  }

  public void setParameterDeclarations(ArrayList<ParameterDeclaration> parameterDeclarations) {
    this.parameterDeclarations = parameterDeclarations;
  }

  public String getMetadataJSON() {
    return metadataJSON;
  }

  public void setMetadataJSON(String metadataJSON) {
    this.metadataJSON = metadataJSON;
  }

  public void setTemplateBody(String templateBody) {
    this.templateBody = templateBody;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, String> getPseudoParameterMap() {
    return pseudoParameterMap;
  }

  public void setPseudoParameterMap(Map<String, String> pseudoParameterMap) {
    this.pseudoParameterMap = pseudoParameterMap;
  }

  public void setTemplateFormatVersion(String templateFormatVersion) {
    this.templateFormatVersion = templateFormatVersion;
  }

  public String getTemplateFormatVersion() {
    return templateFormatVersion;
  }

  public Map<String, Map<String, Map<String, String>>> getMapping() {
    return mapping;
  }

  public void setMapping(Map<String, Map<String, Map<String, String>>> mapping) {
    this.mapping = mapping;
  }

  public ArrayList<StackEntity.Parameter> getParameters() {
    return parameters;
  }

  public void setParameters(ArrayList<StackEntity.Parameter> parameters) {
    this.parameters = parameters;
  }

  public Map<String, Boolean> getConditionMap() {
    return conditionMap;
  }

  public void setConditionMap(Map<String, Boolean> conditionMap) {
    this.conditionMap = conditionMap;
  }

  public Map<String, StackEntity.Parameter> getParameterMap() {
    Map<String, StackEntity.Parameter> map = Maps.newLinkedHashMap();
    for (StackEntity.Parameter parameter: parameters) {
      map.put(parameter.getKey(), parameter);
    }
    return map;
  }

  public void setResourceDependencyManager(DependencyManager resourceDependencyManager) {
    this.resourceDependencyManager = resourceDependencyManager;
  }

  public DependencyManager getResourceDependencyManager() {
    return resourceDependencyManager;
  }

  public ArrayList<StackEntity.Output> getWorkingOutputs() {
    return workingOutputs;
  }

  public void setWorkingOutputs(ArrayList<StackEntity.Output> workingOutputs) {
    this.workingOutputs = workingOutputs;
  }
}
