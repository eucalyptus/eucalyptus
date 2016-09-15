package com.eucalyptus.simplequeue.persistence.postgresql;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simplequeue.InvalidParameterValueException;
import com.eucalyptus.simplequeue.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.QueueDoesNotExistException;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simplequeue.persistence.QueuePersistence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

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
      Criteria criteria = Entities.createCriteria(QueueEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName));
      QueueEntity queueEntity = (QueueEntity) criteria.uniqueResult();
      if (queueEntity == null) {
        return null;
      } else {
        return queueFromQueueEntity(queueEntity);
      }
    }
  }

  @Override
  public Queue createQueue(String accountId, String queueName, Map<String, String> attributes) throws QueueAlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Criteria criteria = Entities.createCriteria(QueueEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName));
      QueueEntity queueEntity = (QueueEntity) criteria.uniqueResult();
      if (queueEntity == null) {
        queueEntity = new QueueEntity();
        queueEntity.setAccountId(accountId);
        queueEntity.setQueueName(queueName);
        queueEntity.setAttributes(convertAttributeMapToJson(attributes));
        Entities.persist(queueEntity);
        db.commit( );
        return queueFromQueueEntity(queueEntity);
      } else {
        throw new QueueAlreadyExistsException("Queue " + queueName + " already exists");
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
      Criteria criteria = Entities.createCriteria(QueueEntity.class)
        .add(Restrictions.eq("accountId", accountId));
      if (queueNamePrefix != null) {
        criteria = criteria.add(Restrictions.like("queueName", queueNamePrefix + "%"));
      }
      List<QueueEntity> queueEntities = (List<QueueEntity>) criteria.list();
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
  public void deleteQueue(String accountId, String queueName) throws QueueDoesNotExistException {
    try ( TransactionResource db =
            Entities.transactionFor(QueueEntity.class) ) {
      Criteria criteria = Entities.createCriteria(QueueEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("queueName", queueName));
      QueueEntity queueEntity = (QueueEntity) criteria.uniqueResult();
      if (queueEntity == null) {
        throw new QueueDoesNotExistException("The specified queue does not exist.");
      } else {
        Entities.delete(queueEntity);
      }
      db.commit();
    }

  }
}
