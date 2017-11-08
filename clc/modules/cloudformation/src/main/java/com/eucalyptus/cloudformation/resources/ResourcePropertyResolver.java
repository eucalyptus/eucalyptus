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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ResourcePropertyResolver {
  private static final Logger LOG = Logger.getLogger(ResourcePropertyResolver.class);
  public static JsonNode getJsonNodeFromResourceProperties(ResourceProperties resourceProperties) throws CloudFormationException {
    return getJsonNodeFromObject(resourceProperties);
  }

  private static JsonNode getJsonNodeFromObject(Object object) throws CloudFormationException {
    if (object == null) return null;
    ObjectNode jsonNode = JsonHelper.createObjectNode();
    BeanInfo beanInfo = null;
    try {
      beanInfo = Introspector.getBeanInfo(object.getClass());
    } catch (IntrospectionException ex) {
      LOG.error("Unable to create bean info for class " + object.getClass().getCanonicalName() + ".  Check signatures for getters and setters");
      throw new InternalFailureException(ex.getMessage());
    }
    Map<String, PropertyDescriptor> propertyDescriptorMap = Maps.newHashMap();
    for (PropertyDescriptor propertyDescriptor:beanInfo.getPropertyDescriptors()) {
      propertyDescriptorMap.put(propertyDescriptor.getName(), propertyDescriptor);
    }

    for (Field field: object.getClass().getDeclaredFields()) {
      Property property = field.getAnnotation(Property.class);
      if (property == null) continue;
      String defaultName = field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);
      String name = (property.name() == null || property.name().isEmpty() ? defaultName: property.name());
      Object objectValue = getField(propertyDescriptorMap, field, object);
      if (objectValue == null) {
        continue;
      } else if (objectValue instanceof String) {
        jsonNode.put(name, (String) objectValue);
      } else if (objectValue instanceof Integer) {
        jsonNode.put(name, String.valueOf((Integer) objectValue));
      } else if (objectValue instanceof Long) {
        jsonNode.put(name, String.valueOf((Long) objectValue));
      } else if (objectValue instanceof Float) {
        jsonNode.put(name, String.valueOf((Float) objectValue));
      } else if (objectValue instanceof Double) {
        jsonNode.put(name, String.valueOf((Double) objectValue));
      } else if (objectValue instanceof Boolean) {
        jsonNode.put(name, String.valueOf((Boolean) objectValue));
      } else if (objectValue instanceof JsonNode) {
        jsonNode.put(name, (JsonNode) objectValue);
      } else if (objectValue instanceof Collection) {
        jsonNode.put(name, getJsonNodeFromCollection((Collection<?>) objectValue));
      } else {
        jsonNode.put(name, getJsonNodeFromObject(objectValue));
      }
    }
    return jsonNode;
  }

  private static JsonNode getJsonNodeFromCollection(Collection<?> collection) throws CloudFormationException {
    if (collection == null) return null;
    ArrayNode jsonNode = JsonHelper.createArrayNode();
    for (Object object: collection) {
      if (object == null) {
        jsonNode.add((JsonNode) null); // TODO: really?
      } else if (object instanceof String) {
        jsonNode.add((String) object);
      } else if (object instanceof Integer) {
        jsonNode.add(String.valueOf((Integer) object));
      } else if (object instanceof Long) {
        jsonNode.add(String.valueOf((Long) object));
      } else if (object instanceof Float) {
        jsonNode.add(String.valueOf((Float) object));
      } else if (object instanceof Double) {
        jsonNode.add(String.valueOf((Double) object));
      } else if (object instanceof Boolean) {
        jsonNode.add(String.valueOf((Boolean) object));
      } else if (object instanceof JsonNode) {
        jsonNode.add((JsonNode) object);
      } else if (object instanceof Collection) {
        jsonNode.add(getJsonNodeFromCollection((Collection) object));
      } else {
        jsonNode.add(getJsonNodeFromObject(object));
      }
    }
    return jsonNode;
  }


  public static void populateResourceProperties(Object object, JsonNode jsonNode, boolean enforceStrictProperties) throws CloudFormationException {
    if (jsonNode == null) return; // TODO: consider this case
    BeanInfo beanInfo = null;
    try {
      beanInfo = Introspector.getBeanInfo(object.getClass());
    } catch (IntrospectionException ex) {
      LOG.error("Unable to create bean info for class " + object.getClass().getCanonicalName() + ".  Check signatures for getters and setters");
      throw new InternalFailureException(ex.getMessage());
    }
    Map<String, PropertyDescriptor> propertyDescriptorMap = Maps.newHashMap();
    for (PropertyDescriptor propertyDescriptor:beanInfo.getPropertyDescriptors()) {
      propertyDescriptorMap.put(propertyDescriptor.getName(), propertyDescriptor);
    }
    Set<String> unprocessedFieldNames = Sets.newHashSet(jsonNode.fieldNames());
    for (Field field: object.getClass().getDeclaredFields()) {
      Property property = field.getAnnotation(Property.class);
      if (property == null) continue;
      String defaultName = field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);
      String name = (property.name() == null || property.name().isEmpty() ? defaultName: property.name());
      Required required = field.getAnnotation(Required.class);
      if (required != null && !jsonNode.has(name)) {
        throw new ValidationErrorException("Template error: " + name + " is a required field");
      }
      if (!jsonNode.has(name)) continue; // no value to populate...
      JsonNode valueNode = jsonNode.get(name);
      // once here, trying to set a field, remove from propertyFields
      unprocessedFieldNames.remove(name);
      LOG.debug("Populating property with: " + name + "=" + valueNode + " " + valueNode.getClass());
      if (field.getType().equals(String.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type String");
        } else {
          setField(propertyDescriptorMap, field, object, valueNode.asText());
        }
      } else if (field.getType().equals(Integer.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            if (valueNode.asText().isEmpty()) {
              if (required != null) {
                throw new ValidationErrorException("Template error: " + name + " can not be blank (" + valueNode.asText() + ")");
              } else {
                setField(propertyDescriptorMap, field, object, null);
              }
            } else {
                setField(propertyDescriptorMap, field, object, Integer.valueOf(valueNode.asText()));
            }
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Integer (" + valueNode.asText() + ")");
          }
        }
      } else if (field.getType().equals(Long.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            if (valueNode.asText().isEmpty()) {
              if (required != null) {
                throw new ValidationErrorException("Template error: " + name + " can not be blank (" + valueNode.asText() + ")");
              } else {
                setField(propertyDescriptorMap, field, object, null);
              }
            } else {
              setField(propertyDescriptorMap, field, object, Long.valueOf(valueNode.asText()));
            }
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Long (" + valueNode.asText() + ")");
          }
        }
      } else if (field.getType().equals(Float.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            if (valueNode.asText().isEmpty()) {
              if (required != null) {
                throw new ValidationErrorException("Template error: " + name + " can not be blank (" + valueNode.asText() + ")");
              } else {
                setField(propertyDescriptorMap, field, object, null);
              }
            } else {
              setField(propertyDescriptorMap, field, object, Float.valueOf(valueNode.asText()));
            }
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Number (" + valueNode.asText() + ")");

          }
        }
      } else if (field.getType().equals(Double.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            if (valueNode.asText().isEmpty()) {
              if (required != null) {
                throw new ValidationErrorException("Template error: " + name + " can not be blank (" + valueNode.asText() + ")");
              } else {
                setField(propertyDescriptorMap, field, object, null);
              }
            } else {
                setField(propertyDescriptorMap, field, object, Double.valueOf(valueNode.asText()));
            }
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Number (" + valueNode.asText() + ")");

          }
        }
      } else if (field.getType().equals(Boolean.class)) {
        if (!valueNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Boolean");
        } else {
          setField(propertyDescriptorMap, field, object, Boolean.valueOf(valueNode.asText()));
        }
      } else if (field.getType().equals(Object.class)) {
        setField(propertyDescriptorMap, field, object, new Object());
      } else if (JsonNode.class.isAssignableFrom(field.getType())) {
        setField(propertyDescriptorMap, field, object, valueNode);
      } else if (Collection.class.isAssignableFrom(field.getType())) {
        Type genericFieldType = field.getGenericType();
        if(genericFieldType instanceof ParameterizedType){
          if (!valueNode.isArray()) {
            throw new ValidationErrorException("Template error: " + name + " must be of type List");
          }
          Type collectionType = ((ParameterizedType) genericFieldType).getActualTypeArguments()[0];
          if (getField(propertyDescriptorMap, field, object) == null) {
            LOG.error("Class " + object.getClass() + " has a Collection type " + field.getName() + " that must be " +
              "non-null ResourcePropertyResolver.populateResourceProperties can be called");
            throw new InternalFailureException("Class " + object.getClass() + " has a Collection type " + field.getName() + " that must be " +
              "non-null ResourcePropertyResolver.populateResourceProperties can be called");
          }
          populateList((Collection<?>) getField(propertyDescriptorMap, field, object), valueNode, collectionType, field.getName(), enforceStrictProperties);
        } else {
          LOG.error("Class " + object.getClass() + " has a Collection type " + field.getName() + " which is a non-parameterized type.  This " +
            "is not supported for ResourcePropertyResolver.populateResourceProperties");
          throw new InternalFailureException("Class " + object.getClass() + " has a Collection type " + field.getName() + " which is a non-parameterized type.  This " +
            "is not supported for ResourcePropertyResolver.populateResourceProperties");
        }
      } else {
        if (getField(propertyDescriptorMap, field, object) == null) {
          try {
            setField(propertyDescriptorMap, field, object, field.getType().newInstance());
          } catch (IllegalAccessException | InstantiationException ex) {
            LOG.error("Class " + object.getClass() + " may not have a public no-arg constructor.  This is needed for ResourcePropertyResolver.populateResourceProperties()");
            throw new InternalFailureException(ex.getMessage());
          }
        }
        populateResourceProperties(getField(propertyDescriptorMap, field, object), valueNode, enforceStrictProperties);
      }
    }
    if (!unprocessedFieldNames.isEmpty() && enforceStrictProperties) {
      throw new ValidationErrorException("Encountered unsupported property or properties "  + unprocessedFieldNames);
    }
  }

  private static void setField(Map<String, PropertyDescriptor> propertyDescriptorMap, Field field, Object object, Object value)
    throws CloudFormationException {
    if (!propertyDescriptorMap.containsKey(field.getName()) ||
      propertyDescriptorMap.get(field.getName()).getWriteMethod() == null) {
      LOG.error("No public setter for " + field.getName() + " in class " + object.getClass().getName());
      throw new InternalFailureException("No public setter for " + field.getName() + " in class " + object.getClass().getName());
    }
    try {
      propertyDescriptorMap.get(field.getName()).getWriteMethod().invoke(object, value);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }

  private static Object getField(Map<String, PropertyDescriptor> propertyDescriptorMap, Field field, Object object)
    throws CloudFormationException{
    if (!propertyDescriptorMap.containsKey(field.getName()) ||
      propertyDescriptorMap.get(field.getName()).getReadMethod() == null) {
      LOG.error("No public getter for " + field.getName() + " in class " + object.getClass().getName());
      throw new InternalFailureException("No public getter for " + field.getName() + " in class " + object.getClass().getName());
    }
    try {
      return propertyDescriptorMap.get(field.getName()).getReadMethod().invoke(object);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }

  private static void populateList(Collection<?> collection, JsonNode valueNode,
                                   Type collectionType, String fieldName, boolean enforceStrictProperties) throws CloudFormationException {
    for (int i=0;i<valueNode.size();i++) {
      Class<?> collectionTypeClass = (Class) collectionType;
      JsonNode itemNode = valueNode.get(i);
      if (collectionTypeClass.equals(String.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type String");
        } else {
          addToCollection(collection, collectionTypeClass, itemNode.asText());
        }
      } else if (collectionTypeClass.equals(Integer.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Integer");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Integer.valueOf(itemNode.asText()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Integer (" + itemNode.asText() + ")");
          }
        }
      } else if (collectionTypeClass.equals(Long.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Long");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Long.valueOf(itemNode.asText()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Long (" + itemNode.asText() + ")");
          }
        }
      } else if (collectionTypeClass.equals(Float.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Float.valueOf(itemNode.asText()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number (" + itemNode.asText() + ")");
          }
        }
      } else if (collectionTypeClass.equals(Double.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Double.valueOf(itemNode.asText()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number (" + itemNode.asText() + ")");
          }
        }
      } else if (collectionTypeClass.equals(Boolean.class)) {
        if (!itemNode.isValueNode()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Boolean");
        } else {
          addToCollection(collection, collectionTypeClass, Boolean.valueOf(itemNode.asText()));
        }
      } else if (Collection.class.isAssignableFrom(collectionTypeClass)) {
        if(collectionType instanceof ParameterizedType){
          if (!itemNode.isArray()) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type List");
          }
          Type innerCollectionType = ((ParameterizedType) collection).getActualTypeArguments()[0];
          Object newObject = null;
          try {
            newObject = collectionTypeClass.newInstance();
          } catch (IllegalAccessException | InstantiationException ex) {
            LOG.error("Class " + collectionTypeClass.getCanonicalName() + " may not have a public no-arg constructor.  This is needed for ResourcePropertyResolver.populateResourceProperties()");
            throw new InternalFailureException(ex.getMessage());
          }
          populateList((Collection<?>) newObject, itemNode, innerCollectionType, fieldName, enforceStrictProperties);
          addToCollection(collection, collectionTypeClass, newObject);
        } else {
          LOG.error("Class " + collectionTypeClass.getCanonicalName() + " has a Collection type which is a non-parameterized type.  This " +
            "is not supported for ResourcePropertyResolver.populateResourceProperties");
          throw new InternalFailureException("Class " + collectionTypeClass.getCanonicalName() + " has a Collection type which is a non-parameterized type.  This " +
            "is not supported for ResourcePropertyResolver.populateResourceProperties");
        }
      } else {
        Object newObject = null;
        try {
          newObject = collectionTypeClass.newInstance();
        } catch (IllegalAccessException | InstantiationException ex) {
          LOG.error("Class " + collectionTypeClass + " may not have a public no-arg constructor.  This is needed for ResourceData.populateFields()");
          throw new InternalFailureException(ex.getMessage());
        }
        populateResourceProperties(newObject, itemNode, enforceStrictProperties);
        addToCollection(collection, collectionTypeClass, newObject);
      }
    }
  }

  private static void addToCollection(Collection<?> collection, Class<?> collectionTypeClass, Object newObject)
    throws CloudFormationException {
    try {
      // TODO: object check
      Method method = collection.getClass().getMethod("add", Object.class);
      method.invoke(collection, newObject);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
      LOG.error("It appears class " + collection.getClass().getCanonicalName() + " which implements collection does not have a public 'add' method.");
      LOG.error(ex, ex);
      throw new InternalFailureException(ex.getMessage());
    }
  }

}
