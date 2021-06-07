/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.common.msgs.Attribute;
import com.eucalyptus.simplequeue.common.msgs.Message;
import com.eucalyptus.simplequeue.common.msgs.MessageAttribute;
import com.eucalyptus.simplequeue.common.msgs.MessageAttributeValue;
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
