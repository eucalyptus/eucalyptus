/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence;

import java.util.Map;
import java.util.function.Function;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.cassandra.common.CassandraPersistence;
import com.eucalyptus.component.Topology;
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
      return Topology.isEnabled( Cassandra.class ) ?
          "euca-cassandra" :
          defaultPersistence;
    }
    return dbToUse;
  }
}
