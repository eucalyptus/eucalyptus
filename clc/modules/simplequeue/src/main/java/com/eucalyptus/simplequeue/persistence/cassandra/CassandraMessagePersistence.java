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
package com.eucalyptus.simplequeue.persistence.cassandra;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.utils.UUIDs;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.simplequeue.Attribute;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.SimpleQueueService;
import com.eucalyptus.simplequeue.exceptions.InvalidParameterValueException;
import com.eucalyptus.simplequeue.exceptions.ReceiptHandleIsInvalidException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.MessageJsonHelper;
import com.eucalyptus.simplequeue.persistence.MessagePersistence;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ethomas on 11/23/16.
 */
public class CassandraMessagePersistence implements MessagePersistence {

  static Random random = new Random();

  private static final int NUM_PARTITIONS = 25;

  private static final List<String> partitionTokens = IntStream.range(0, NUM_PARTITIONS).boxed().map(String::valueOf).collect(Collectors.toList());

  @Override
  public UUID getNewMessageUUID() {
    return UUIDs.timeBased();
  }

  private static class ReceiveCountsAndExpirationTimestamp {
    private Integer receiveCount;
    private Integer totalReceiveCount;
    private Date expirationTimestamp;
    private Long sendTimeSecs;

    private ReceiveCountsAndExpirationTimestamp(Integer receiveCount, Integer totalReceiveCount, Date expirationTimestamp, Long sendTimeSecs) {
      this.receiveCount = receiveCount;
      this.totalReceiveCount = totalReceiveCount;
      this.expirationTimestamp = expirationTimestamp;
      this.sendTimeSecs = sendTimeSecs;
    }

    public Integer getReceiveCount() {
      return receiveCount;
    }

    public Integer getTotalReceiveCount() {
      return totalReceiveCount;
    }

    public Date getExpirationTimestamp() {
      return expirationTimestamp;
    }

    public Long getSentTimeSecs() {
      return sendTimeSecs;
    }
  }

  @Override
  public Collection<Message> receiveMessages(Queue queue, Map<String, String> receiveAttributes) throws SimpleQueueException {
    List<String> randomPartitionTokens = Lists.newArrayList(partitionTokens);
    Collections.shuffle(randomPartitionTokens);
    Session session = CassandraSessionManager.getSession();
    List<Message> messages = Lists.newArrayList();

    int maxNumMessages = 1;
    try {
      maxNumMessages = Integer.parseInt(receiveAttributes.get(Constants.MAX_NUMBER_OF_MESSAGES));
    } catch (Exception ignore) {
    }

    boolean deadLetterQueue = false;
    String deadLetterQueueAccountId = null;
    String deadLetterQueueName = null;
    int maxReceiveCount = 0;
    int deadLetterQueueMessageRetentionPeriod = 0;
    try {
      Ern deadLetterQueueErn = Ern.parse(receiveAttributes.get(Constants.DEAD_LETTER_TARGET_ARN));
      deadLetterQueueAccountId = deadLetterQueueErn.getAccount();
      deadLetterQueueName = deadLetterQueueErn.getResourceName();
      maxReceiveCount = Integer.parseInt(receiveAttributes.get(Constants.MAX_RECEIVE_COUNT));
      deadLetterQueueMessageRetentionPeriod = Integer.parseInt(receiveAttributes.get(Constants.MESSAGE_RETENTION_PERIOD));
      deadLetterQueue = true;
    } catch (Exception ignore) {
    }

    for (String partitionToken: randomPartitionTokens) {
      long nowSecs = SimpleQueueService.currentTimeSeconds();
      Map<UUID, ReceiveCountsAndExpirationTimestamp> receiveCountMap = Maps.newLinkedHashMap();
      Statement statement1 = new SimpleStatement(
        "SELECT message_id, receive_count, total_receive_count, expiration_timestamp, send_time_secs FROM maybe_visible_messages WHERE account_id = ? " +
          "AND queue_name = ? AND partition_token = ?",
        queue.getAccountId(),
        queue.getQueueName(),
        partitionToken
      );
      for (Row row: session.execute(statement1)) {
        receiveCountMap.put(
          row.getUUID("message_id"),
          new ReceiveCountsAndExpirationTimestamp(
            row.getInt("receive_count"),
            row.getInt("total_receive_count"),
            row.getTimestamp("expiration_timestamp"),
            row.getLong("send_time_secs")
          )
        );
      }
      Statement statement2 = new SimpleStatement(
        "SELECT message_id FROM invisible_messages WHERE account_id = ? " +
          "AND queue_name = ? AND partition_token = ?",
        queue.getAccountId(),
        queue.getQueueName(),
        partitionToken
      );
      for (Row row: session.execute(statement2)) {
        receiveCountMap.remove(row.getUUID("message_id"));
      }
      Statement statement3 = new SimpleStatement(
        "SELECT message_id FROM delayed_messages WHERE account_id = ? " +
          "AND queue_name = ? AND partition_token = ?",
        queue.getAccountId(),
        queue.getQueueName(),
        partitionToken
      );
      for (Row row: session.execute(statement3)) {
        receiveCountMap.remove(row.getUUID("message_id"));
      }

      for (UUID messageId: receiveCountMap.keySet()) {
        int receiveCount = receiveCountMap.get(messageId).getReceiveCount();
        int totalReceiveCount = receiveCountMap.get(messageId).getTotalReceiveCount();
        Date expirationTimestamp = receiveCountMap.get(messageId).getExpirationTimestamp();
        Long sendTimeSecs = receiveCountMap.get(messageId).getSentTimeSecs();
        if (deadLetterQueue && receiveCount >= maxReceiveCount) {
          Statement statement4 = new SimpleStatement(
            "SELECT message_json, send_time_secs FROM messages WHERE account_id = ? " +
              "AND queue_name = ? AND partition_token = ? AND message_id = ?",
            queue.getAccountId(),
            queue.getQueueName(),
            partitionToken,
            messageId
          );
          Iterator<Row> rowIter = session.execute(statement4).iterator();
          if (rowIter.hasNext()) {
            Row row = rowIter.next();

            BatchStatement batchStatement = getBatchStatementForDeleteMessage(queue.getKey(), partitionToken, messageId);
            String messageJson = row.getString("message_json");

            Statement statement5 = new SimpleStatement(
              "INSERT INTO messages (account_id, queue_name, partition_token, message_id, message_json, send_time_secs) " +
                "VALUES (?, ?, ?, ?, ?, ?) USING TTL ?",
              deadLetterQueueAccountId,
              deadLetterQueueName,
              partitionToken,
              messageId,
              messageJson,
              sendTimeSecs,
              deadLetterQueueMessageRetentionPeriod
            );

            batchStatement.add(statement5);

            Date deadLetterExpirationTimestamp = new Date((nowSecs + deadLetterQueueMessageRetentionPeriod) * 1000L);

            Statement statement6 = new SimpleStatement(
              "INSERT INTO maybe_visible_messages (account_id, queue_name, partition_token, message_id, receive_count, " +
                "total_receive_count, expiration_timestamp, send_time_secs) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) USING TTL ?",
              deadLetterQueueAccountId,
              deadLetterQueueName,
              partitionToken,
              messageId,
              0,
              totalReceiveCount,
              deadLetterExpirationTimestamp,
              sendTimeSecs,
              deadLetterQueueMessageRetentionPeriod
            );
            batchStatement.add(statement6);
            session.execute(batchStatement);
          }

          continue;
        }

        // once here, receive the message...
        Statement statement7 = new SimpleStatement(
          "SELECT message_json FROM messages WHERE account_id = ? " +
            "AND queue_name = ? AND partition_token = ? AND message_id = ?",
          queue.getAccountId(),
          queue.getQueueName(),
          partitionToken,
          messageId
        );
        Iterator<Row> rowIter = session.execute(statement7).iterator();
        if (rowIter.hasNext()) {
          BatchStatement batchStatement = new BatchStatement();
          Row row = rowIter.next();
          String messageJson = row.getString("message_json");
          Message message = MessageJsonHelper.jsonToMessage(messageJson);
          message.setMessageId(messageId.toString());
          int visibleTtl = (int) (expirationTimestamp.getTime() / 1000 - nowSecs);
          if (visibleTtl < 1) visibleTtl = 1;
          if (totalReceiveCount == 0) {
            message.getAttribute().add(new Attribute(Constants.APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, "" + nowSecs));
            messageJson = MessageJsonHelper.messageToJson(message);
            Statement statement8 = new SimpleStatement(
              "UPDATE messages USING TTL ? SET message_json = ? WHERE account_id = ? " +
                "AND queue_name = ? AND partition_token = ? AND message_id = ?",
              visibleTtl,
              messageJson,
              queue.getAccountId(),
              queue.getQueueName(),
              partitionToken,
              messageId
            );
            batchStatement.add(statement8);
          }

          int visibilityTimeout = queue.getVisibilityTimeout();
          if (receiveAttributes.containsKey(Constants.VISIBILITY_TIMEOUT)) {
            visibilityTimeout = Integer.parseInt(receiveAttributes.get(Constants.VISIBILITY_TIMEOUT));
          }

          receiveCount++;
          totalReceiveCount++;
          Statement statement9 = new SimpleStatement(
            "UPDATE maybe_visible_messages USING TTL ? SET receive_count = ?, total_receive_count = ? " +
              "WHERE account_id = ? AND queue_name = ? AND partition_token = ? AND message_id = ?",
            visibleTtl,
            receiveCount,
            totalReceiveCount,
            queue.getAccountId(),
            queue.getQueueName(),
            partitionToken,
            messageId
          );
          batchStatement.add(statement9);

          if (visibleTtl < visibilityTimeout) {
            visibilityTimeout = visibleTtl;
          }

          if (visibilityTimeout > 0) {
            Statement statement10 = new SimpleStatement(
              "INSERT INTO invisible_messages (account_id, queue_name, partition_token, message_id, receive_count, expiration_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?) USING TTL ? ",
              queue.getAccountId(),
              queue.getQueueName(),
              partitionToken,
              messageId,
              receiveCount,
              expirationTimestamp,
              visibilityTimeout
            );
            batchStatement.add(statement10);
          }
          session.execute(batchStatement);

          // Set the 'attributes' that are stored as first class fields
          message.getAttribute().add(new Attribute(Constants.APPROXIMATE_RECEIVE_COUNT, "" + totalReceiveCount));
          // send timestamp isn't updated but used in queries.  The attribute is in seconds though, so convert
          message.getAttribute().add(new Attribute(Constants.SENT_TIMESTAMP, "" + sendTimeSecs));
          message.setReceiptHandle(queue.getAccountId() + ":" + queue.getQueueName() + ":"
            + messageId.toString() + ":" + partitionToken + ":" + receiveCount);
          messages.add(message);
        }
        if (messages.size() >= maxNumMessages) break;
      }
      if (messages.size() >= maxNumMessages) break;
    }
    return messages;
  }

  @Override
  public void sendMessage(Queue queue, Message message, Map<String, String> sendAttributes) throws SimpleQueueException {
    Session session = CassandraSessionManager.getSession();
    BatchStatement batchStatement = new BatchStatement();

    int delaySeconds = queue.getDelaySeconds();
    if (sendAttributes.containsKey(Constants.DELAY_SECONDS)) {
      delaySeconds = Integer.parseInt(sendAttributes.get(Constants.DELAY_SECONDS));
    }
    String messageJson = MessageJsonHelper.messageToJson(message);

    String partitionToken = partitionTokens.get(random.nextInt(partitionTokens.size()));

    UUID messageId = UUID.fromString(message.getMessageId());

    long nowSecs = SimpleQueueService.currentTimeSeconds();

    Statement statement1 = new SimpleStatement(
      "INSERT INTO messages (account_id, queue_name, partition_token, message_id, message_json, send_time_secs) " +
        "VALUES (?, ?, ?, ?, ?, ?) USING TTL ?",
      queue.getAccountId(),
      queue.getQueueName(),
      partitionToken,
      messageId,
      messageJson,
      nowSecs,
      queue.getMessageRetentionPeriod());

    batchStatement.add(statement1);

    Date expirationTimestamp = new Date((nowSecs + queue.getMessageRetentionPeriod()) * 1000L);

    Statement statement2 = new SimpleStatement(
      "INSERT INTO maybe_visible_messages (account_id, queue_name, partition_token, message_id, receive_count, " +
        "total_receive_count, expiration_timestamp, send_time_secs) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) USING TTL ?",
      queue.getAccountId(),
      queue.getQueueName(),
      partitionToken,
      messageId,
      0,
      0,
      expirationTimestamp,
      nowSecs,
      queue.getMessageRetentionPeriod());

    batchStatement.add(statement2);

    if (delaySeconds > 0) {
      Statement statement3 = new SimpleStatement(
        "INSERT INTO delayed_messages (account_id, queue_name, partition_token, message_id) " +
          "VALUES (?, ?, ?, ?) USING TTL ?",
        queue.getAccountId(),
        queue.getQueueName(),
        partitionToken,
        messageId,
        delaySeconds);
      batchStatement.add(statement3);
    }
    session.execute(batchStatement);
  }

  @Override
  public boolean deleteMessage(Queue.Key queueKey, String receiptHandle) throws SimpleQueueException {
    boolean found = false;
    // receipt handle (currently) looks like accountId:queueName:message-id:partition-token:receive-count
    StringTokenizer stok = new StringTokenizer(receiptHandle,":");
    if (stok.countTokens() != 5) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    String receiptHandleAccountId = stok.nextToken();
    String receiptHandleQueueName = stok.nextToken();
    UUID messageId = UUID.fromString(stok.nextToken());
    String partitionToken = stok.nextToken();
    if (!partitionTokens.contains(partitionToken)) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \"" + receiptHandle + "\" is not a valid for this queue.");
    }
    int receiveCount = 0;
    try {
      receiveCount = Integer.parseInt(stok.nextToken());
    } catch (NumberFormatException e) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    if (!receiptHandleAccountId.equals(queueKey.getAccountId()) || !receiptHandleQueueName.equals(queueKey.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }
    Session session = CassandraSessionManager.getSession();
    Statement statement = new SimpleStatement(
      "SELECT receive_count FROM maybe_visible_messages WHERE account_id = ? AND queue_name = ? AND " +
        "partition_token = ? AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    for (Row row: session.execute(statement)) {
      if (row.getInt("receive_count") == receiveCount) {
        found = true;
        break;
      }
    }
    if (found) {
      BatchStatement batchStatement = getBatchStatementForDeleteMessage(queueKey, partitionToken, messageId);
      session.execute(batchStatement);
    }
    return found;
  }

  private BatchStatement getBatchStatementForDeleteMessage(Queue.Key queueKey, String partitionToken, UUID messageId) {
    BatchStatement batchStatement = new BatchStatement();
    Statement statement1 = new SimpleStatement(
      "DELETE FROM messages WHERE account_id = ? AND queue_name = ? AND partition_token = ? AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    batchStatement.add(statement1);
    Statement statement2 = new SimpleStatement(
      "DELETE FROM maybe_visible_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?  AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    batchStatement.add(statement2);
    Statement statement3 = new SimpleStatement(
      "DELETE FROM delayed_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?  AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    batchStatement.add(statement3);
    Statement statement4 = new SimpleStatement(
      "DELETE FROM invisible_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?  AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    batchStatement.add(statement4);
    return batchStatement;
  }

  private BatchStatement getBatchStatementForDeleteAllMessages(Queue.Key queueKey, String partitionToken) {
    BatchStatement batchStatement = new BatchStatement();
    Statement statement1 = new SimpleStatement(
      "DELETE FROM messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken
    );
    batchStatement.add(statement1);
    Statement statement2 = new SimpleStatement(
      "DELETE FROM maybe_visible_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken
    );
    batchStatement.add(statement2);
    Statement statement3 = new SimpleStatement(
      "DELETE FROM delayed_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken
    );
    batchStatement.add(statement3);
    Statement statement4 = new SimpleStatement(
      "DELETE FROM invisible_messages WHERE account_id = ? AND queue_name = ? AND partition_token = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken
    );
    batchStatement.add(statement4);
    return batchStatement;
  }


  @Override
  public void deleteAllMessages(Queue.Key queueKey) {
    Session session = CassandraSessionManager.getSession();
    for (String partitionToken: partitionTokens) {
      BatchStatement batchStatement = getBatchStatementForDeleteAllMessages(queueKey, partitionToken);
      session.execute(batchStatement);
    }
  }

  @Override
  public Map<String, String> getApproximateMessageCounts(Queue.Key queueKey) {
    Map<String, String> result = Maps.newHashMap();
    long totalDelayedMessages = 0;
    long totalInvisibleMessages = 0;
    long totalVisibleMessages = 0;
    long totalMessages = 0;
    Session session = CassandraSessionManager.getSession();
    for (String partitionToken : partitionTokens) {
      Statement statement1 = new SimpleStatement(
        "SELECT COUNT(*) FROM maybe_visible_messages WHERE account_id = ? AND queue_name = ? " +
          "AND partition_token = ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken
      );
      Iterator<Row> rowIter1 = session.execute(statement1).iterator();
      if (rowIter1.hasNext()) {
        Row row1 = rowIter1.next();
        totalMessages += row1.getLong(0);
      }
      Statement statement2 = new SimpleStatement(
        "SELECT COUNT(*) FROM invisible_messages WHERE account_id = ? AND queue_name = ? " +
          "AND partition_token = ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken
      );
      Iterator<Row> rowIter2 = session.execute(statement2).iterator();
      if (rowIter2.hasNext()) {
        Row row2 = rowIter2.next();
        totalInvisibleMessages += row2.getLong(0);
      }
      Statement statement3 = new SimpleStatement(
        "SELECT COUNT(*) FROM delayed_messages WHERE account_id = ? AND queue_name = ? " +
          "AND partition_token = ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken
      );
      Iterator<Row> rowIter3 = session.execute(statement3).iterator();
      if (rowIter3.hasNext()) {
        Row row3 = rowIter3.next();
        totalDelayedMessages += row3.getLong(0);
      }
    }
    totalVisibleMessages = totalMessages - totalDelayedMessages - totalInvisibleMessages;
    result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED, String.valueOf(totalDelayedMessages));
    result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, String.valueOf(totalInvisibleMessages));
    result.put(Constants.APPROXIMATE_NUMBER_OF_MESSAGES, String.valueOf(totalVisibleMessages));
    return result;
  }

  @Override
  public void changeMessageVisibility(Queue.Key queueKey, String receiptHandle, Integer visibilityTimeout) throws SimpleQueueException {
    boolean found = false;
    // receipt handle (currently) looks like accountId:queueName:message-id:partition-token:receive-count
    StringTokenizer stok = new StringTokenizer(receiptHandle,":");
    if (stok.countTokens() != 5) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    String receiptHandleAccountId = stok.nextToken();
    String receiptHandleQueueName = stok.nextToken();
    UUID messageId = UUID.fromString(stok.nextToken());
    String partitionToken = stok.nextToken();
    if (!partitionTokens.contains(partitionToken)) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \"" + receiptHandle + "\" is not a valid for this queue.");
    }
    int receiveCount = 0;
    try {
      receiveCount = Integer.parseInt(stok.nextToken());
    } catch (NumberFormatException e) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid receipt handle.");
    }
    if (!receiptHandleAccountId.equals(queueKey.getAccountId()) || !receiptHandleQueueName.equals(queueKey.getQueueName())) {
      throw new ReceiptHandleIsInvalidException("The input receipt handle \""+receiptHandle+"\" is not a valid for this queue.");
    }
    Session session = CassandraSessionManager.getSession();
    Statement statement1 = new SimpleStatement(
      "SELECT receive_count, expiration_timestamp FROM maybe_visible_messages WHERE account_id = ? " +
        "AND queue_name = ? AND partition_token = ? AND message_id = ?",
      queueKey.getAccountId(),
      queueKey.getQueueName(),
      partitionToken,
      messageId
    );
    Date expirationTimestamp = null;
    for (Row row: session.execute(statement1)) {
      if (row.getInt("receive_count") == receiveCount) {
        expirationTimestamp = row.getTimestamp("expiration_timestamp");
        found = true;
        break;
      }
    }
    if (!found) {
      throw new InvalidParameterValueException("Value " + receiptHandle + " for parameter ReceiptHandle is invalid. Reason: Message does not exist or is not available for visibility timeout change.");
    }
    Statement statement2;
    int maxVisibilityTimeout = (int) ((expirationTimestamp.getTime() - System.currentTimeMillis()) / 1000);
    if (maxVisibilityTimeout < visibilityTimeout) {
      visibilityTimeout = maxVisibilityTimeout;
    }
    if (visibilityTimeout <= 0) {
      statement2 = new SimpleStatement(
        "DELETE FROM invisible_messages WHERE account_id = ? AND queue_name = ? AND " +
          "partition_token = ? AND message_id = ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken,
        messageId
      );
    } else {
      statement2 = new SimpleStatement(
        "INSERT INTO invisible_messages (account_id, queue_name, partition_token, message_id, " +
          "receive_count, expiration_timestamp) VALUES (?, ?, ?, ?, ?, ?) USING TTL ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken,
        messageId,
        receiveCount,
        expirationTimestamp,
        visibilityTimeout
      );
    }
    session.execute(statement2);
  }

  @Override
  public Long getApproximateAgeOfOldestMessage(Queue.Key queueKey) {
    Session session = CassandraSessionManager.getSession();
    Long oldestTimestamp = null;
    for (String partitionToken : partitionTokens) {
      Statement statement = new SimpleStatement(
        "SELECT MIN(send_time_secs) FROM maybe_visible_messages WHERE account_id = ? AND queue_name = ? " +
          "AND partition_token = ?",
        queueKey.getAccountId(),
        queueKey.getQueueName(),
        partitionToken
      );
      Iterator<Row> rowIter = session.execute(statement).iterator();
      if (rowIter.hasNext()) {
        Row row = rowIter.next();
        Long currentTimestamp = row.getLong(0);
        if (currentTimestamp != null && currentTimestamp != 0) {
          if (oldestTimestamp == null) {
            oldestTimestamp = currentTimestamp;
          } else if (oldestTimestamp > currentTimestamp) {
            oldestTimestamp = currentTimestamp;
          }
        }
      }
    }
    if (oldestTimestamp == null || oldestTimestamp == 0L) return 0L;
    return SimpleQueueService.currentTimeSeconds() - oldestTimestamp;
  }
}
