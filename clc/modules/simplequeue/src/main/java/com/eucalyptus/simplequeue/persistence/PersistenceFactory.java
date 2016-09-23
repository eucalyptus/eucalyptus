package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlMessagePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlQueuePersistence;

/**
 * Created by ethomas on 9/7/16.
 */
public class PersistenceFactory {
  private static QueuePersistence queuePersistence = new PostgresqlQueuePersistence();
  private static MessagePersistence messagePersistence = new PostgresqlMessagePersistence();

  public static QueuePersistence getQueuePersistence() {
    return queuePersistence;
  }

  public static MessagePersistence getMessagePersistence() {
    return messagePersistence;
  }
}
