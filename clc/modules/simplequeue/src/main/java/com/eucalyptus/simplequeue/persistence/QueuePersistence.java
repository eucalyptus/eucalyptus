package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.exceptions.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ethomas on 9/7/16.
 */
public interface QueuePersistence {
  Queue lookupQueue(String accountId, String queueName);
  Queue createQueue(String accountId, String queueName, Map<String, String> attributes) throws QueueAlreadyExistsException;
  Collection<Queue> listQueuesByPrefix(String accountId, String queueNamePrefix);
  void deleteQueue(String accountId, String queueName) throws QueueDoesNotExistException;
}
