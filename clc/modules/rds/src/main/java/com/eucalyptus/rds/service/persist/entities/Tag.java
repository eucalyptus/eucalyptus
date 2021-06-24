/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.common.policy.RdsResourceName;
import com.eucalyptus.rds.service.persist.views.TagView;
import com.eucalyptus.util.Strings;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;


@Entity
@PersistenceContext(name = "eucalyptus_rds")
@Table(name = "rds_tag")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name="resource_type", discriminatorType = DiscriminatorType.STRING, length = 32) // ignored by Hibernate (for JOINED)
@AttributeOverride(name = "displayName", column = @Column(name = "metadata_display_name", updatable = false, nullable = false, length = 128))
public class Tag<T extends Tag<T>> extends UserMetadata<Tag.State>
    implements RdsMetadata.TagMetadata, TagView {

  private static final long serialVersionUID = 1L;

  enum State {
    available
  }

  @Column(name = "resource_id", nullable = false)
  private String resourceId;

  @Column(name = "resource_type", nullable = false)
  private String resourceType;

  @Column(name = "resource_arn", nullable = false, length = 2048)
  private String resourceArn;

  @Column(name = "tag_value", nullable = false, length = 256)
  private String value;

  protected Tag() {
  }

  protected Tag(
      final OwnerFullName owner,
      final String resourceArn,
      final String displayName,
      final String value
  ) {
    super(owner, displayName);
    final RdsResourceName arn = RdsResourceName.parse(resourceArn);
    setResourceArn(resourceArn);
    setResourceId(arn.getResourceName());
    setResourceType(Strings.trimPrefix("rds:", arn.getResourceType()));
    setValue(value);
  }

  public static Tag named(final OwnerFullName userFullName, final String name) {
    final Tag example = new Tag();
    if (userFullName != null) {
      example.setOwnerAccountNumber(userFullName.getAccountNumber());
    }
    example.setDisplayName(name);
    return example;
  }

  @Override
  protected String createUniqueName() {
    return getOwnerAccountNumber() + ":" + getResourceType() + ":" + getResourceId() + ":" + getDisplayName();
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getResourceArn() {
    return resourceArn;
  }

  public void setResourceArn(String resourceArn) {
    this.resourceArn = resourceArn;
  }

  @Override public String getKey() {
    return getDisplayName();
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String getPartition() {
    return ComponentIds.lookup(Eucalyptus.class).name();
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Eucalyptus.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId(
            "resource-type", this.getResourceType(),
            "resource-id", this.getResourceId(),
            "tag", this.getDisplayName());
  }
}
