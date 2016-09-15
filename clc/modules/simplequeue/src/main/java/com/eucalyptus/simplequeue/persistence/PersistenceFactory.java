package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlQueuePersistence;

/**
 * Created by ethomas on 9/7/16.
 */
public class PersistenceFactory {
  private static QueuePersistence queuePersistence = new PostgresqlQueuePersistence();

  public static QueuePersistence getQueuePersistence() {
    return queuePersistence;
  }
}
