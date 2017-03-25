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
package com.eucalyptus.portal.awsusage;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ethomas on 11/22/16.
 */
@ConfigurableClass(root = "services.billing", description = "Parameters controlling billing service")
public class CassandraSessionManager {
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

    session.execute("CREATE TABLE IF NOT EXISTS eucalyptus_billing.aws_records (" +
      "  account_id TEXT,\n" +
      "  service TEXT,\n" +
      "  operation TEXT,\n" +
      "  usage_type TEXT,\n" +
      "  resource TEXT,\n" +
      "  start_time TIMESTAMP,\n" +
      "  end_time TIMESTAMP,\n" +
      "  usage_value TEXT, \n" +
      "  natural_id TIMEUUID,\n" +
      "  operation_usage_type_concat TEXT, \n" +
      "  PRIMARY KEY((account_id, service), end_time, natural_id)\n" +
      ") WITH CLUSTERING ORDER BY (end_time ASC, natural_id ASC);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_operation_idx ON eucalyptus_billing.aws_records (operation);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_usage_type_idx ON eucalyptus_billing.aws_records (usage_type);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_operation_usage_type_idx ON eucalyptus_billing.aws_records (operation_usage_type_concat);");

    // separate table for records as it may not be a low cardinality value (secondary index otherwise)
    session.execute("CREATE TABLE IF NOT EXISTS eucalyptus_billing.aws_records_by_resource (" +
      "  account_id TEXT,\n" +
      "  service TEXT,\n" +
      "  operation TEXT,\n" +
      "  usage_type TEXT,\n" +
      "  resource TEXT,\n" +
      "  start_time TIMESTAMP,\n" +
      "  end_time TIMESTAMP,\n" +
      "  usage_value TEXT, \n" +
      "  natural_id TIMEUUID,\n" +
      "  operation_usage_type_concat TEXT, \n" +
      "  PRIMARY KEY((account_id, service, resource), end_time, natural_id)\n" +
      ") WITH CLUSTERING ORDER BY (end_time ASC, natural_id ASC);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_by_resource_operation_idx ON eucalyptus_billing.aws_records_by_resource (operation);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_by_resource_usage_type_idx ON eucalyptus_billing.aws_records_by_resource (usage_type);");
    session.execute("CREATE INDEX IF NOT EXISTS aws_records_by_resource_operation_usage_type_idx ON eucalyptus_billing.aws_records_by_resource (operation_usage_type_concat);");
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
