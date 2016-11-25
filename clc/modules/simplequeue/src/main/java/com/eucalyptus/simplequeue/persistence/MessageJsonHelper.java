/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.Attribute;
import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.MessageAttribute;
import com.eucalyptus.simplequeue.MessageAttributeValue;
import com.eucalyptus.simplequeue.exceptions.InternalFailureException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import java.io.IOException;

/**
 * Created by ethomas on 11/23/16.
 */
public class MessageJsonHelper {
  private static final String ATTRIBUTES = "Attributes";
  private static final String MD5_OF_MESSAGE_ATTRIBUTES = "Md5OfMessageAttributes";
  private static final String BINARY_LIST_VALUE = "BinaryListValue";
  private static final String STRING_LIST_VALUE = "StringListValue";
  private static final String BINARY_VALUE = "BinaryValue";
  private static final String STRING_VALUE = "StringValue";
  private static final String DATA_TYPE = "DataType";
  private static final String MESSAGE_ATTRIBUTES = "MessageAttributes";
  private static final String MD5_OF_BODY = "Md5OfBody";
  private static final String BODY = "Body";

  public static Message jsonToMessage(String messageJson) throws SimpleQueueException {
    if (messageJson == null) return null;
    Message message = new Message();
    try {
      ObjectNode messageNode = (ObjectNode) new ObjectMapper().readTree(messageJson);
      if (messageNode.has(BODY)) {
        message.setBody(messageNode.get(BODY).textValue());
      }
      if (messageNode.has(MD5_OF_BODY)) {
        message.setmD5OfBody(messageNode.get(MD5_OF_BODY).textValue());
      }
      if (messageNode.has(MESSAGE_ATTRIBUTES)) {
        ObjectNode messageAttributesNode = (ObjectNode) messageNode.get(MESSAGE_ATTRIBUTES);
        for (String name : Lists.newArrayList(messageAttributesNode.fieldNames())) {
          ObjectNode messageAttributeValueNode = (ObjectNode) messageAttributesNode.get(name);
          MessageAttributeValue messageAttributeValue = new MessageAttributeValue();
          if (messageAttributeValueNode.has(DATA_TYPE)) {
            messageAttributeValue.setDataType(messageAttributeValueNode.get(DATA_TYPE).textValue());
          }
          if (messageAttributeValueNode.has(STRING_VALUE)) {
            messageAttributeValue.setStringValue(messageAttributeValueNode.get(STRING_VALUE).textValue());
          }
          if (messageAttributeValueNode.has(BINARY_VALUE)) {
            messageAttributeValue.setBinaryValue(messageAttributeValueNode.get(BINARY_VALUE).textValue());
          }
          if (messageAttributeValueNode.has(STRING_LIST_VALUE)) {
            for (int i = 0; i < messageAttributeValueNode.get(STRING_LIST_VALUE).size(); i++) {
              messageAttributeValue.getStringListValue().add(messageAttributeValueNode.get(STRING_LIST_VALUE).get(i).textValue());
            }
          }
          if (messageAttributeValueNode.has(BINARY_LIST_VALUE)) {
            for (int i = 0; i < messageAttributeValueNode.get(BINARY_LIST_VALUE).size(); i++) {
              messageAttributeValue.getBinaryListValue().add(messageAttributeValueNode.get(BINARY_LIST_VALUE).get(i).textValue());
            }
          }
          MessageAttribute messageAttribute = new MessageAttribute();
          messageAttribute.setName(name);
          messageAttribute.setValue(messageAttributeValue);
          message.getMessageAttribute().add(messageAttribute);
        }
      }
      if (messageNode.has(MD5_OF_MESSAGE_ATTRIBUTES)) {
        message.setmD5OfMessageAttributes(messageNode.get(MD5_OF_MESSAGE_ATTRIBUTES).textValue());
      }
      if (messageNode.has(ATTRIBUTES)) {
        ObjectNode attributesNode = (ObjectNode) messageNode.get(ATTRIBUTES);
        for (String name : Lists.newArrayList(attributesNode.fieldNames())) {
          message.getAttribute().add(new Attribute(name, attributesNode.get(name).textValue()));
        }
      }
    } catch (IOException | ClassCastException e) {
      throw new InternalFailureException("Invalid JSON");
    }
    return message;
  }

  public static String messageToJson(Message message) {
    if (message == null) return null;
    ObjectNode messageNode = new ObjectMapper().createObjectNode();
    if (message.getBody() != null) {
      messageNode.put(BODY, message.getBody());
    }
    if (message.getmD5OfBody() != null) {
      messageNode.put(MD5_OF_BODY, message.getmD5OfBody());
    }
    if (message.getMessageAttribute() != null) {
      ObjectNode messageAttributeNode = messageNode.putObject(MESSAGE_ATTRIBUTES);
      for (MessageAttribute messageAttribute : message.getMessageAttribute()) {
        if (messageAttribute.getValue() != null) {
          ObjectNode messageAttributeValueNode = messageAttributeNode.putObject(messageAttribute.getName());
          if (messageAttribute.getValue().getDataType() != null) {
            messageAttributeValueNode.put(DATA_TYPE, messageAttribute.getValue().getDataType());
          }
          if (messageAttribute.getValue().getStringValue() != null) {
            messageAttributeValueNode.put(STRING_VALUE, messageAttribute.getValue().getStringValue());
          }
          if (messageAttribute.getValue().getBinaryValue() != null) {
            messageAttributeValueNode.put(BINARY_VALUE, messageAttribute.getValue().getBinaryValue());
          }
          if (messageAttribute.getValue().getStringListValue() != null) {
            ArrayNode messageAttributeValueStringListNode = messageAttributeValueNode.putArray(STRING_LIST_VALUE);
            for (String value : messageAttribute.getValue().getStringListValue()) {
              messageAttributeValueStringListNode.add(value);
            }
          }
          if (messageAttribute.getValue().getBinaryListValue() != null) {
            ArrayNode messageAttributeValueBinaryListNode = messageAttributeValueNode.putArray(BINARY_LIST_VALUE);
            for (String value : messageAttribute.getValue().getBinaryListValue()) {
              messageAttributeValueBinaryListNode.add(value);
            }
          }
        }
      }
    }
    if (message.getmD5OfMessageAttributes() != null) {
      messageNode.put(MD5_OF_MESSAGE_ATTRIBUTES, message.getmD5OfMessageAttributes());
    }
    if (message.getAttribute() != null) {
      ObjectNode attributeNode = messageNode.putObject(ATTRIBUTES);
      for (Attribute attribute : message.getAttribute()) {
        attributeNode.put(attribute.getName(), attribute.getValue());
      }
    }
    return messageNode.toString();
  }
}
