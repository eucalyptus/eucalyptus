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
 *
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence.cassandra;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.utils.UUIDs;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.exceptions.QueueAlreadyExistsException;
import com.eucalyptus.simplequeue.exceptions.QueueDoesNotExistException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simplequeue.persistence.QueuePersistence;
import com.eucalyptus.util.ThrowingFunction;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ethomas on 11/22/16.
 */
public class CassandraQueuePersistence implements QueuePersistence {

  static Random random = new Random();

  private static final int NUM_PARTITIONS = 25;

  private static final Collection<String> partitionTokens = IntStream.range(0, NUM_PARTITIONS).boxed().map(String::valueOf).collect(Collectors.toSet());

  private final CassandraSessionManager.SessionProvider sessionProvider;

  public CassandraQueuePersistence( final CassandraSessionManager.SessionProvider sessionProvider ) {
    this.sessionProvider = sessionProvider;
  }

  public static CassandraQueuePersistence external( ) {
    return new CassandraQueuePersistence( CassandraSessionManager.externalProvider( ) );
  }

  public static CassandraQueuePersistence internal( ) {
    return new CassandraQueuePersistence( CassandraSessionManager.internalProvider( ) );
  }

  @Override
  public Queue lookupQueue(String accountId, String queueName) {
    return doWithSession( session -> {
      Statement statement1 = new SimpleStatement(
          "SELECT unique_id_per_version, attributes, partition_token FROM queues WHERE account_id=? AND queue_name = ?",
          accountId,
          queueName
      );
      Iterator<Row> rowIter = session.execute( statement1 ).iterator( );
      if ( !rowIter.hasNext( ) ) {
        return null;
      }
      Row row = rowIter.next( );
      String partitionToken = row.getString( "partition_token" );
      Queue queue = new Queue( );
      queue.setAccountId( accountId );
      queue.setQueueName( queueName );
      queue.setUniqueIdPerVersion( row.getUUID( "unique_id_per_version" ).toString( ) );
      queue.setAttributes( row.getMap( "attributes", String.class, String.class ) );
      Statement statement2 = new SimpleStatement(
          "UPDATE queues_by_partition SET last_lookup = ? WHERE partition_token = ? AND account_id=? AND queue_name = ?",
          new Date( ),
          partitionToken,
          accountId,
          queueName
      );
      session.execute( statement2 );
      return queue;
    } );
  }

  @Override
  public Queue createQueue(String accountId, String queueName, Map<String, String> attributes) throws QueueAlreadyExistsException {
    return doThrowsWithSession( session -> {
      if ( lookupQueue( accountId, queueName ) != null ) {
        throw new QueueAlreadyExistsException( "Queue " + queueName + " already exists" );
      }
      String partitionToken = String.valueOf( random.nextInt( NUM_PARTITIONS ) );
      UUID uniqueIdPerVersion = UUIDs.timeBased( );
      BatchStatement batchStatement = new BatchStatement( );
      Statement statement1 = new SimpleStatement(
          "INSERT INTO queues (account_id, queue_name, unique_id_per_version, attributes, partition_token) VALUES (?, ?, ?, ?, ?)",
          accountId,
          queueName,
          uniqueIdPerVersion,
          attributes,
          partitionToken
      );
      batchStatement.add( statement1 );
      Statement statement2 = new SimpleStatement(
          "INSERT INTO queues_by_partition (partition_token, account_id, queue_name, last_lookup) VALUES (?, ?, ?, ?)",
          partitionToken,
          accountId,
          queueName,
          new Date( )
      );
      session.execute( statement2 );
      batchStatement.add( statement2 );
      Queue queue = new Queue( );
      queue.setAccountId( accountId );
      queue.setQueueName( queueName );
      queue.setUniqueIdPerVersion( uniqueIdPerVersion.toString( ) );
      queue.setAttributes( attributes );
      String deadLetterTargetArn = queue.getDeadLetterTargetArn( );
      if ( deadLetterTargetArn != null ) {
        Statement statement3 = new SimpleStatement(
            "INSERT INTO queues_by_source_queue (source_queue_arn, account_id, queue_name) VALUES (?, ?, ?)",
            deadLetterTargetArn,
            accountId,
            queueName
        );
        batchStatement.add( statement3 );
      }
      session.execute( batchStatement );
      return queue;
    } );
  }


  @Override
  public Collection<Queue.Key> listQueues(String accountId, String queueNamePrefix) {
    return doWithSession( session -> {
      Statement statement;
      if ( queueNamePrefix == null && accountId == null ) {
        statement = new SimpleStatement(
            "SELECT queue_name FROM queues"
        );
      } else if ( queueNamePrefix == null ) {
        statement = new SimpleStatement(
            "SELECT queue_name FROM queues WHERE account_id = ?",
            accountId
        );
      } else {
        statement = new SimpleStatement(
            "SELECT queue_name FROM queues WHERE account_id = ? AND queue_name >= ? AND queue_name < ?",
            accountId,
            queueNamePrefix,
            incrementString( queueNamePrefix )
        );
      }
      List<Queue.Key> queueKeys = Lists.newArrayList( );
      for ( Row row : session.execute( statement ) ) {
        queueKeys.add( new Queue.Key( accountId, row.getString( "queue_name" ) ) );
      }
      return queueKeys;
    } );
  }

  private String incrementString(String queueNamePrefix) {
    char[] queueNamePrefixChars = queueNamePrefix.toCharArray();
    // queue names can not have an FFFF char, so we're ok here.
    queueNamePrefixChars[queueNamePrefixChars.length-1]++;
    return new String(queueNamePrefixChars);
  }

  @Override
  public Collection<Queue.Key> listDeadLetterSourceQueues(String accountId, String deadLetterTargetArn) {
    return doWithSession( session -> {
      Statement statement = new SimpleStatement(
          "SELECT queue_name, attributes FROM queues WHERE account_id = ?",
          accountId
      );
      List<Queue.Key> queueKeys = Lists.newArrayList( );
      for ( Row row : session.execute( statement ) ) {
        Queue queue = new Queue( );
        queue.setAccountId( accountId );
        queue.setQueueName( row.getString( "queue_name" ) );
        queue.setAttributes( row.getMap( "attributes", String.class, String.class ) );
        try {
          if ( queue.getRedrivePolicy( ) != null && queue.getRedrivePolicy( ).isObject( ) &&
              queue.getRedrivePolicy( ).has( Constants.DEAD_LETTER_TARGET_ARN ) &&
              queue.getRedrivePolicy( ).get( Constants.DEAD_LETTER_TARGET_ARN ).isTextual( ) &&
              Objects.equals( deadLetterTargetArn, queue.getRedrivePolicy( ).get( Constants.DEAD_LETTER_TARGET_ARN ).textValue( ) ) ) {
            queueKeys.add( queue.getKey( ) );
          }
        } catch ( SimpleQueueException ignore ) {
          // redrive policy doesn't match, ignore it
        }
      }
      return queueKeys;
    } );
  }

  @Override
  public Queue updateQueueAttributes(String accountId, String queueName, Map<String, String> attributes) throws QueueDoesNotExistException {
    return doThrowsWithSession( session -> {
      Queue queue = lookupQueue( accountId, queueName );
      if ( queue == null ) {
        throw new QueueDoesNotExistException( "Queue " + queueName + " does not exist" );
      }
      UUID uniqueIdPerVersion = UUIDs.timeBased( );
      BatchStatement batchStatement = new BatchStatement( );
      Statement statement1 = new SimpleStatement(
          "UPDATE queues SET unique_id_per_version = ?, attributes = ? WHERE account_id = ? AND queue_name = ?",
          uniqueIdPerVersion,
          attributes,
          accountId,
          queueName
      );
      batchStatement.add( statement1 );
      String oldDeadLetterTargetArn = queue.getDeadLetterTargetArn( );
      queue.setAttributes( attributes );
      queue.setUniqueIdPerVersion( uniqueIdPerVersion.toString( ) );
      String newDeadLetterTargetArn = queue.getDeadLetterTargetArn( );
      if ( !Objects.equals( oldDeadLetterTargetArn, newDeadLetterTargetArn ) ) {
        if ( oldDeadLetterTargetArn != null ) {
          Statement statement2 = new SimpleStatement(
              "DELETE FROM  queues_by_source_queue WHERE source_queue_arn = ? AND account_id = ? AND queue_name = ?",
              oldDeadLetterTargetArn,
              accountId,
              queueName
          );
          batchStatement.add( statement2 );
        }
        if ( newDeadLetterTargetArn != null ) {
          Statement statement3 = new SimpleStatement(
              "INSERT INTO queues_by_source_queue (source_queue_arn, account_id, queue_name) VALUES (?, ?, ?)",
              newDeadLetterTargetArn,
              accountId,
              queueName
          );
          batchStatement.add( statement3 );
        }
      }
      session.execute( batchStatement );
      return lookupQueue( accountId, queueName );
    } );
  }

  @Override
  public void deleteQueue(String accountId, String queueName) throws QueueDoesNotExistException {
    doThrowsWithSession( session -> {
      Statement statement1 = new SimpleStatement(
          "SELECT partition_token, attributes FROM queues WHERE account_id=? AND queue_name = ?",
          accountId,
          queueName
      );
      Iterator<Row> rowIter = session.execute( statement1 ).iterator( );
      if ( !rowIter.hasNext( ) ) {
        throw new QueueDoesNotExistException( "The specified queue does not exist." );
      }
      Row row = rowIter.next( );
      String partitionToken = row.getString( "partition_token" );

      Queue queue = new Queue( );
      queue.setAccountId( accountId );
      queue.setQueueName( queueName );
      queue.setAttributes( row.getMap( "attributes", String.class, String.class ) );
      String deadLetterTargetArn = queue.getDeadLetterTargetArn( );

      BatchStatement batchStatement = new BatchStatement( );
      Statement statement2 = new SimpleStatement(
          "DELETE FROM QUEUES WHERE account_id = ? AND queue_name = ?",
          accountId,
          queueName
      );
      batchStatement.add( statement2 );
      Statement statement3 = new SimpleStatement(
          "DELETE FROM queues_by_partition WHERE partition_token = ? AND account_id = ? AND queue_name = ?",
          partitionToken,
          accountId,
          queueName
      );
      batchStatement.add( statement3 );
      if ( deadLetterTargetArn != null ) {
        Statement statement4 = new SimpleStatement(
            "DELETE FROM  queues_by_source_queue WHERE source_queue_arn = ? AND account_id = ? AND queue_name = ?",
            deadLetterTargetArn,
            accountId,
            queueName
        );
        batchStatement.add( statement4 );
      }
      session.execute( batchStatement );
      return null;
    } );
  }

  @Override
  public Collection<Queue.Key> listActiveQueues(String partitionToken) {
    return doWithSession( session -> {
      Statement statement = new SimpleStatement(
          "SELECT account_id, queue_name, last_lookup FROM queues_by_partition WHERE partition_token = ?",
          partitionToken
      );
      List<Queue.Key> queueKeys = Lists.newArrayList( );
      Date now = new Date( );
      for ( Row row : session.execute( statement ) ) {
        Date lastLookup = row.getTimestamp( "last_lookup" );
        if ( now.getTime( ) - lastLookup.getTime( ) <= SimpleQueueProperties.ACTIVE_QUEUE_TIME_SECS * 1000L ) {
          queueKeys.add( new Queue.Key( row.getString( "account_id" ), row.getString( "queue_name" ) ) );
        }
      }
      return queueKeys;
    } );
  }

  @Override
  public Collection<String> getPartitionTokens() {
    if (SimpleQueueProperties.ENABLE_METRICS_COLLECTION) {
      return partitionTokens;
    } else {
      return Collections.EMPTY_LIST;
    }
  }

  @Override
  public long countQueues(String accountNumber) {
    return doWithSession( session -> {
      Statement statement = new SimpleStatement(
          "SELECT COUNT(*) FROM queues WHERE account_id = ?",
          accountNumber
      );
      Iterator<Row> rowIter = session.execute( statement ).iterator( );
      if ( rowIter.hasNext( ) ) {
        Row row = rowIter.next( );
        return row.getLong( 0 );
      }
      return 0L;
    } );
  }

  private <R,E extends SimpleQueueException> R doThrowsWithSession(
      final ThrowingFunction<Session,R,E> callbackFunction
  ) throws E {
    return sessionProvider.doThrowsWithSession( callbackFunction );
  }

  private <R> R doWithSession( final Function<Session,R> callbackFunction ) {
    return sessionProvider.doWithSession( callbackFunction );
  }
}
