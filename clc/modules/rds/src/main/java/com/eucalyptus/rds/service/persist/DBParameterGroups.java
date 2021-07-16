/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.common.msgs.Parameter;
import com.eucalyptus.rds.service.RdsService;
import com.eucalyptus.rds.service.persist.entities.DBParameter;
import com.eucalyptus.rds.service.persist.entities.DBParameterGroup;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Json;
import com.eucalyptus.util.TypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;


import static com.eucalyptus.util.Json.JsonOption.UpperCamelPropertyNaming;


public interface DBParameterGroups {

  <T> T lookupByName(
      OwnerFullName ownerFullName,
      String name,
      Predicate<? super DBParameterGroup> filter,
      Function<? super DBParameterGroup,T> transform ) throws RdsMetadataException;

  <T> List<T> list(
      OwnerFullName ownerFullName,
      Predicate<? super DBParameterGroup> filter,
      Function<? super DBParameterGroup,T> transform ) throws RdsMetadataException;

  DBParameterGroup save( DBParameterGroup dbParameterGroup ) throws RdsMetadataException;

  List<DBParameterGroup> deleteByExample( DBParameterGroup example ) throws RdsMetadataException;

  <T> T updateByExample(
      DBParameterGroup example,
      OwnerFullName ownerFullName,
      String desc,
      Function<? super DBParameterGroup, T> updateTransform
  ) throws RdsMetadataException;

  AbstractPersistentSupport<RdsMetadata.DBParameterGroupMetadata, DBParameterGroup, RdsMetadataException> withRetries( );

  /**
   * Load default parameters for a family
   *
   * Will load an empty group for unknown family.
   */
  @SuppressWarnings("UnstableApiUsage")
  static DefaultDBGroupParameters loadDefaultParameters(final String family) {
    DefaultDBGroupParameters defaultParameters = new DefaultDBGroupParameters();

    try {
      final String resourceName = "rds-pg-" + family + ".json";
      final String parametersJson = Resources.toString(
          Resources.getResource(RdsService.class, resourceName),
          StandardCharsets.UTF_8);

      final ObjectMapper mapper = Json.mapper(EnumSet.of(UpperCamelPropertyNaming));
      defaultParameters = Json.readObject(
          mapper.reader(),
          DefaultDBGroupParameters.class,
          parametersJson);
    } catch (final Exception ignore) {
    }

    return defaultParameters;
  }

  @TypeMapper
  enum DBParameterTransform implements CompatFunction<DBParameter, Parameter> {
    INSTANCE;

    @Override
    public Parameter apply(final DBParameter parameter) {
      final Parameter result = new Parameter();
      result.setAllowedValues(parameter.getAllowedValues());
      result.setApplyMethod(Objects.toString(parameter.getApplyMethod(), null));
      result.setApplyType(parameter.getApplyType());
      result.setDataType(parameter.getDataType());
      result.setDescription(parameter.getDescription());
      result.setIsModifiable(parameter.isModifiable());
      result.setMinimumEngineVersion(parameter.getMinimumEngineVersion());
      result.setParameterName(parameter.getParameterName());
      result.setParameterValue(parameter.getParameterValue());
      result.setSource(Objects.toString(parameter.getSource(), null));
      return result;
    }
  }

  @TypeMapper
  enum DBParameterGroupTransform implements CompatFunction<DBParameterGroup, com.eucalyptus.rds.common.msgs.DBParameterGroup> {
    INSTANCE;

    @Override
    public com.eucalyptus.rds.common.msgs.DBParameterGroup apply(final DBParameterGroup group) {
      final com.eucalyptus.rds.common.msgs.DBParameterGroup result = new com.eucalyptus.rds.common.msgs.DBParameterGroup();
      result.setDBParameterGroupArn( group.getArn() );
      result.setDBParameterGroupFamily( group.getFamily() );
      result.setDBParameterGroupName( group.getDisplayName() );
      result.setDescription( group.getDescription() );
      return result;
    }
  }
}
