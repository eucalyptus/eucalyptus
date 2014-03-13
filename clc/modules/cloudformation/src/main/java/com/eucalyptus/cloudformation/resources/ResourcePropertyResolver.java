package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2EBSBlockDevice;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2MountPoint;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterface;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterfacePrivateIPSpecification;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.transform.ToString;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class ResourcePropertyResolver {
  private static final Logger LOG = Logger.getLogger(ResourcePropertyResolver.class);
  public static JsonNode getJsonNodeFromResourceProperties(ResourceProperties resourceProperties) throws CloudFormationException {
    return getJsonNodeFromObject(resourceProperties);
  }

  private static JsonNode getJsonNodeFromObject(Object object) throws CloudFormationException {
    if (object == null) return null;
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode jsonNode = mapper.createObjectNode();
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
      } else if (objectValue instanceof Double) {
        jsonNode.put(name, String.valueOf((Double) objectValue));
      } else if (objectValue instanceof Boolean) {
        jsonNode.put(name, String.valueOf((Boolean) objectValue));
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
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode jsonNode = mapper.createArrayNode();
    for (Object object: collection) {
      if (object == null) {
        jsonNode.add((JsonNode) null); // TODO: really?
      } else if (object instanceof String) {
        jsonNode.add((String) object);
      } else if (object instanceof Integer) {
        jsonNode.add(String.valueOf((Integer) object));
      } else if (object instanceof Double) {
        jsonNode.add(String.valueOf((Double) object));
      } else if (object instanceof Boolean) {
        jsonNode.add(String.valueOf((Boolean) object));
      } else if (object instanceof Collection) {
        jsonNode.add(getJsonNodeFromCollection((Collection) object));
      } else {
        jsonNode.add(getJsonNodeFromObject(object));
      }
    }
    return jsonNode;
  }


  public static void populateResourceProperties(Object object, JsonNode jsonNode) throws CloudFormationException {
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
      LOG.info("Populating property with: " + name + "=" + valueNode + " " + valueNode.getClass());
      if (field.getType().equals(String.class)) {
        if (!valueNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type String");
        } else {
          setField(propertyDescriptorMap, field, object, valueNode.textValue());
        }
      } else if (field.getType().equals(Integer.class)) {
        if (!valueNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            setField(propertyDescriptorMap, field, object, Integer.valueOf(valueNode.textValue()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Integer (" + valueNode.textValue() + ")");
          }
        }
      } else if (field.getType().equals(Double.class)) {
        if (!valueNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Number");
        } else {
          try {
            setField(propertyDescriptorMap, field, object, Double.valueOf(valueNode.textValue()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + name + " must be of type Number (" + valueNode.textValue() + ")");

          }
        }
      } else if (field.getType().equals(Boolean.class)) {
        if (!valueNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + name + " must be of type Boolean");
        } else {
          setField(propertyDescriptorMap, field, object, Boolean.valueOf(valueNode.textValue()));
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
          populateList((Collection<?>) getField(propertyDescriptorMap, field, object), valueNode, collectionType, field.getName());
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
        populateResourceProperties(getField(propertyDescriptorMap, field, object), valueNode);
      }
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
                                      Type collectionType, String fieldName) throws CloudFormationException {
    for (int i=0;i<valueNode.size();i++) {
      Class<?> collectionTypeClass = (Class) collectionType;
      JsonNode itemNode = valueNode.get(i);
      if (collectionTypeClass.equals(String.class)) {
        if (!itemNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type String");
        } else {
          addToCollection(collection, collectionTypeClass, itemNode.textValue());
        }
      } else if (collectionTypeClass.equals(Integer.class)) {
        if (!itemNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Integer");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Integer.valueOf(itemNode.textValue()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Integer (" + itemNode.textValue() + ")");
          }
        }
      } else if (collectionTypeClass.equals(Double.class)) {
        if (!itemNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number");
        } else {
          try {
            addToCollection(collection, collectionTypeClass, Double.valueOf(itemNode.textValue()));
          } catch (NumberFormatException ex) {
            throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Number (" + itemNode.textValue() + ")");

          }
        }
      } else if (collectionTypeClass.equals(Boolean.class)) {
        if (!itemNode.isTextual()) {
          throw new ValidationErrorException("Template error: " + fieldName + " must have members of type Boolean");
        } else {
          addToCollection(collection, collectionTypeClass, Boolean.valueOf(itemNode.textValue()));
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
          populateList((Collection<?>) newObject, itemNode, innerCollectionType, fieldName);
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
          LOG.error("Class " + collectionTypeClass + " may not have a public no-arg constructor.  This is needed for Reso/**/urceData.populateFields()");
          throw new InternalFailureException(ex.getMessage());
        }
        populateResourceProperties(newObject, itemNode);
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
