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
 *
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence.postgresql;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simplequeue.Attribute;
import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.MessageAttribute;
import com.eucalyptus.simplequeue.MessageAttributeValue;
import com.eucalyptus.simplequeue.SimpleQueueService;
import com.eucalyptus.simplequeue.exceptions.InternalFailureException;
import com.eucalyptus.simplequeue.exceptions.ReceiptHandleIsInvalidException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.MessagePersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ethomas on 9/16/16.
 */
public class PostgresqlMessagePersistence implements MessagePersistence {
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

  private static Message jsonToMessage(String messageJson) throws SimpleQueueException {
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
          // a couple of attributes are stored in first class fields, not the message body
          if (name.equals(SimpleQueueService.MessageAttributeName.ApproximateReceiveCount.toString())) continue;
          if (name.equals(SimpleQueueService.EucaInternalMessageAttributeName.EucaLocalReceiveCount.toString())) continue;
          if (name.equals(SimpleQueueService.MessageAttributeName.SentTimestamp.toString())) continue;
          message.getAttribute().add(new Attribute(name, attributesNode.get(name).textValue()));
        }
      }
    } catch (IOException | ClassCastException e) {
      throw new InternalFailureException("Invalid JSON");
    }
    return message;
  }

  private static String messageToJson(Message message) {
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

  @Override
  public Collection<Message> receiveMessages(String accountId, String queueName, int visibilityTimeout, int maxNumberOfMessages) throws SimpleQueueException {
    // TODO: limit (a lot)
    List<Message> messages = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      Criteria criteria = Entities.createCriteria(MessageEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName));
      List<MessageEntity> messageEntityList = criteria.list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          Message message = jsonToMessage(messageEntity.getMessageJson());
          message.setMessageId(messageEntity.getMessageId());
          // set receive timestamp if first time being received
          if (messageEntity.getReceiveCount() == 0) {
            message.getAttribute().add(
              new Attribute(
                SimpleQueueService.MessageAttributeName.ApproximateFirstReceiveTimestamp.toString(),
                "" + System.currentTimeMillis()
              )
            );
            // add the new attribute
            messageEntity.setMessageJson(messageToJson(message));
          }
          // update receive count (not stored in message json as updated often)
          messageEntity.setLocalReceiveCount(messageEntity.getLocalReceiveCount() + 1);
          messageEntity.setReceiveCount(messageEntity.getReceiveCount() + 1);
          // but we should update the attributes being returned anyway
          message.getAttribute().add(
            new Attribute(SimpleQueueService.MessageAttributeName.ApproximateReceiveCount.toString(),
              "" + messageEntity.getReceiveCount())
          );
          message.getAttribute().add(
            new Attribute(SimpleQueueService.EucaInternalMessageAttributeName.EucaLocalReceiveCount.toString(),
              "" + messageEntity.getLocalReceiveCount())
          );
          // send timestamp isn't updated but used in queries
          message.getAttribute().add(
            new Attribute(SimpleQueueService.MessageAttributeName.SentTimestamp.toString(),
              "" + messageEntity.getSentTimestamp())
          );
          message.setReceiptHandle(messageEntity.getAccountId() + ":" + messageEntity.getQueueName() + ":" + messageEntity.getMessageId() + ":" + messageEntity.getLocalReceiveCount());
          messages.add(message);
        }
      }
      db.commit();
      return messages;
    }
  }

  @Override
  public void sendMessage(String accountId, String queueName, Message message) throws SimpleQueueException {
    // TODO: limit (a lot)
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      MessageEntity messageEntity = new MessageEntity();
      messageEntity.setMessageId(message.getMessageId());
      messageEntity.setAccountId(accountId);
      messageEntity.setQueueName(queueName);
      Map<String, String> attributeMap = Maps.newHashMap();
      if (message.getAttribute() != null) {
        for (Attribute attribute: message.getAttribute()) {
          attributeMap.put(attribute.getName(), attribute.getValue());
        }
      }
      if (attributeMap.get(SimpleQueueService.MessageAttributeName.ApproximateReceiveCount.toString()) != null) {
        messageEntity.setReceiveCount(Integer.parseInt(attributeMap.get(SimpleQueueService.MessageAttributeName.ApproximateReceiveCount.toString())));
      } else {
        throw new InternalFailureException("No value passed in for receive count in send message");
      }
      if (attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaLocalReceiveCount.toString()) != null) {
        messageEntity.setLocalReceiveCount(Integer.parseInt(attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaLocalReceiveCount.toString())));
      } else {
        throw new InternalFailureException("No value passed in for local receive count in send message");
      }
      if (attributeMap.get(SimpleQueueService.MessageAttributeName.SentTimestamp.toString()) != null) {
        messageEntity.setSentTimestamp(Long.parseLong(attributeMap.get(SimpleQueueService.MessageAttributeName.SentTimestamp.toString())));
      } else {
        throw new InternalFailureException("No value passed in for sent timestamp in send message");
      }

      if (attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaDelaySeconds.toString()) != null) {
        messageEntity.setVisibleTimestamp(messageEntity.getSentTimestamp() + 1000 * Long.parseLong(attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaDelaySeconds.toString())));
      } else {
        throw new InternalFailureException("No value passed in for delay seconds in send message");
      }

      if (attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaMessageRetentionPeriod.toString()) != null) {
        messageEntity.setExpiredTimestamp(messageEntity.getSentTimestamp() + 1000 * Long.parseLong(attributeMap.get(SimpleQueueService.EucaInternalMessageAttributeName.EucaMessageRetentionPeriod.toString())));
      } else {
        throw new InternalFailureException("No value passed in for message retention period in send message");
      }
      messageEntity.setMessageJson(messageToJson(message));
      Entities.persist(messageEntity);
      db.commit();
    }
  }

  @Override
  public void deleteMessage(String accountId, String queueName, String receiptHandle) throws SimpleQueueException {
    // receipt handle (currently) looks like accountId:queueName:message-id:<message-id>-<receive-count>
    StringTokenizer stok = new StringTokenizer(receiptHandle,":");
    if (stok.countTokens() != 4) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    String receiptHandleAccountId = stok.nextToken();
    String receiptHandleQueueName = stok.nextToken();
    String messageId = stok.nextToken();
    int receiveCount = 0;
    try {
      receiveCount = Integer.parseInt(stok.nextToken());
    } catch (NumberFormatException e) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    if (!receiptHandleAccountId.equals(accountId) || !receiptHandleQueueName.equals(queueName)) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }

    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      Criteria criteria = Entities.createCriteria(MessageEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName))
        .add(Restrictions.eq("messageId", messageId))
        .add(Restrictions.eq("receiveCount", receiveCount));

      // No errors if no results
      List<MessageEntity> messageEntityList = criteria.list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          Entities.delete(messageEntity);
        }
      }
      db.commit();
    }
  }

  @Override
  public void deleteAllMessages(String accountId, String queueName) {
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      Criteria criteria = Entities.createCriteria(MessageEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName));

      List<MessageEntity> messageEntityList = criteria.list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          Entities.delete(messageEntity);
        }
      }
      db.commit();
    }
  }
}
