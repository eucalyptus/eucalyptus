/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.engine;

import java.util.Map;
import com.eucalyptus.rds.service.persist.views.DBInstanceView;
import com.google.common.collect.Maps;

/**
 *
 */
public enum RdsEngine {
  postgres("PostgreSQL", 5432, "11.5"){
    @Override
    public Map<String, String> getStackParameters(final DBInstanceView dbInstance) {
      final Map<String,String> parameters = Maps.newTreeMap();
      putIfNotNull(parameters, "PostgresUser", dbInstance.getMasterUsername());
      putIfNotNull(parameters, "PostgresPassword", dbInstance.getMasterUserPassword());
      putIfNotNull(parameters, "PostgresDatabase", dbInstance.getDbName());
      putIfNotNull(parameters, "PostgresVersion", dbInstance.getEngineVersion());
      putIfNotNull(parameters, "PostgresPort", dbInstance.getDbPort());
      return parameters;
    }
  },
  ;

  private final String descName;
  private final int port;
  private final String version;

  RdsEngine(
      final String descName,
      final int port,
      final String version
  ) {
    this.descName = descName;
    this.port = port;
    this.version = version;
  }

  public String getDescription() {
    return descName;
  }

  public String getDefaultDatabaseName() {
    return name();
  }

  public int getDefaultDatabasePort() {
    return port;
  }

  public String getDefaultDatabaseVersion() {
    return version;
  }

  public String getDefaultDatabaseVersionDescription() {
    return descName + " " + version;
  }

  public abstract Map<String,String> getStackParameters(DBInstanceView dbInstance);

  private static void putIfNotNull(final Map<String,String> parameters, final String key, final Object value) {
    if ( value != null ) {
      parameters.put( key, String.valueOf(value) );
    }
  }
}
