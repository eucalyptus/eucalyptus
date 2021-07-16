/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.engine;

import com.eucalyptus.rds.service.persist.entities.DBParameterGroup;
import com.eucalyptus.rds.service.persist.views.DBParameterGroupView;
import com.eucalyptus.rds.service.persist.views.DBParameterView;
import com.eucalyptus.util.Json;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import java.io.IOException;
import java.util.Map;
import com.eucalyptus.rds.service.persist.views.DBInstanceView;
import com.google.common.collect.Maps;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

/**
 *
 */
public enum RdsEngine {

  postgres("PostgreSQL", 5432, "12", List.of("9.6", "10", "11", "12", "13")){
    @Override public Map<String, String> getStackParameters(final DBInstanceView dbInstance) {
      return RdsEngine.putPostgresParameters(Maps.newTreeMap(), dbInstance);
    }
  },
  mariadb("MariaDB", 3306, "10.5", List.of("10.1", "10.2", "10.3", "10.4", "10.5")){
    @Override public Map<String, String> getStackParameters(final DBInstanceView dbInstance) {
      return RdsEngine.putMariaDbParameters(Maps.newTreeMap(), dbInstance);
    }
  },
  ;

  @Immutable
  public interface Version {
    String getFamily();
    String getVersion();
    String getVersionDescription();
  }

  private final String descName;
  private final int defaultPort;
  private final String defaultVersion;
  private final List<String> familyVersions;

  RdsEngine(
      final String descName,
      final int defaultPort,
      final String defaultVersion,
      final List<String> familyVersions
  ) {
    this.descName = descName;
    this.defaultPort = defaultPort;
    this.defaultVersion = defaultVersion;
    this.familyVersions = familyVersions;
  }

  public String getDescription() {
    return descName;
  }

  public Iterable<Version> getVersions() {
    return familyVersions.map(version -> ImmutableVersion.builder()
        .family(name() + version)
        .version(version)
        .versionDescription(getDescription() + " " + version)
        .build()
    );
  }

  public String getDefaultDatabaseName() {
    return name();
  }

  public int getDefaultDatabasePort() {
    return defaultPort;
  }

  public String getDefaultDatabaseVersion() {
    return defaultVersion;
  }

  public String buildHandle(
      final DBParameterGroupView group,
      final Iterable<? extends DBParameterView> parameters
  ) {
    final Map<String,String> parameterMap = Stream.ofAll(parameters)
        .filter(DBParameterView.Source.user)
        .toJavaMap(parameter -> Tuple.of(parameter.getParameterName(), parameter.getParameterValue()));
    try {
      return Json.writeObjectAsString(parameterMap);
    } catch (IOException e) {
      return "{}";
    }
  }

  public abstract Map<String,String> getStackParameters(DBInstanceView dbInstance);

  private static void putIfNotNull(final Map<String,String> parameters, final String key, final Object value) {
    if ( value != null ) {
      parameters.put( key, String.valueOf(value) );
    }
  }

  private static void putCommonParameters(
      final Map<String,String> parameters,
      final DBInstanceView dbInstance
  ) {
    putIfNotNull(parameters, "ParameterHandle", dbInstance.getDbParameterHandle());
  }

  private static Map<String,String> putPostgresParameters(
      final Map<String,String> parameters,
      final DBInstanceView dbInstance
  ) {
    putCommonParameters(parameters, dbInstance);
    putIfNotNull(parameters, "PostgresUser", dbInstance.getMasterUsername());
    putIfNotNull(parameters, "PostgresPassword", dbInstance.getMasterUserPassword());
    putIfNotNull(parameters, "PostgresDatabase", dbInstance.getDbName());
    putIfNotNull(parameters, "PostgresVersion", dbInstance.getEngineVersion());
    putIfNotNull(parameters, "PostgresPort", dbInstance.getDbPort());
    putIfNotNull(parameters, "ParameterHandle", dbInstance.getDbParameterHandle());
    return parameters;
  }

  private static Map<String,String> putMariaDbParameters(
      final Map<String,String> parameters,
      final DBInstanceView dbInstance
  ) {
    putCommonParameters(parameters, dbInstance);
    putIfNotNull(parameters, "MariadbUser", dbInstance.getMasterUsername());
    putIfNotNull(parameters, "MariadbPassword", dbInstance.getMasterUserPassword());
    putIfNotNull(parameters, "MariadbDatabase", dbInstance.getDbName());
    putIfNotNull(parameters, "MariadbVersion", dbInstance.getEngineVersion());
    putIfNotNull(parameters, "MariadbPort", dbInstance.getDbPort());
    return parameters;
  }
}
