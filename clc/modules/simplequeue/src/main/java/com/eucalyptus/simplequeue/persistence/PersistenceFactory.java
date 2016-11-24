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

import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.persistence.cassandra.CassandraQueuePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlMessagePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlQueuePersistence;

/**
 * Created by ethomas on 9/7/16.
 */
public class PersistenceFactory {
  private static QueuePersistence postgresQueuePersistence = new PostgresqlQueuePersistence();
  private static QueuePersistence cassandraQueuePersistence = new CassandraQueuePersistence();
  private static MessagePersistence messagePersistence = new PostgresqlMessagePersistence();
  public static QueuePersistence getQueuePersistence() {
    return ("cassandra".equalsIgnoreCase(SimpleQueueProperties.DB_TO_USE)) ? cassandraQueuePersistence: postgresQueuePersistence;
  }
  public static MessagePersistence getMessagePersistence() {
    return messagePersistence;
  }

}
