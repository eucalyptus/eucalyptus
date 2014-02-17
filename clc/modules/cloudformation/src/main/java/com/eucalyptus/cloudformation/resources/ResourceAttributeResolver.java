package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by ethomas on 2/4/14.
 */
public class ResourceAttributeResolver {

  private static final Logger LOG = Logger.getLogger(ResourceAttributeResolver.class);

  public static boolean resourceHasAttribute(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    return getReadMethod(resourceInfo, attributeName) != null;
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

  public static String getResourceAttributeJson(ResourceInfo resourceInfo, String attributeName) throws CloudFormationException {
    try {
      Method method = getReadMethod(resourceInfo, attributeName);
      return (String) method.invoke(resourceInfo);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }
}
