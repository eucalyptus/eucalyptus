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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence.postgresql;

import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simplequeue.common.msgs.Attribute;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.common.msgs.Message;
import com.eucalyptus.simplequeue.SimpleQueueService;
import com.eucalyptus.simplequeue.exceptions.InvalidParameterValueException;
import com.eucalyptus.simplequeue.exceptions.ReceiptHandleIsInvalidException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.MessageJsonHelper;
import com.eucalyptus.simplequeue.persistence.MessagePersistence;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.util.Either;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Created by ethomas on 9/16/16.
 */
public class PostgresqlMessagePersistence implements MessagePersistence {

  @Override
  public UUID getNewMessageUUID() {
    return UUID.randomUUID();
  }

  @Override
  public Collection<Message> receiveMessages(Queue queue, Map<String, String> receiveAttributes) throws SimpleQueueException {
    Either<SimpleQueueException, List<Message>> returnValue =
      Entities.asDistinctTransaction(MessageEntity.class, new Function<Void, Either<SimpleQueueException, List<Message>>>() {

      @Nullable
      @Override
      public Either<SimpleQueueException, List<Message>> apply(@Nullable Void aVoid) {
        Either<SimpleQueueException, List<Message>> either;
        long now = SimpleQueueService.currentTimeSeconds();
        List<Message> messages = Lists.newArrayList();
        Optional<SimpleQueueException> simpleQueueExceptionOptional;
        try {
          List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
            .whereEqual(MessageEntity_.accountId, queue.getAccountId())
            .whereEqual(MessageEntity_.queueName, queue.getQueueName())
//      -- Just delete the expired messages here
//        // messages with an expiration time of exactly now should expire, so we want the expiration
//        // timestamp to be strictly greater than now
//        .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
              // messages with a visibility time of exactly now should be visible, so we want the the visibility
              // timestamp to be less than or equal to now.
            .where(Entities.restriction(MessageEntity.class).le(MessageEntity_.visibleTimestampSecs, now))
            .orderBy(MessageEntity_.visibleTimestampSecs)
              // TODO: consider what happens if this returns too many results.
            .list();
          boolean deadLetterQueue = false;
          String deadLetterQueueAccountId = null;
          String deadLetterQueueName = null;
          int maxReceiveCount = 0;
          long deadLetterQueueMessageRetentionPeriod = 0;
          try {
            Ern deadLetterQueueErn = Ern.parse(receiveAttributes.get(Constants.DEAD_LETTER_TARGET_ARN));
            deadLetterQueueAccountId = deadLetterQueueErn.getAccount();
            deadLetterQueueName = deadLetterQueueErn.getResourceName();
            maxReceiveCount = Integer.parseInt(receiveAttributes.get(Constants.MAX_RECEIVE_COUNT));
            deadLetterQueueMessageRetentionPeriod = Long.parseLong(receiveAttributes.get(Constants.MESSAGE_RETENTION_PERIOD));
            deadLetterQueue = true;
          } catch (Exception ignore) {
          }
          int numMessages = 0;
          int maxNumMessages = 1;
          try {
            maxNumMessages = Integer.parseInt(receiveAttributes.get(Constants.MAX_NUMBER_OF_MESSAGES));
          } catch (Exception ignore) {
          }

          if (messageEntityList != null) {
            for (MessageEntity messageEntity : messageEntityList) {
              if (messageEntity.getExpiredTimestampSecs() <= now) {
                Entities.delete(messageEntity);
                continue;
              }
              if (deadLetterQueue && messageEntity.getLocalReceiveCount() >= maxReceiveCount) {
                // move to dead letter
                messageEntity.setLocalReceiveCount(0);
                messageEntity.setAccountId(deadLetterQueueAccountId);
                messageEntity.setQueueName(deadLetterQueueName);
                messageEntity.setExpiredTimestampSecs(messageEntity.getSentTimestampSecs() + deadLetterQueueMessageRetentionPeriod);
                continue;
              }

              Message message = MessageJsonHelper.jsonToMessage(messageEntity.getMessageJson());
              message.setMessageId(messageEntity.getMessageId());
              // set receive timestamp if first time being received
              if (messageEntity.getReceiveCount() == 0) {
                message.getAttribute().add(new Attribute(Constants.APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, "" + now));
                // add the new attribute
                messageEntity.setMessageJson(MessageJsonHelper.messageToJson(message));
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
              messages.add(message);
              numMessages++;
              if (numMessages >= maxNumMessages) break;
            }
          }
          either = Either.right(messages);
        } catch (SimpleQueueException ex) {
          either = Either.left(ex);
        }
        return either;
      }
    }).apply(null);
    if (returnValue.isLeft()) {
      throw returnValue.getLeft();
    } else {
      return returnValue.getRight();
    }
  }

  @Override
  public void sendMessage(Queue queue, Message message, Map<String, String> sendAttributes) {
    Entities.asDistinctTransaction(MessageEntity.class, new Function<Void, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable Void aVoid) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setMessageId(message.getMessageId());
        messageEntity.setAccountId(queue.getAccountId());
        messageEntity.setQueueName(queue.getQueueName());
        Map<String, String> attributeMap = Maps.newHashMap();
        if (message.getAttribute() != null) {
          for (Attribute attribute : message.getAttribute()) {
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
        messageEntity.setMessageJson(MessageJsonHelper.messageToJson(message));
        Entities.persist(messageEntity);
        return null;
      }
    }).apply(null);
  }

  @Override
  public boolean deleteMessage(Queue.Key queueKey, String receiptHandle) throws SimpleQueueException {
    boolean found = false;
    // receipt handle (currently) looks like accountId:queueName:message-id:receive-count
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
    if (!receiptHandleAccountId.equals(queueKey.getAccountId()) || !receiptHandleQueueName.equals(queueKey.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }

    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {

      // No errors if no results
      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
        .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
        .whereEqual(MessageEntity_.messageId, messageId)
        .whereEqual(MessageEntity_.receiveCount, receiveCount)
        .list();
      if (messageEntityList != null) {
        found = true;
        for (MessageEntity messageEntity:messageEntityList) {
          Entities.delete(messageEntity);
        }
      }
      db.commit();
    }
    return found;
  }

  @Override
  public void changeMessageVisibility(Queue.Key queueKey, String receiptHandle, Integer visibilityTimeout) throws SimpleQueueException {
    // receipt handle (currently) looks like accountId:queueName:message-id:receive-count
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
    if (!receiptHandleAccountId.equals(queueKey.getAccountId()) || !receiptHandleQueueName.equals(queueKey.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }

    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      long now = SimpleQueueService.currentTimeSeconds();
      // No errors if no results
      List<MessageEntity> messageEntityList = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
        .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
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
  public Long getApproximateAgeOfOldestMessage(Queue.Key queueKey) {
    long now = SimpleQueueService.currentTimeSeconds();
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      List<MessageEntity> messageEntities = Entities.criteriaQuery(MessageEntity.class)
        .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
        .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
        // messages with an expiration time of exactly now should expire, so we want the expiration
        // timestamp to be strictly greater than now
        .where(Entities.restriction(MessageEntity.class).gt(MessageEntity_.expiredTimestampSecs, now))
        .orderBy(MessageEntity_.sentTimestampSecs)
        .maxResults(1)
        .list();
      if (messageEntities == null || messageEntities.size() == 0) return 0L;
      return now - messageEntities.get(0).getSentTimestampSecs();
    }
  }


  @Override
  public void deleteAllMessages(Queue.Key queueKey) {
    try ( TransactionResource db =
            Entities.transactionFor(MessageEntity.class) ) {
      Entities.delete(
        Entities.restriction( MessageEntity.class ).all(
          Entities.restriction( MessageEntity.class ).equal( MessageEntity_.accountId, queueKey.getAccountId() ).build( ),
          Entities.restriction( MessageEntity.class ).equal( MessageEntity_.queueName, queueKey.getQueueName() ).build( )
        ).build()
      ).delete();
      db.commit();
    }
  }

  @Override
  public Map<String, String> getApproximateMessageCounts(Queue.Key queueKey) {
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
            .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
            .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
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
          .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
          .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
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
          .whereEqual(MessageEntity_.accountId, queueKey.getAccountId())
          .whereEqual(MessageEntity_.queueName, queueKey.getQueueName())
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
