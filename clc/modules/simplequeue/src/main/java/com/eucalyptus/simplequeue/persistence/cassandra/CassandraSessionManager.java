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
package com.eucalyptus.simplequeue.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.eucalyptus.cassandra.common.CassandraComponent;
import com.eucalyptus.cassandra.common.CassandraKeyspaceSpecification;
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
@CassandraKeyspaceSpecification( "eucalyptus_simplequeue" )
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
