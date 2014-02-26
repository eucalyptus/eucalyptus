/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 12/18/13.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_resources" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackResourceEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "description")
  String description;
  @Column(name = "logical_resource_id", nullable = false )
  String logicalResourceId;
  @Column(name = "metadata" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String metadata;
  @Column(name = "physical_resource_id" )
  String physicalResourceId;
  @Column(name = "resource_status", nullable = false )
  @Enumerated(EnumType.STRING)
  Status resourceStatus;
  @Column(name = "resource_status_reason" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String resourceStatusReason;
  @Column(name = "resource_type", nullable = false )
  String resourceType;

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }

  @Column(name = "stack_id", nullable = false )
  String stackId;
  @Column(name = "stack_name", nullable = false )
  String stackName;
  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  public enum Status {
    CREATE_IN_PROGRESS,
    CREATE_FAILED,
    CREATE_COMPLETE,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETE,
    UPDATE_IN_PROGRESS,
    UPDATE_FAILED,
    UPDATE_COMPLETE
  }

  public StackResourceEntity() {
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public String getPhysicalResourceId() {
    return physicalResourceId;
  }

  public void setPhysicalResourceId(String physicalResourceId) {
    this.physicalResourceId = physicalResourceId;
  }

  public Status getResourceStatus() {
    return resourceStatus;
  }

  public void setResourceStatus(Status resourceStatus) {
    this.resourceStatus = resourceStatus;
  }

  public String getResourceStatusReason() {
    return resourceStatusReason;
  }

  public void setResourceStatusReason(String resourceStatusReason) {
    this.resourceStatusReason = resourceStatusReason;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
}
