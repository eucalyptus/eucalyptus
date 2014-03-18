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

import com.eucalyptus.cloudformation.TemplateParameter;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;


/**
 * Created by ethomas on 12/10/13.
 */
public class Template {
  private StackEntity stackEntity;
  private Map<String, ResourceInfo> resourceInfoMap = Maps.newLinkedHashMap();
  private ArrayList<TemplateParameter> templateParameters = Lists.newArrayList(); // used only for validate template

  public Template() {
  }

  public Map<String, ResourceInfo> getResourceInfoMap() {
    return resourceInfoMap;
  }

  public void setResourceInfoMap(Map<String, ResourceInfo> resourceInfoMap) {
    this.resourceInfoMap = resourceInfoMap;
  }


  public StackEntity getStackEntity() {
    return stackEntity;
  }

  public void setStackEntity(StackEntity stackEntity) {
    this.stackEntity = stackEntity;
  }

  public ArrayList<TemplateParameter> getTemplateParameters() {
    return templateParameters;
  }

  public void setTemplateParameters(ArrayList<TemplateParameter> templateParameters) {
    this.templateParameters = templateParameters;
  }
}
