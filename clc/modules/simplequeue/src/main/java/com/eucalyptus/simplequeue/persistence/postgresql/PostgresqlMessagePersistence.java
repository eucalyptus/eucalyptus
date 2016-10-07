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
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.MessageAttribute;
import com.eucalyptus.simplequeue.MessageAttributeValue;
import com.eucalyptus.simplequeue.SimpleQueueService;
import com.eucalyptus.simplequeue.exceptions.InternalFailureException;
import com.eucalyptus.simplequeue.exceptions.InvalidParameterValueException;
import com.eucalyptus.simplequeue.exceptions.ReceiptHandleIsInvalidException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.MessagePersistence;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
  public Collection<MessageWithReceiveCounts> receiveMessages(Queue queue, Map<String, String> receiveAttributes) throws SimpleQueueException {
    List<MessageWithReceiveCounts> messagesWithExtraInfo = Lists.newArrayList();
    long now = SimpleQueueService.currentTimeSeconds();
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queue.getAccountId())
        .whereEqual(MessageEntity_.queueName, queue.getQueueName())
        // messages with an expiration time of exactly now should expire, so we want the expiration
        // timestamp to be strictly greater than now
        .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
        // messages with a visibility time of exactly now should be visible, so we want the the visibility
        // timestamp to be less than or equal to now.
        .where(Entities.restriction(MessageEntity.class).le(MessageEntity_.visibleTimestampSecs, now))
        .list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          Message message = jsonToMessage(messageEntity.getMessageJson());
          message.setMessageId(messageEntity.getMessageId());
          // set receive timestamp if first time being received
          if (messageEntity.getReceiveCount() == 0) {
            message.getAttribute().add(new Attribute(Constants.APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, "" + now));
            // add the new attribute
            messageEntity.setMessageJson(messageToJson(message));
          }
          // update visible timestamp (use visibility timeout)
          int visibilityTimeout = queue.getVisibilityTimeout();
          if (receiveAttributes.containsKey(Constants.VISIBILITY_TIMEOUT)) {
            visibilityTimeout = Integer.parseInt(receiveAttributes.get(Constants.VISIBILITY_TIMEOUT));
          }
          messageEntity.setVisibleTimestampSecs(now + visibilityTimeout);
          // update receive count (not stored in message json as updated often)
          messageEntity.setLocalReceiveCount(messageEntity.getLocalReceiveCount() + 1);
          messageEntity.setReceiveCount(messageEntity.getReceiveCount() + 1);

          // Set the 'attributes' that are stored as first class fields
          message.getAttribute().add(new Attribute(Constants.APPROXIMATE_RECEIVE_COUNT, "" + messageEntity.getReceiveCount()));
          // send timestamp isn't updated but used in queries.  The attribute is in seconds though, so convert
          message.getAttribute().add(new Attribute(Constants.SENT_TIMESTAMP, "" + (messageEntity.getSentTimestampSecs())));
          message.setReceiptHandle(messageEntity.getAccountId() + ":" + messageEntity.getQueueName() + ":" + messageEntity.getMessageId() + ":" + messageEntity.getLocalReceiveCount());
          MessageWithReceiveCounts messageWithReceiveCounts = new MessageWithReceiveCounts();
          messageWithReceiveCounts.setMessage(message);
          messageWithReceiveCounts.setLocalReceiveCount(messageEntity.getLocalReceiveCount());
          messageWithReceiveCounts.setReceiveCount(messageEntity.getReceiveCount());
          messagesWithExtraInfo.add(messageWithReceiveCounts);
        }
      }
      db.commit();
      return messagesWithExtraInfo;
    }
  }

  @Override
  public void sendMessage(Queue queue, Message message, Map<String, String> sendAttributes) throws SimpleQueueException {
    // TODO: limit (a lot)
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      MessageEntity messageEntity = new MessageEntity();
      messageEntity.setMessageId(message.getMessageId());
      messageEntity.setAccountId(queue.getAccountId());
      messageEntity.setQueueName(queue.getQueueName());
      Map<String, String> attributeMap = Maps.newHashMap();
      if (message.getAttribute() != null) {
        for (Attribute attribute: message.getAttribute()) {
          attributeMap.put(attribute.getName(), attribute.getValue());
        }
      }
      messageEntity.setReceiveCount(0);
      messageEntity.setLocalReceiveCount(0);
      messageEntity.setSentTimestampSecs(SimpleQueueService.currentTimeSeconds());
      messageEntity.setExpiredTimestampSecs(messageEntity.getSentTimestampSecs() + queue.getMessageRetentionPeriod());

      int delaySeconds = queue.getDelaySeconds();
      if (sendAttributes.containsKey(Constants.DELAY_SECONDS)) {
        delaySeconds = Integer.parseInt(sendAttributes.get(Constants.DELAY_SECONDS));
      }
      messageEntity.setVisibleTimestampSecs(messageEntity.getSentTimestampSecs() + delaySeconds);
      messageEntity.setMessageJson(messageToJson(message));
      Entities.persist(messageEntity);
      db.commit();
    }
  }

  @Override
  public void deleteMessage(Queue queue, String receiptHandle) throws SimpleQueueException {
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
    if (!receiptHandleAccountId.equals(queue.getAccountId()) || !receiptHandleQueueName.equals(queue.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }

    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {

      // No errors if no results
      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queue.getAccountId())
        .whereEqual(MessageEntity_.queueName, queue.getQueueName())
        .whereEqual(MessageEntity_.messageId, messageId)
        .whereEqual(MessageEntity_.receiveCount, receiveCount)
        .list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          Entities.delete(messageEntity);
        }
      }
      db.commit();
    }
  }

  @Override
  public void changeMessageVisibility(Queue queue, String receiptHandle, Integer visibilityTimeout) throws SimpleQueueException {
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
    if (!receiptHandleAccountId.equals(queue.getAccountId()) || !receiptHandleQueueName.equals(queue.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }

    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      long now = SimpleQueueService.currentTimeSeconds();
      // No errors if no results
      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queue.getAccountId())
        .whereEqual(MessageEntity_.queueName, queue.getQueueName())
        .whereEqual(MessageEntity_.messageId, messageId)
        .whereEqual(MessageEntity_.receiveCount, receiveCount)
        .list();
      int countedResults = 0;
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          countedResults++;
          messageEntity.setVisibleTimestampSecs(now + visibilityTimeout);
        }
      }
      if (countedResults == 0) {
        throw new InvalidParameterValueException("Value " + receiptHandle + " for parameter ReceiptHandle is invalid. Reason: Message does not exist or is not available for visibility timeout change.");
      }
      db.commit();
    }

  }

  @Override
  public void moveMessageToDeadLetterQueue(Queue queue, Message message, Queue deadLetterQueue) {
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {

      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queue.getAccountId())
        .whereEqual(MessageEntity_.queueName, queue.getQueueName())
        .whereEqual(MessageEntity_.messageId, message.getMessageId())
        .list();
      if (messageEntityList != null) {
        for (MessageEntity messageEntity:messageEntityList) {
          // being called this message was received one too many times, reset the count
          messageEntity.setReceiveCount(messageEntity.getReceiveCount() - 1);
          // reset the local count
          messageEntity.setLocalReceiveCount(0);
          messageEntity.setAccountId(deadLetterQueue.getAccountId());
          messageEntity.setQueueName(deadLetterQueue.getQueueName());
          messageEntity.setExpiredTimestampSecs(messageEntity.getSentTimestampSecs() + deadLetterQueue.getMessageRetentionPeriod());
        }
      }
      db.commit();
    }

  }

  @Override
  public void deleteAllMessages(Queue queue) {
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      Entities.delete(
        Entities.restriction( MessageEntity.class ).all(
          Entities.restriction( MessageEntity.class ).equal( MessageEntity_.accountId, queue.getAccountId() ).build( ),
          Entities.restriction( MessageEntity.class ).equal( MessageEntity_.queueName, queue.getQueueName() ).build( )
        ).build()
      ).delete();
      db.commit();
    }
  }

  @Override
  public Map<String, String> getApproximateMessageCounts(Queue queue) {
    Map<String, String> result = Maps.newHashMap();
    long now = SimpleQueueService.currentTimeSeconds();
    // TODO: see if we can do this with a more efficient query
    // first get 'in flight' messages (
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      // ApproximateNumberOfMessagesDelayed - returns the approximate number of messages that are pending to be added to the queue.
      // i.e. not seen yet and not visible yet
      result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
        Entities.count(MessageEntity.class)
            .whereEqual(MessageEntity_.accountId, queue.getAccountId())
            .whereEqual(MessageEntity_.queueName, queue.getQueueName())
              // messages with an expiration time of exactly now should expire, so we want the expiration
              // timestamp to be strictly greater than now
            .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
              // messages that is delayed is not yet visible, so we want the visibility timestamp to be
              // strictly greater than now
            .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.visibleTimestampSecs, now))
            .whereEqual(MessageEntity_.receiveCount, 0)
            .uniqueResult()
            .toString()
      );
      // ApproximateNumberOfMessagesNotVisible - returns the approximate number of messages that are not timed-out and not deleted. For more information, see Resources Required to Process Messages in the Amazon SQS Developer Guide.
      result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
        Entities.count(MessageEntity.class)
          .whereEqual(MessageEntity_.accountId, queue.getAccountId())
          .whereEqual(MessageEntity_.queueName, queue.getQueueName())
            // messages with an expiration time of exactly now should expire, so we want the expiration
            // timestamp to be strictly greater than now
          .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
            // messages that are not visible are not delayed.  A message that is not visible must
            // have been received at least once.  (Because otherwise it is delayed, visible, or expired)
          .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.visibleTimestampSecs, now))
          .where(Entities.restriction(MessageEntity.class).notEqual(MessageEntity_.receiveCount, 0))
          .uniqueResult()
          .toString()
      );
      //      ApproximateNumberOfMessages - returns the approximate number of visible messages in a queue.      result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
      result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES,
        Entities.count(MessageEntity.class)
          .whereEqual(MessageEntity_.accountId, queue.getAccountId())
          .whereEqual(MessageEntity_.queueName, queue.getQueueName())
            // messages with an expiration time of exactly now should expire, so we want the expiration
            // timestamp to be strictly greater than now
          .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
            // messages with a visibility time of exactly now should be visible, so we want the the visibility
            // timestamp to be less than or equal to now.
          .where(Entities.restriction(MessageEntity.class).le(MessageEntity_.visibleTimestampSecs, now))
          .uniqueResult()
          .toString()
      );
      // TODO: there is probably a more efficient or clever query we could make to group the above
      // in one query.  It may or may not be worth it.
    }
    return result;
  }

}
