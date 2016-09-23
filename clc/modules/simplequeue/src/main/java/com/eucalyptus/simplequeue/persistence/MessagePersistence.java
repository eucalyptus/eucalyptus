package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.exceptions.InternalFailureException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;

import java.util.Collection;

/**
 * Created by ethomas on 9/16/16.
 */
public interface MessagePersistence {
  Collection<Message> receiveMessages(String accountId, String queueName) throws SimpleQueueException;

  void sendMessage(String accountId, String queueName, Message message) throws SimpleQueueException;

  void deleteMessage(String accountId, String queueName, String receiptHandle) throws SimpleQueueException;

  void deleteAllMessages(String accountId, String queueName);

}
