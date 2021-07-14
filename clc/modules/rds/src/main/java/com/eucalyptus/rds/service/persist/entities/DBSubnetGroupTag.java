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
@Table(name = "rds_tag_db_subnet_group")
@DiscriminatorValue("subgrp")
public class DBSubnetGroupTag extends Tag<DBSubnetGroupTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "dbsubnetgroup_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private DBSubnetGroup dbSubnetGroup;

  protected DBSubnetGroupTag() {
  }

  protected DBSubnetGroupTag(final DBSubnetGroup dbSubnetGroup, final String key, final String value) {
    super(dbSubnetGroup.getOwner(), dbSubnetGroup.getArn(), key, value);
    setDbSubnetGroup(dbSubnetGroup);
  }

  public static DBSubnetGroupTag create(final DBSubnetGroup dbSubnetGroup, final String key, final String value) {
    return new DBSubnetGroupTag(dbSubnetGroup, key, value);
  }

  public DBSubnetGroup getDbSubnetGroup() {
    return dbSubnetGroup;
  }

  public void setDbSubnetGroup(final DBSubnetGroup dbSubnetGroup) {
    this.dbSubnetGroup = dbSubnetGroup;
  }
}
