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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.eucalyptus.cassandra.common.CassandraComponent;
import com.eucalyptus.cassandra.common.CassandraKeyspace;
import com.eucalyptus.cassandra.common.CassandraPersistence;
import com.eucalyptus.cassandra.common.util.CqlUtil;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.ThrowingFunction;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ethomas on 11/22/16.
 */
@CassandraKeyspace( "eucalyptus_simplequeue" )
public class CassandraSessionManager implements CassandraComponent {
  private static final Logger LOG = Logger.getLogger(CassandraSessionManager.class);

  private static final CassandraSessionManager external = new CassandraSessionManager();

  private Cluster cluster = null;
  private Session session = null;

  public static CassandraSessionManager external( ) {
    return external;
  }

  private synchronized void initCluster() {
    initCluster(SimpleQueueProperties.CASSANDRA_HOST);
  }
  private synchronized void initCluster(String contactPoint) {
    if (session != null) {
      session.close();
      session = null;
    }
    if (cluster != null) {
      cluster.close();
      cluster = null;
    }
    LOG.info("Trying to connect to the cluster " + contactPoint);
    List<String> contactPoints = Lists.newArrayList();
    for (String s: Splitter.on(",").omitEmptyStrings().split(contactPoint)) {
      contactPoints.add(s);
    }
    cluster = Cluster.builder().addContactPoints(contactPoints.toArray(new String[0])).build();
    session = cluster.connect();
    createSchema( session );
  }

  private static void createSchema( final Session session ) {
    // create new keyspace/tables (should not do here)  TODO: move
    session.execute("CREATE KEYSPACE IF NOT EXISTS eucalyptus_simplequeue " +
      "WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ");

    session.execute("USE eucalyptus_simplequeue;");

    try {
      final String cql = Resources.toString(
          Resources.getResource("2017-03-03-eucalyptus-simplequeue-base.cql"),
          StandardCharsets.UTF_8 );
      CqlUtil.splitCql( cql ).forEach( session::execute );
    } catch ( final IOException | ParseException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public synchronized Session getSession() {
    if (session == null) {
      initCluster();
    }
    return session;
  }

  public static SessionProvider externalProvider( ) {
    return new ExternalSessionProvider( );
  }

  public static SessionProvider internalProvider( ) {
    return new InternalSessionProvider( );
  }

  public static class ChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
      try {
        external( ).initCluster((String) newValue);
      } catch (Exception e) {
        throw new ConfigurablePropertyException(e.getMessage());
      }
    }
  }

  static interface SessionProvider {
    <R,E extends SimpleQueueException> R doThrowsWithSession( final ThrowingFunction<Session,R,E> callbackFunction ) throws E;
    <R> R doWithSession( final Function<Session,R> callbackFunction );
  }

  static class ExternalSessionProvider implements SessionProvider {
    public <R,E extends SimpleQueueException> R doThrowsWithSession( final ThrowingFunction<Session,R,E> callbackFunction ) throws E {
      return callbackFunction.apply( CassandraSessionManager.external( ).getSession( ) );
    }

    public <R> R doWithSession( final Function<Session,R> callbackFunction ) {
      return callbackFunction.apply( CassandraSessionManager.external( ).getSession( ) );
    }
  }

  private static class InternalSessionProvider implements SessionProvider {
    public <R,E extends SimpleQueueException> R doThrowsWithSession( final ThrowingFunction<Session,R,E> callbackFunction ) throws E {
      return CassandraPersistence.doThrowsWithSession( "eucalyptus_simplequeue", callbackFunction );
    }

    public <R> R doWithSession( final Function<Session,R> callbackFunction ) {
      return CassandraPersistence.doWithSession( "eucalyptus_simplequeue", callbackFunction );
    }
  }
}
