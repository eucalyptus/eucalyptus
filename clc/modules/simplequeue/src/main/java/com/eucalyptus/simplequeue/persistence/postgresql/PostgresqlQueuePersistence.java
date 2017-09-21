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
    queue.setUniqueIdPerVersion(queueEntity.getNaturalId() + "/" + queueEntity.getVersion());
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
  public Collection<Queue.Key> listQueues(String accountId, String queueNamePrefix) {
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
      List<Queue.Key> queueKeys = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity: queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          queueKeys.add(queue.getKey());
        }
      }
      return queueKeys;
    }
  }

  @Override
  public Collection<Queue.Key> listDeadLetterSourceQueues(String accountId, String deadLetterTargetArn) {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId);
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue.Key> queueKeys = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity: queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          if (Objects.equals(deadLetterTargetArn, queue.getDeadLetterTargetArn())) {
            queueKeys.add(queue.getKey());
          }
        }
      }
      return queueKeys;
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
      return Collections.emptyList( );
    }
  }

  @Override
  public long countQueues(String accountNumber) {
    try (TransactionResource db =
           Entities.transactionFor(QueueEntity.class)) {
      return Entities.count(QueueEntity.class).whereEqual(QueueEntity_.accountId, accountNumber).uniqueResult();
    }
  }

  @Override
  public Collection<Queue.Key> listActiveQueues(String partitionToken) {
    try (TransactionResource db =
           Entities.transactionFor(QueueEntity.class)) {
      long nowSecs = SimpleQueueService.currentTimeSeconds();
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.partitionToken, partitionToken)
        .where(Entities.restriction(QueueEntity.class).ge(QueueEntity_.lastLookupTimestampSecs, nowSecs - SimpleQueueProperties.ACTIVE_QUEUE_TIME_SECS));
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue.Key> queueKeys = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity : queueEntities) {
          Queue queue = queueFromQueueEntity(queueEntity);
          queueKeys.add(queue.getKey());
        }
      }
      return queueKeys;
    }
  }
}
