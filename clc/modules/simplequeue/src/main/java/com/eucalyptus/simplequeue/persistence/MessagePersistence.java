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

import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ethomas on 9/16/16.
 */
public interface MessagePersistence {

  Collection<Message> receiveMessages(Queue queue, Map<String, String> receiveAttributes) throws SimpleQueueException;

  void sendMessage(Queue queue, Message message, Map<String, String> sendAttributes) throws SimpleQueueException;

  boolean deleteMessage(Queue.Key queueKey, String receiptHandle) throws SimpleQueueException;

  void deleteAllMessages(Queue.Key queueKey);

  Map<String, String> getApproximateMessageCounts(Queue.Key queueKey);

  void changeMessageVisibility(Queue.Key queueKey, String receiptHandle, Integer visibilityTimeout) throws SimpleQueueException;

  Long getApproximateAgeOfOldestMessage(Queue.Key queueKey);

}
