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

import java.util.Map;
import java.util.function.Function;
import com.eucalyptus.cassandra.common.CassandraPersistence;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.common.policy.SimpleQueueResourceName;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.persistence.cassandra.CassandraMessagePersistence;
import com.eucalyptus.simplequeue.persistence.cassandra.CassandraQueuePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlMessagePersistence;
import com.eucalyptus.simplequeue.persistence.postgresql.PostgresqlQueuePersistence;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import javaslang.Tuple;
import javaslang.Tuple2;

/**
 * Created by ethomas on 9/7/16.
 */
public class PersistenceFactory {
  private static final String defaultPersistence = "postgres";
  private static final Map<String, Tuple2<QueuePersistence,MessagePersistence>> persistenceMap =
      ImmutableMap.<String,Tuple2<QueuePersistence,MessagePersistence>>builder( )
      .put( "cassandra", Tuple.of( CassandraQueuePersistence.external( ), CassandraMessagePersistence.external( ) ) )
      .put( "euca-cassandra", Tuple.of( CassandraQueuePersistence.internal( ), CassandraMessagePersistence.internal( ) ) )
      .put( defaultPersistence, Tuple.of( new PostgresqlQueuePersistence( ), new PostgresqlMessagePersistence( ) ) )
      .build( );

  public static QueuePersistence getQueuePersistence( ) {
    return persistence( Tuple2::_1 );
  }
  public static MessagePersistence getMessagePersistence( ) {
    return persistence( Tuple2::_2 );
  }

  public static boolean queueHasMessages(SimpleQueueResourceName ern) {
    // TODO: make a new persistence method somewhere
    Queue queue = getQueuePersistence().lookupQueue(ern.getAccount(), ern.getResourceName());
    if (queue == null) {
      return false;
    }
    return Long.parseLong(getMessagePersistence().getApproximateMessageCounts(queue.getKey()).get(Constants.APPROXIMATE_NUMBER_OF_MESSAGES)) > 0;
  }

  private static <P> P persistence( Function<Tuple2<QueuePersistence,MessagePersistence>,P> extractor ) {
    return extractor.apply( persistenceMap.getOrDefault(
        resolveAuto( MoreObjects.firstNonNull( SimpleQueueProperties.DB_TO_USE, defaultPersistence ) ),
        persistenceMap.get( defaultPersistence ) ) );
  }

  private static String resolveAuto( final String dbToUse ) {
    if ( "auto".equals( dbToUse ) ) {
      return CassandraPersistence.isAvailable( ) ?
          "euca-cassandra" :
          defaultPersistence;
    }
    return dbToUse;
  }
}
