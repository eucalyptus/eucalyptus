/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 12/18/13.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_resources" )
public class StackResourceEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;
  @Column(name = "description", length =  4000)
  String description;
  @Column(name = "logical_resource_id", nullable = false )
  String logicalResourceId;
  @Column(name = "metadata_json" )
  @Type(type="text")
  String metadataJson;
  @Column(name = "physical_resource_id" )
  @Type(type="text")
  String physicalResourceId;
  @Column(name = "resource_status", nullable = false )
  @Enumerated(EnumType.STRING)
  Status resourceStatus;
  @Column(name = "resource_status_reason" )
  @Type(type="text")
  String resourceStatusReason;
  @Column(name = "resource_type", nullable = false )
  String resourceType;
  @Column(name = "is_ready", nullable = false)
  Boolean ready = false;
  @Column(name = "properties_json" )
  @Type(type="text")
  String propertiesJson;
  @Column(name = "update_policy_json" )
  @Type(type="text")
  String updatePolicyJson;
  @Column(name = "creation_policy_json" )
  @Type(type="text")
  String creationPolicyJson;
  @Column(name = "deletionPolicy", nullable = false )
  String deletionPolicy = "Delete";
  @Column(name = "is_allowed_by_condition", nullable = false)
  Boolean allowedByCondition;
  @Column(name = "is_created_enough_to_delete", nullable = false)
  Boolean createdEnoughToDelete;
  @Column(name = "reference_value_json" )
  @Type(type="text")
  String referenceValueJson;

  public String getUpdateType() {
    return updateType;
  }

  public void setUpdateType(String updateType) {
    this.updateType = updateType;
  }

  @Column(name = "resource_attributes_json" )
  @Type(type="text")
  String resourceAttributesJson;

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }

  @Column(name = "stack_id", nullable = false, length = 400)
  String stackId;
  @Column(name = "stack_name", nullable = false )
  String stackName;
  @Column(name = "resource_version")
  Integer resourceVersion;


  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  @Column(name="update_type")
  String updateType;

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

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
  }

  public Boolean getReady() {
    return ready;
  }

  public void setReady(Boolean ready) {
    this.ready = ready;
  }

  public String getPropertiesJson() {
    return propertiesJson;
  }

  public void setPropertiesJson(String propertiesJson) {
    this.propertiesJson = propertiesJson;
  }

  public String getUpdatePolicyJson() {
    return updatePolicyJson;
  }

  public void setUpdatePolicyJson(String updatePolicyJson) {
    this.updatePolicyJson = updatePolicyJson;
  }

  public String getCreationPolicyJson() {
    return creationPolicyJson;
  }

  public void setCreationPolicyJson(String creationPolicyJson) {
    this.creationPolicyJson = creationPolicyJson;
  }

  public String getDeletionPolicy() {
    return deletionPolicy;
  }

  public void setDeletionPolicy(String deletionPolicy) {
    this.deletionPolicy = deletionPolicy;
  }

  public Boolean getAllowedByCondition() {
    return allowedByCondition;
  }

  public void setAllowedByCondition(Boolean allowedByCondition) {
    this.allowedByCondition = allowedByCondition;
  }

  public String getReferenceValueJson() {
    return referenceValueJson;
  }

  public void setReferenceValueJson(String referenceValueJson) {
    this.referenceValueJson = referenceValueJson;
  }

  public String getResourceAttributesJson() {
    return resourceAttributesJson;
  }

  public void setResourceAttributesJson(String resourceAttributesJson) {
    this.resourceAttributesJson = resourceAttributesJson;
  }

  public Integer getResourceVersion() {
    return resourceVersion;
  }

  public Boolean getCreatedEnoughToDelete() {
    return createdEnoughToDelete;
  }

  public void setCreatedEnoughToDelete(Boolean createdEnoughToDelete) {
    this.createdEnoughToDelete = createdEnoughToDelete;
  }

  public void setResourceVersion(Integer resourceVersion) {
    this.resourceVersion = resourceVersion;
  }

  @Override
  public String toString() {
    return "StackResourceEntity{" +
      "accountId='" + accountId + '\'' +
      ", description='" + description + '\'' +
      ", logicalResourceId='" + logicalResourceId + '\'' +
      ", metadataJson='" + metadataJson + '\'' +
      ", physicalResourceId='" + physicalResourceId + '\'' +
      ", resourceStatus=" + resourceStatus +
      ", resourceStatusReason='" + resourceStatusReason + '\'' +
      ", resourceType='" + resourceType + '\'' +
      ", ready=" + ready +
      ", createdEnoughToDelete=" + createdEnoughToDelete +
      ", propertiesJson='" + propertiesJson + '\'' +
      ", updatePolicyJson='" + updatePolicyJson + '\'' +
      ", creationPolicyJson='" + creationPolicyJson + '\'' +
      ", deletionPolicy='" + deletionPolicy + '\'' +
      ", allowedByCondition=" + allowedByCondition +
      ", referenceValueJson='" + referenceValueJson + '\'' +
      ", resourceAttributesJson='" + resourceAttributesJson + '\'' +
      ", stackId='" + stackId + '\'' +
      ", stackName='" + stackName + '\'' +
      ", updateType='" + updateType + '\'' +
      ", recordDeleted=" + recordDeleted +
      '}';
  }
}
