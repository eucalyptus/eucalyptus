package com.eucalyptus.simplequeue.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.google.common.primitives.Ints;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 11/22/16.
 */
public class CassandraSessionManager {
  private static final Logger LOG = Logger.getLogger(CassandraSessionManager.class);
  private static Cluster cluster = null;
  private static Session session = null;

  private static synchronized void initCluster() {
    initCluster(SimpleQueueProperties.CASSANDRA_HOST);
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
    cluster = Cluster.builder().addContactPoint(contactPoint).build();
    session = cluster.connect();

    // create new keyspace/tables (should not do here)  TODO: move
    session.execute("CREATE KEYSPACE IF NOT EXISTS eucalyptus_simplequeue " +
      "WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ");

    session.execute("USE eucalyptus_simplequeue;");

    session.execute("CREATE TABLE IF NOT EXISTS queues (" +
      "account_id TEXT, " +
      "queue_name TEXT, " +
      "unique_id_per_version TIMEUUID, " +
      "attributes MAP<TEXT, TEXT>, " +
      "partition_token text," +
      "PRIMARY KEY ((account_id), queue_name)" +
      ") WITH CLUSTERING ORDER BY (queue_name ASC);");

    session.execute("CREATE TABLE IF NOT EXISTS queues_by_source_queue (" +
      "source_queue_arn TEXT," +
      "account_id TEXT, " +
      "queue_name TEXT, " +
      "last_lookup TIMESTAMP, " +
      "PRIMARY KEY ((source_queue_arn), account_id, queue_name)" +
      ");");

    session.execute("CREATE TABLE IF NOT EXISTS queues_by_partition (" +
      "partition_token TEXT," +
      "account_id TEXT, " +
      "queue_name TEXT, " +
      "last_lookup TIMESTAMP, " +
      "PRIMARY KEY ((partition_token), account_id, queue_name)" +
      ");");
  }


  public static synchronized Session getSession() {
    if (session == null) {
      initCluster();
    }
    return session;
  }

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
