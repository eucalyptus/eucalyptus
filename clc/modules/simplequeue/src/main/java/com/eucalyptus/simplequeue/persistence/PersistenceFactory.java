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

import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.common.policy.SimpleQueueResourceName;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.persistence.cassandra.CassandraMessagePersistence;
import com.eucalyptus.simplequeue.persistence.cassandra.CassandraQueuePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlMessagePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlQueuePersistence;

/**
 * Created by ethomas on 9/7/16.
 */
public class PersistenceFactory {
  private static QueuePersistence postgresqlQueuePersistence = new PostgresqlQueuePersistence();
  private static QueuePersistence cassandraQueuePersistence = new CassandraQueuePersistence();
  private static MessagePersistence postgresqlMessagePersistence = new PostgresqlMessagePersistence();
  private static MessagePersistence cassandraMessagePersistence = new CassandraMessagePersistence();
  public static QueuePersistence getQueuePersistence() {
    return "cassandra".equalsIgnoreCase(SimpleQueueProperties.DB_TO_USE) ? cassandraQueuePersistence: postgresqlQueuePersistence;
  }
  public static MessagePersistence getMessagePersistence() {
    return "cassandra".equalsIgnoreCase(SimpleQueueProperties.DB_TO_USE) ? cassandraMessagePersistence: postgresqlMessagePersistence;
  }

  public static boolean queueHasMessages(SimpleQueueResourceName ern) {
    // TODO: make a new persistence method somewhere
    Queue queue = PersistenceFactory.getQueuePersistence().lookupQueue(ern.getAccount(), ern.getResourceName());
    if (queue == null) {
      return false;
    }
    return Long.parseLong(PersistenceFactory.getMessagePersistence().getApproximateMessageCounts(queue.getKey()).get(Constants.APPROXIMATE_NUMBER_OF_MESSAGES)) > 0;
  }
}
