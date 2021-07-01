/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;


@Entity
@PersistenceContext(name = "eucalyptus_rds")
@Table(name = "rds_tag_db_parameter_group")
@DiscriminatorValue("pg")
public class DBParameterGroupTag extends Tag<DBParameterGroupTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "dbparametergroup_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private DBParameterGroup dbParameterGroup;

  protected DBParameterGroupTag() {
  }

  protected DBParameterGroupTag(final DBParameterGroup dbParameterGroup, final String key, final String value) {
    super(dbParameterGroup.getOwner(), dbParameterGroup.getArn(), key, value);
    setDbParameterGroup(dbParameterGroup);
  }

  public static DBParameterGroupTag create(final DBParameterGroup dbParameterGroup, final String key, final String value) {
    return new DBParameterGroupTag(dbParameterGroup, key, value);
  }

  public DBParameterGroup getDbParameterGroup() {
    return dbParameterGroup;
  }

  public void setDbParameterGroup(DBParameterGroup dbParameterGroup) {
    this.dbParameterGroup = dbParameterGroup;
  }
}
