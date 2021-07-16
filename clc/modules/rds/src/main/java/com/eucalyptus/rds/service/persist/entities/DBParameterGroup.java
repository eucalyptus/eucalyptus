/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.service.persist.Taggable;
import com.eucalyptus.rds.service.persist.views.DBParameterGroupView;
import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;


/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_rds" )
@Table( name = "rds_db_parameter_group" )
public class DBParameterGroup extends UserMetadata<DBParameterGroup.Status> implements RdsMetadata.DBParameterGroupMetadata, DBParameterGroupView, Taggable<DBParameterGroupTag> {
  private static final long serialVersionUID = 1L;

  public enum Status {
    available,
  }

  @Column( name = "rds_dpg_description", length = 1024, updatable = false )
  private String description;

  @Column( name = "rds_dpg_family", nullable = false, updatable = false )
  private String family;

  @OneToMany( fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true, mappedBy = "dbParameterGroup" )
  @OrderBy( "parameterName" )
  private List<DBParameter> parameters = Lists.newArrayList();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "dbParameterGroup")
  private List<DBParameterGroupTag> tags = Lists.newArrayList();

  protected DBParameterGroup( ) {
  }

  protected DBParameterGroup(final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }
  
  public static DBParameterGroup create(
      final OwnerFullName owner,
      final String name,
      final String description,
      final String family
  ) {
    final DBParameterGroup group = new DBParameterGroup( owner, name );
    group.setState(Status.available);
    group.setDescription(description);
    group.setFamily(family);
    return group;
  }

  @Override
  public DBParameterGroupTag createTag(final String key, final String value) {
    return DBParameterGroupTag.create(this, key, value);
  }

  @Override
  public void updateTag(final DBParameterGroupTag tag, final String value) {
    tag.setValue(value);
  }

  public static DBParameterGroup exampleWithOwner( final OwnerFullName owner ) {
    return new DBParameterGroup( owner, null );
  }

  public static DBParameterGroup exampleWithName( final OwnerFullName owner, final String name ) {
    return new DBParameterGroup( owner, name );
  }

  public boolean isDefault() {
    return getName() != null && getName().startsWith("default.");
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override public String getFamily() {
    return family;
  }

  public void setFamily(String family) {
    this.family = family;
  }

  public List<DBParameter> getParameters() {
    return parameters;
  }

  public void setParameters(
      List<DBParameter> parameters) {
    this.parameters = parameters;
  }

  public List<DBParameterGroupTag> getTags() {
    return tags;
  }

  public void setTags(List<DBParameterGroupTag> tags) {
    this.tags = tags;
  }

  @Override
  public String getPartition() {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Rds.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId("pg", getDisplayName());
  }
}
