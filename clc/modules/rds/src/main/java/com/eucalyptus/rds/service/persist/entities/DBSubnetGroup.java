/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.common.RdsMetadata.DBSubnetGroupMetadata;
import com.eucalyptus.rds.service.persist.views.DBSubnetGroupView;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_rds" )
@Table( name = "rds_db_subnet_group" )
public class DBSubnetGroup extends UserMetadata<DBSubnetGroup.Status> implements DBSubnetGroupMetadata, DBSubnetGroupView {
  private static final long serialVersionUID = 1L;

  public enum Status {
    Complete,
  }

  @Column( name = "rds_dsg_description", length = 1024, updatable = false )
  private String description;

  @Column( name = "rds_dsg_vpc_id", updatable = false )
  private String vpcId;

  @OneToMany( fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true, mappedBy = "dbSubnetGroup" )
  @OrderBy( "subnetId" )
  private List<DBSubnet> subnets;

  protected DBSubnetGroup( ) {
  }

  protected DBSubnetGroup(final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }


  public static DBSubnetGroup create(
      final OwnerFullName owner,
      final String name,
      final String description
  ) {
    final DBSubnetGroup group = new DBSubnetGroup( owner, name );
    group.setDescription(description);
    group.setState(Status.Complete);
    return group;
  }

  public static DBSubnetGroup exampleWithOwner( final OwnerFullName owner ) {
    return new DBSubnetGroup( owner, null );
  }

  public static DBSubnetGroup exampleWithName( final OwnerFullName owner, final String name ) {
    return new DBSubnetGroup( owner, name );
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

  public List<DBSubnet> getSubnets() {
    return subnets;
  }

  public void setSubnets(final List<DBSubnet> subnets) {
    this.subnets = subnets;
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
        .relativeId("subgrp", getDisplayName());
  }
}
