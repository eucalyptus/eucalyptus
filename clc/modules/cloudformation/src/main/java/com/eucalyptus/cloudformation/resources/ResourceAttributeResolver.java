/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ResourceAttributeResolver {

  private static final Logger LOG = Logger.getLogger(ResourceAttributeResolver.class);

  public static boolean resourceHasAttribute(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    return getReadMethod(resourceInfo, attributeName) != null;
  }

  public static Collection<String> getResourceAttributeNames(ResourceInfo resourceInfo) throws CloudFormationException {
    List<String> attributeNames = Lists.newArrayList();
    BeanInfo beanInfo = null;
    try {
      beanInfo = Introspector.getBeanInfo(resourceInfo.getClass());
    } catch (IntrospectionException ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
    Map<String, PropertyDescriptor> propertyDescriptorMap = Maps.newHashMap();
    for (PropertyDescriptor propertyDescriptor:beanInfo.getPropertyDescriptors()) {
      propertyDescriptorMap.put(propertyDescriptor.getName(), propertyDescriptor);
    }
    for (Field field: resourceInfo.getClass().getDeclaredFields()) {
      AttributeJson attribute = field.getAnnotation(AttributeJson.class);
      if (attribute == null) continue;
      String attributeName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
      if (attribute.name() != null && !attribute.name().isEmpty()) {
        attributeName = attribute.name();
      }
      if (propertyDescriptorMap.containsKey(field.getName())) {
        attributeNames.add(attributeName);
      }
    }
    return attributeNames;
  }

  private static Method getReadMethod(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    BeanInfo beanInfo = null;
    try {
      beanInfo = Introspector.getBeanInfo(resourceInfo.getClass());
    } catch (IntrospectionException ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
    Map<String, PropertyDescriptor> propertyDescriptorMap = Maps.newHashMap();
    for (PropertyDescriptor propertyDescriptor:beanInfo.getPropertyDescriptors()) {
      propertyDescriptorMap.put(propertyDescriptor.getName(), propertyDescriptor);
    }
    for (Field field: resourceInfo.getClass().getDeclaredFields()) {
      AttributeJson attribute = field.getAnnotation(AttributeJson.class);
      if (attribute == null) continue;
      if (attribute.name().equals(attributeName) || field.getName().equals(Introspector.decapitalize(attributeName))) {
        if (propertyDescriptorMap.containsKey(field.getName())) {
          return propertyDescriptorMap.get(field.getName()).getReadMethod();
        }
      }
    }
    return null;
  }



  private static Method getWriteMethod(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    BeanInfo beanInfo = null;
    try {
      beanInfo = Introspector.getBeanInfo(resourceInfo.getClass());
    } catch (IntrospectionException ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
    Map<String, PropertyDescriptor> propertyDescriptorMap = Maps.newHashMap();
    for (PropertyDescriptor propertyDescriptor:beanInfo.getPropertyDescriptors()) {
      propertyDescriptorMap.put(propertyDescriptor.getName(), propertyDescriptor);
    }
    for (Field field: resourceInfo.getClass().getDeclaredFields()) {
      AttributeJson attribute = field.getAnnotation(AttributeJson.class);
      if (attribute == null) continue;
      if (attribute.name().equals(attributeName) || field.getName().equals(Introspector.decapitalize(attributeName))) {
        if (propertyDescriptorMap.containsKey(field.getName())) {
          return propertyDescriptorMap.get(field.getName()).getWriteMethod();
        }
      }
    }
    return null;
  }

  public static String getResourceAttributeJson(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    try {
      Method method = getReadMethod(resourceInfo, attributeName);
      return (String) method.invoke(resourceInfo);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }

  public static void setResourceAttributeJson(ResourceInfo resourceInfo, String attributeName, String attributeValue) throws CloudFormationException {
    try {
      Method method = getWriteMethod(resourceInfo, attributeName);
      method.invoke(resourceInfo, attributeValue);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }

}
