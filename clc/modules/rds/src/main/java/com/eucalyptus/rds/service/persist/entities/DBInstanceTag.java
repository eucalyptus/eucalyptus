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
@Table(name = "rds_tag_db_instance")
@DiscriminatorValue("db")
public class DBInstanceTag extends Tag<DBInstanceTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "dbinstance_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private DBInstance dbInstance;

  protected DBInstanceTag() {
  }

  protected DBInstanceTag(final DBInstance dbInstance, final String key, final String value) {
    super(dbInstance.getOwner(), dbInstance.getArn(), key, value);
    setDbInstance(dbInstance);
  }

  public static DBInstanceTag create(final DBInstance dbInstance, final String key, final String value) {
    return new DBInstanceTag(dbInstance, key, value);
  }

  public DBInstance getDbInstance() {
    return dbInstance;
  }

  public void setDbInstance(DBInstance dbInstance) {
    this.dbInstance = dbInstance;
  }
}
