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
import com.eucalyptus.simplequeue.exceptions.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;
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
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 9/7/16.
 */
public class PostgresqlQueuePersistence implements QueuePersistence {
  @Override
  public Queue lookupQueue(String accountId, String queueName) {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Optional<QueueEntity> queueEntityOptional = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId)
        .whereEqual(QueueEntity_.queueName, queueName)
        .uniqueResultOption();
      if (queueEntityOptional.isPresent()) {
        return queueFromQueueEntity(queueEntityOptional.get());
      } else {
        return null;
      }
    }
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
  public Collection<Queue> listQueuesByPrefix(String accountId, String queueNamePrefix) {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Entities.EntityCriteriaQuery<QueueEntity, QueueEntity> queryCriteria = Entities.criteriaQuery(QueueEntity.class)
        .whereEqual(QueueEntity_.accountId, accountId);
      if (queueNamePrefix != null) {
        queryCriteria = queryCriteria.where(
          Entities.restriction(QueueEntity.class).like(QueueEntity_.queueName, queueNamePrefix + "%")
        );
      }
      List<QueueEntity> queueEntities = queryCriteria.list();
      List<Queue> queues = Lists.newArrayList();
      if (queueEntities != null) {
        for (QueueEntity queueEntity: queueEntities) {
          queues.add(queueFromQueueEntity(queueEntity));
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
}
