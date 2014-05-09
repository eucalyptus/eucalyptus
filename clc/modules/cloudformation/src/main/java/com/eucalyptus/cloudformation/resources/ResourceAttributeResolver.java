/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
