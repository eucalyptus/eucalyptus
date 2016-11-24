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
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.exceptions.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;
import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ethomas on 9/7/16.
 */
public interface QueuePersistence {
  Queue lookupQueue(String accountId, String queueName);
  Queue createQueue(String accountId, String queueName, Map<String, String> attributes) throws QueueAlreadyExistsException;
  Collection<Queue.Key> listQueues(String accountId, String queueNamePrefix);
  Collection<Queue.Key> listDeadLetterSourceQueues(String accountId, String deadLetterTargetArn);
  Queue updateQueueAttributes(String accountId, String queueName, Map<String, String> attributes) throws QueueDoesNotExistException;
  void deleteQueue(String accountId, String queueName) throws QueueDoesNotExistException;
  Collection<Queue.Key> listActiveQueues(String partitionToken);

  Collection<String> getPartitionTokens();
}
