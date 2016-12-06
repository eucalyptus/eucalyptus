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
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.SimpleQueueService;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.exceptions.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simplequeue.persistence.QueuePersistence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ethomas on 9/7/16.
 */
public class PostgresqlQueuePersistence implements QueuePersistence {

  static Random random = new Random();

  private static final int NUM_PARTITIONS = 25;

  private static final Collection<String> partitionTokens = IntStream.range(0, NUM_PARTITIONS).boxed().map(String::valueOf).collect(Collectors.toSet());

  @Override
  public Queue lookupQueue(String accountId, String queueName) {
    Queue queue = null;
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Optional<QueueEntity> queueEntityOptional = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId)
        .whereEqual(QueueEntity_.queueName, queueName)
        .uniqueResultOption();
      if (queueEntityOptional.isPresent()) {
        queueEntityOptional.get().setLastLookupTimestampSecs(SimpleQueueService.currentTimeSeconds());
        queue =  queueFromQueueEntity(queueEntityOptional.get());
      }
      db.commit();
    }
    return queue;
  }

  @Override
  public Queue createQueue(String accountId, String queueName, Map<String, String> attributes) throws QueueAlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Optional<QueueEntity> queueEntityOptional = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId)
        .whereEqual(QueueEntity_.queueName, queueName)
        .uniqueResultOption();
      if (queueEntityOptional.isPresent()) {
        throw new QueueAlreadyExistsException("Queue " + queueName + " already exists");
      } else {
        QueueEntity queueEntity = new QueueEntity();
        queueEntity.setAccountId(accountId);
        queueEntity.setQueueName(queueName);
        queueEntity.setAttributes(convertAttributeMapToJson(attributes));
        queueEntity.setLastLookupTimestampSecs(SimpleQueueService.currentTimeSeconds());
        queueEntity.setPartitionToken(String.valueOf(random.nextInt(NUM_PARTITIONS)));
        Entities.persist(queueEntity);
        db.commit( );
        return queueFromQueueEntity(queueEntity);
      }
    }
  }

  private Queue queueFromQueueEntity(QueueEntity queueEntity) {
    Queue queue = new Queue();
    queue.setAccountId(queueEntity.getAccountId());
    queue.setQueueName(queueEntity.getQueueName());
    queue.getAttributes().putAll(convertJsonToAttributeMap(queueEntity.getAttributes()));
    queue.setUniqueId(queueEntity.getNaturalId());
    queue.setVersion(queueEntity.getVersion());
    return queue;
  }

  private Map<String, String> convertJsonToAttributeMap(String attributes) {
    Map<String, String> attributeMap = Maps.newTreeMap();
    try {
      JsonNode jsonNode = new ObjectMapper().readTree(attributes);
      for (String key: Lists.newArrayList(jsonNode.fieldNames())) {
        attributeMap.put(key, jsonNode.get(key).textValue());
      }
    } catch (IOException e) {
      // TODO: log
    }
    return attributeMap;
  }

  private String convertAttributeMapToJson(Map<String, String> attributes) {
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    for (String key: attributes.keySet()) {
      objectNode.put(key, attributes.get(key));
    }
    return objectNode.toString();
  }


  @Override
  public Collection<Queue> listQueues(String accountId, String queueNamePrefix) {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class);
      if (accountId != null) {
        queryCriteria = queryCriteria.whereEqual(QueueEntity_.accountId, accountId);
      }
      if (queueNamePrefix != null) {
        queryCriteria = queryCriteria.where(
          Entities.restriction(QueueEntity.class).like(QueueEntity_.queueName, queueNamePrefix + "%")
        );
      }
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue> queues = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity: queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          queues.add(queue);
        }
      }
      return queues;
    }
  }

  @Override
  public Collection<Queue> listDeadLetterSourceQueues(String accountId, String deadLetterTargetArn) {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId);
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue> queues = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity: queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          try {
            if (queue.getRedrivePolicy() != null && queue.getRedrivePolicy().isObject() &&
              queue.getRedrivePolicy().has(Constants.DEAD_LETTER_TARGET_ARN) &&
              queue.getRedrivePolicy().get(Constants.DEAD_LETTER_TARGET_ARN).isTextual() &&
              Objects.equals(deadLetterTargetArn, queue.getRedrivePolicy().get(Constants.DEAD_LETTER_TARGET_ARN).textValue())) {
              queues.add(queue);
            }
          } catch (SimpleQueueException ignore) {
            // redrive policy doesn't match, ignore it
          }
        }
      }
      return queues;
    }
  }

  @Override
  public Queue updateQueueAttributes(String accountId, String queueName, Map<String, String> attributes) throws QueueDoesNotExistException {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Optional<QueueEntity> queueEntityOptional = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId)
        .whereEqual(QueueEntity_.queueName, queueName)
        .uniqueResultOption();
      if (queueEntityOptional.isPresent()) {
        QueueEntity queueEntity = queueEntityOptional.get();
        queueEntity.setAttributes(convertAttributeMapToJson(attributes));
        db.commit( );
        return queueFromQueueEntity(queueEntity);
      } else {
        throw new QueueDoesNotExistException("The specified queue does not exist.");
      }
    }
  }

  @Override
  public void deleteQueue(String accountId, String queueName) throws QueueDoesNotExistException {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Optional<QueueEntity> queueEntityOptional = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId)
        .whereEqual(QueueEntity_.queueName, queueName)
        .uniqueResultOption();
      if (queueEntityOptional.isPresent()) {
        Entities.delete(queueEntityOptional.get());
      } else {
        throw new QueueDoesNotExistException("The specified queue does not exist.");
      }
      db.commit();
    }
  }

  @Override
  public Collection<String> getPartitionTokens() {
    if (SimpleQueueProperties.ENABLE_METRICS_COLLECTION) {
      return partitionTokens;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  @Override
  public Collection<Queue> listActiveQueues(String partitionToken) {
    try (TransactionResource db =
           Entities.transactionFor(QueueEntity.class)) {
      long nowSecs = SimpleQueueService.currentTimeSeconds();
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.partitionToken, partitionToken)
        .where(Entities.restriction(QueueEntity.class).ge(QueueEntity_.lastLookupTimestampSecs, nowSecs - SimpleQueueProperties.ACTIVE_QUEUE_TIME_SECS));
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue> queues = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity : queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          queues.add(queue);
        }
      }
      return queues;
    }
  }
}
