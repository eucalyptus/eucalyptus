/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.awsusage;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.eucalyptus.cassandra.common.CassandraComponent;
import com.eucalyptus.cassandra.common.CassandraKeyspaceSpecification;
import com.eucalyptus.cassandra.common.CassandraPersistence;
import com.eucalyptus.cassandra.common.util.CqlUtil;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Exceptions;
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
@CassandraKeyspaceSpecification( "eucalyptus_billing" )
@ConfigurableClass(root = "services.billing", description = "Parameters controlling billing service")
public class CassandraSessionManager implements CassandraComponent {
  // TODO: this is a temporary class and needs to be replaced once the cassandra framework is committed to master.
  private static final Logger LOG = Logger.getLogger(CassandraSessionManager.class);
  @ConfigurableField(
    initial = "postgres",
    description = "The db to use"
  )
  public static volatile String DB_TO_USE = "postgres";
  @ConfigurableField(
    initial = "127.0.0.1",
    description = "The host for cassandra",
    changeListener = CassandraSessionManager.ChangeListener.class )
  public static volatile String CASSANDRA_HOST = "127.0.0.1";
  private static Cluster cluster = null;
  private static Session session = null;

  private static synchronized void initCluster() {
    initCluster(CassandraSessionManager.CASSANDRA_HOST);
  }
  private static synchronized void initCluster(String contactPoint) {
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

    // create new keyspace/tables (should not do here)  TODO: move
    session.execute("CREATE KEYSPACE IF NOT EXISTS eucalyptus_billing " +
      "WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ");

    session.execute("USE eucalyptus_billing;");

    try {
      final String cql = Resources.toString(
          Resources.getResource("2017-03-28-eucalyptus-billing-base.cql"),
          StandardCharsets.UTF_8 );
      CqlUtil.splitCql( cql ).forEach( session::execute );
    } catch ( final IOException | ParseException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  private static synchronized Session getSession() {
    if (session == null) {
      initCluster();
    }
    return session;
  }

  /**
   * Perform work using a datastax session in a callback.
   */
  public static <R> R doWithSession(
      final Function<? super Session,? extends R> callbackFunction
  ) {
    if ( "cassandra".equals( DB_TO_USE ) ) {
      return callbackFunction.apply( getSession( ) );
    } else {
      return CassandraPersistence.doWithSession( "eucalyptus_billing", callbackFunction );
    }
  };

  public static class ChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue) throws ConfigurablePropertyException {
      try {
        initCluster((String) newValue);
      } catch (Exception e) {
        throw new ConfigurablePropertyException(e.getMessage());
      }
    }
  }
}
