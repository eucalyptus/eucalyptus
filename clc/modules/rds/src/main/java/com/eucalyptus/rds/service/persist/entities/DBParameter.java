/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.rds.service.persist.views.DBParameterView;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_rds" )
@Table( name = "rds_db_parameter", indexes = {
    @Index( name = "rds_db_parameter_id_idx", columnList = "rds_db_parameter_group_id" )
} )
public class DBParameter extends AbstractPersistent implements DBParameterView {

  private static final long serialVersionUID = 1L;

  @ManyToOne
  @JoinColumn( name = "rds_db_parameter_group_id", nullable = false, updatable = false )
  private DBParameterGroup dbParameterGroup;

  @Column( name = "rds_dbp_allowed_values", length = 4096 )
  private String allowedValues;

  @Column( name = "rds_dbp_apply_method", nullable = false )
  @Enumerated( EnumType.STRING )
  private ApplyMethod applyMethod;

  @Column( name = "rds_dbp_apply_type", nullable = false )
  private String applyType;

  @Column( name = "rds_dbp_data_type", nullable = false )
  private String dataType;

  @Column( name = "rds_dbp_description", length = 1024, nullable = false )
  private String description;

  @Column( name = "rds_dbp_modifiable", nullable = false )
  private Boolean modifiable;

  @Column( name = "rds_dbp_minimum_engine_version" )
  private String minimumEngineVersion;

  @Column( name = "rds_dbp_name", nullable = false )
  private String parameterName;

  @Column( name = "rds_dbp_value", length = 1024)
  private String parameterValue;

  @Column( name = "rds_dbp_source", nullable = false)
  @Enumerated( EnumType.STRING )
  private Source source;

  protected DBParameter() {
  }

  public static DBParameter create(
      final DBParameterGroup dbParameterGroup,
      final String parameterName
  ) {
    final DBParameter parameter = new DBParameter();
    parameter.setDbParameterGroup(dbParameterGroup);
    parameter.setParameterName(parameterName);
    return parameter;
  }

  public DBParameterGroup getDbParameterGroup() {
    return dbParameterGroup;
  }

  public void setDbParameterGroup(
      DBParameterGroup dbParameterGroup) {
    this.dbParameterGroup = dbParameterGroup;
  }

  @Override public String getAllowedValues() {
    return allowedValues;
  }

  public void setAllowedValues(String allowedValues) {
    this.allowedValues = allowedValues;
  }

  @Override public ApplyMethod getApplyMethod() {
    return applyMethod;
  }

  public void setApplyMethod(
      ApplyMethod applyMethod) {
    this.applyMethod = applyMethod;
  }

  @Override public String getApplyType() {
    return applyType;
  }

  public void setApplyType(String applyType) {
    this.applyType = applyType;
  }

  @Override public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  @Override public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean isModifiable() {
    return modifiable;
  }

  public void setModifiable(Boolean modifiable) {
    this.modifiable = modifiable;
  }

  @Override public String getMinimumEngineVersion() {
    return minimumEngineVersion;
  }

  public void setMinimumEngineVersion(String minimumEngineVersion) {
    this.minimumEngineVersion = minimumEngineVersion;
  }

  @Override public String getParameterName() {
    return parameterName;
  }

  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
  }

  @Override public String getParameterValue() {
    return parameterValue;
  }

  public void setParameterValue(String parameterValue) {
    this.parameterValue = parameterValue;
  }

  @Override public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }
}
