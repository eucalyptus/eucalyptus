/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.rds.service.persist.Taggable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.rds.common.Rds;
import com.eucalyptus.rds.common.RdsMetadata.DBInstanceMetadata;
import com.eucalyptus.rds.service.persist.entities.DBInstance.Status;
import com.eucalyptus.rds.service.persist.views.DBInstanceView;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_rds" )
@Table( name = "rds_db_instance" )
public class DBInstance extends UserMetadata<Status> implements DBInstanceMetadata, DBInstanceView, Taggable<DBInstanceTag> {
  private static final long serialVersionUID = 1L;

  public enum Status {
    available,
    backing_up,
    backtracking,
    configuring_enhanced_monitoring,
    configuring_iam_database_auth,
    configuring_log_exports,
    converting_to_vpc,
    creating,
    deleting,
    failed,
    inaccessible_encryption_credentials,
    incompatible_network,
    incompatible_option_group,
    incompatible_parameters,
    incompatible_restore,
    maintenance,
    modifying,
    moving_to_vpc,
    rebooting,
    renaming,
    resetting_master_credentials,
    restore_error,
    starting,
    stopped,
    stopping,
    storage_full,
    storage_optimization,
    upgrading,
    ;

    @SuppressWarnings("unused")
    public static Status fromString(final String value) {
      return Status.valueOf( value.replace('-', '_') );
    }

    public String toString() {
      return name().replace('_', '-');
    }
  }

  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "rds_db_parameter_group_id" )
  private DBParameterGroup dbParameterGroup;

  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "rds_db_subnet_group_id" )
  private DBSubnetGroup dbSubnetGroup;

  @Column( name = "rds_db_zone" )
  private String availabilityZone;

  @Column( name = "rds_db_alloc_storage", nullable = false )
  private Integer allocatedStorage;

  @Column( name = "rds_db_copy_tags_snapshot", nullable = false )
  private Boolean copyTagsToSnapshot;

  @Column( name = "rds_db_instance_class", nullable = false )
  private String instanceClass;

  @Column( name = "rds_db_engine", nullable = false )
  private String engine;

  @Column( name = "rds_db_engine_version", nullable = false )
  private String engineVersion;

  @Column( name = "rds_db_name", nullable = false )
  private String dbName;

  @Column( name = "rds_db_port", nullable = false )
  private Integer dbPort;

  @Column( name = "rds_db_master_user" )
  private String masterUsername;

  @Column( name = "rds_db_master_pass" )
  private String masterUserPassword;

  @Column( name = "rds_db_public", nullable = false )
  private Boolean publiclyAccessible;

  @ElementCollection
  @CollectionTable( name = "rds_db_instance_vpc_sgs", joinColumns = @JoinColumn( name = "rds_db_instance_id" ))
  @Column( name = "rds_db_security_group_id" )
  @OrderColumn( name = "rds_db_security_group_index")
  private List<String> vpcSecurityGroups = Lists.newArrayList();

  @Embedded
  private DBInstanceRuntime dbInstanceRuntime;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "dbInstance")
  private List<DBInstanceTag> tags = Lists.newArrayList();

  protected DBInstance( ) {
  }

  protected DBInstance(final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static DBInstance create(
      final OwnerFullName owner,
      final String name,
      final Integer allocatedStorage,
      final Boolean copyTagsToSnapshot,
      final String dbName,
      final Integer dbPort,
      final String instanceClass,
      final String engine,
      final String engineVersion,
      final Boolean publiclyAccessible
      ) {
    final DBInstance instance = new DBInstance( owner, name );
    instance.setDbInstanceRuntime(new DBInstanceRuntime(instance));
    instance.setState(Status.creating);
    instance.setAllocatedStorage(allocatedStorage);
    instance.setCopyTagsToSnapshot(copyTagsToSnapshot);
    instance.setDbName(dbName);
    instance.setDbPort(dbPort);
    instance.setInstanceClass(instanceClass);
    instance.setEngine(engine);
    instance.setEngineVersion(engineVersion);
    instance.setPubliclyAccessible(publiclyAccessible);
    return instance;
  }

  @Override public DBInstanceTag createTag(final String key, final String value) {
    return DBInstanceTag.create(this, key, value);
  }

  @Override public void updateTag(final DBInstanceTag tag, final String value) {
    tag.setValue(value);
  }

  public static DBInstance exampleWithOwner( final OwnerFullName owner ) {
    return new DBInstance( owner, null );
  }

  public static DBInstance exampleWithName( final OwnerFullName owner, final String name ) {
    return new DBInstance( owner, name );
  }

  public static DBInstance exampleWithStatus( final Status status ) {
    final DBInstance dbInstance = new DBInstance( );
    dbInstance.setState( status );
    dbInstance.setStateChangeStack( null );
    dbInstance.setLastState( null );
    return dbInstance;
  }

  public static DBInstance exampleWithUuid( final String uuid ) {
    final DBInstance dbInstance = new DBInstance();
    dbInstance.setNaturalId( uuid );
    return dbInstance;
  }

  public DBParameterGroup getDbParameterGroup() {
    return dbParameterGroup;
  }

  public void setDbParameterGroup(final DBParameterGroup dbParameterGroup) {
    this.dbParameterGroup = dbParameterGroup;
  }

  public DBSubnetGroup getDbSubnetGroup() {
    return dbSubnetGroup;
  }

  public void setDbSubnetGroup(final DBSubnetGroup dbSubnetGroup) {
    this.dbSubnetGroup = dbSubnetGroup;
  }

  public Integer getAllocatedStorage() {
    return allocatedStorage;
  }

  public void setAllocatedStorage(final Integer allocatedStorage) {
    this.allocatedStorage = allocatedStorage;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(final String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public Boolean getCopyTagsToSnapshot() {
    return copyTagsToSnapshot;
  }

  public void setCopyTagsToSnapshot(final Boolean copyTagsToSnapshot) {
    this.copyTagsToSnapshot = copyTagsToSnapshot;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(final String dbName) {
    this.dbName = dbName;
  }

  public Integer getDbPort() {
    return dbPort;
  }

  public void setDbPort(final Integer dbPort) {
    this.dbPort = dbPort;
  }

  public String getInstanceClass() {
    return instanceClass;
  }

  public void setInstanceClass(final String instanceClass) {
    this.instanceClass = instanceClass;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(final String engine) {
    this.engine = engine;
  }

  public String getEngineVersion() {
    return engineVersion;
  }

  public void setEngineVersion(final String engineVersion) {
    this.engineVersion = engineVersion;
  }

  public String getMasterUsername() {
    return masterUsername;
  }

  public void setMasterUsername(final String masterUsername) {
    this.masterUsername = masterUsername;
  }

  public String getMasterUserPassword() {
    return masterUserPassword;
  }

  public void setMasterUserPassword(final String masterUserPassword) {
    this.masterUserPassword = masterUserPassword;
  }

  public Boolean getPubliclyAccessible() {
    return publiclyAccessible;
  }

  public void setPubliclyAccessible(final Boolean publiclyAccessible) {
    this.publiclyAccessible = publiclyAccessible;
  }

  public List<String> getVpcSecurityGroups() {
    return vpcSecurityGroups;
  }

  public void setVpcSecurityGroups(final List<String> vpcSecurityGroups) {
    this.vpcSecurityGroups = vpcSecurityGroups;
  }

  public DBInstanceRuntime getDbInstanceRuntime() {
    return dbInstanceRuntime;
  }

  public void setDbInstanceRuntime(final DBInstanceRuntime dbInstanceRuntime) {
    this.dbInstanceRuntime = dbInstanceRuntime;
  }

  public String getDbParameterHandle() {
    return getDbInstanceRuntime().getDbParameterHandle();
  }

  @Override public List<DBInstanceTag> getTags() {
    return tags;
  }

  @Override public void setTags(List<DBInstanceTag> tags) {
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
        .relativeId("db", getDisplayName());
  }

  @PostLoad
  protected void onLoad() {
    if ( dbInstanceRuntime == null ) {
      dbInstanceRuntime = new DBInstanceRuntime(this);
    }
  }
}
