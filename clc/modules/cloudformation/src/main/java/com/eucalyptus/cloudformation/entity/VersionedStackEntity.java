/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Created by ethomas on 12/18/13.
 */
@MappedSuperclass
public abstract class VersionedStackEntity extends AbstractPersistent {

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "create_operation_timestamp")
  Date createOperationTimestamp;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_operation_timestamp")
  Date lastUpdateOperationTimestamp;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "delete_operation_timestamp")
  Date deleteOperationTimestamp;

  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "capabilities_json" )
  @Type(type="text")
  String capabilitiesJson;

  @Column(name = "description", length =  4000)
  String description;

  @Column(name = "disable_rollback", nullable = false )
  Boolean disableRollback;

  @Column(name = "pseudo_parameter_map_json" )
  @Type(type="text")
  String pseudoParameterMapJson;

  @Column(name = "condition_map_json" )
  @Type(type="text")
  String conditionMapJson;

  @Column(name = "resource_dependency_manager_json" )
  @Type(type="text")
  String resourceDependencyManagerJson;


  @Column(name = "mapping_json" )
  @Type(type="text")
  String mappingJson;

  @Column(name = "notification_arns_json" )
  @Type(type="text")
  String notificationARNsJson;

  @Column(name = "working_outputs_json" )
  @Type(type="text")
  String workingOutputsJson;

  @Column(name = "outputs_json" )
  @Type(type="text")
  String outputsJson;

  @Column(name = "parameters_json" )
  @Type(type="text")
  String parametersJson;

  @Column(name = "stack_id", nullable = false, length = 400 )
  String stackId;

  @Column( name = "stack_policy")
  @Type(type="text")
  String stackPolicy;

  @Column(name = "stack_name", nullable = false )
  String stackName;

  @Column(name = "stack_status", nullable = false )
  @Enumerated(EnumType.STRING)
  Status stackStatus;

  @Column(name = "stack_status_reason" )
  @Type(type="text")
  String stackStatusReason;

  @Column(name = "tags_json" )
  @Type(type="text")
  String tagsJson;

  @Column( name = "template_body" )
  @Type(type="text")
  String templateBody;

  @Column(name = "template_format_version", nullable = false )
  String templateFormatVersion;

  @Column(name = "timeout_in_minutes")
  Integer timeoutInMinutes;

  @Column(name = "stack_version")
  Integer stackVersion;

  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  VersionedStackEntity() {  }

  public Date getCreateOperationTimestamp() {
    return createOperationTimestamp;
  }

  public void setCreateOperationTimestamp(Date createOperationTimestamp) {
    this.createOperationTimestamp = createOperationTimestamp;
  }

  public Date getLastUpdateOperationTimestamp() {
    return lastUpdateOperationTimestamp;
  }

  public void setLastUpdateOperationTimestamp(Date lastUpdateOperationTimestamp) {
    this.lastUpdateOperationTimestamp = lastUpdateOperationTimestamp;
  }

  public Date getDeleteOperationTimestamp() {
    return deleteOperationTimestamp;
  }

  public void setDeleteOperationTimestamp(Date deleteOperationTimestamp) {
    this.deleteOperationTimestamp = deleteOperationTimestamp;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getCapabilitiesJson() {
    return capabilitiesJson;
  }

  public String getResourceDependencyManagerJson() {
    return resourceDependencyManagerJson;
  }

  public void setResourceDependencyManagerJson(String resourceDependencyManagerJson) {
    this.resourceDependencyManagerJson = resourceDependencyManagerJson;
  }

  public void setCapabilitiesJson(String capabilitiesJson) {
    this.capabilitiesJson = capabilitiesJson;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getDisableRollback() {
    return disableRollback;
  }

  public void setDisableRollback(Boolean disableRollback) {
    this.disableRollback = disableRollback;
  }

  public String getPseudoParameterMapJson() {
    return pseudoParameterMapJson;
  }

  public void setPseudoParameterMapJson(String pseudoParameterMapJson) {
    this.pseudoParameterMapJson = pseudoParameterMapJson;
  }

  public String getConditionMapJson() {
    return conditionMapJson;
  }

  public void setConditionMapJson(String conditionMapJson) {
    this.conditionMapJson = conditionMapJson;
  }

  public String getMappingJson() {
    return mappingJson;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public void setTemplateBody(String templateBody) {
    this.templateBody = templateBody;
  }

  public void setMappingJson(String mappingJson) {
    this.mappingJson = mappingJson;
  }

  public String getNotificationARNsJson() {
    return notificationARNsJson;
  }

  public void setNotificationARNsJson(String notificationARNsJson) {
    this.notificationARNsJson = notificationARNsJson;
  }

  public String getWorkingOutputsJson() {
    return workingOutputsJson;
  }

  public void setWorkingOutputsJson(String workingOutputsJson) {
    this.workingOutputsJson = workingOutputsJson;
  }

  public String getOutputsJson() {
    return outputsJson;
  }

  public void setOutputsJson(String outputsJson) {
    this.outputsJson = outputsJson;
  }

  public String getParametersJson() {
    return parametersJson;
  }

  public void setParametersJson(String parametersJson) {
    this.parametersJson = parametersJson;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  @Override
  public void setNaturalId( final String naturalId ) {
    super.setNaturalId( naturalId );
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public Status getStackStatus() {
    return stackStatus;
  }

  public void setStackStatus(Status stackStatus) {
    this.stackStatus = stackStatus;
  }

  public String getStackStatusReason() {
    return stackStatusReason;
  }

  public void setStackStatusReason(String stackStatusReason) {
    this.stackStatusReason = stackStatusReason;
  }

  public String getTagsJson() {
    return tagsJson;
  }

  public void setTagsJson(String tagsJson) {
    this.tagsJson = tagsJson;
  }

  public String getTemplateFormatVersion() {
    return templateFormatVersion;
  }

  public void setTemplateFormatVersion(String templateFormatVersion) {
    this.templateFormatVersion = templateFormatVersion;
  }

  public Integer getTimeoutInMinutes() {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes(Integer timeoutInMinutes) {
    this.timeoutInMinutes = timeoutInMinutes;
  }

  public String getStackPolicy() {
    return stackPolicy;
  }

  public void setStackPolicy(String stackPolicy) {
    this.stackPolicy = stackPolicy;
  }

  public Integer getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(Integer stackVersion) {
    this.stackVersion = stackVersion;
  }

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }

  @Override
  public String toString() {
    return "VersionedStackEntity{" +
      "createOperationTimestamp=" + createOperationTimestamp +
      ", lastUpdateOperationTimestamp=" + lastUpdateOperationTimestamp +
      ", deleteOperationTimestamp=" + deleteOperationTimestamp +
      ", accountId='" + accountId + '\'' +
      ", capabilitiesJson='" + capabilitiesJson + '\'' +
      ", description='" + description + '\'' +
      ", disableRollback=" + disableRollback +
      ", pseudoParameterMapJson='" + pseudoParameterMapJson + '\'' +
      ", conditionMapJson='" + conditionMapJson + '\'' +
      ", resourceDependencyManagerJson='" + resourceDependencyManagerJson + '\'' +
      ", mappingJson='" + mappingJson + '\'' +
      ", notificationARNsJson='" + notificationARNsJson + '\'' +
      ", workingOutputsJson='" + workingOutputsJson + '\'' +
      ", outputsJson='" + outputsJson + '\'' +
      ", parametersJson='" + parametersJson + '\'' +
      ", stackId='" + stackId + '\'' +
      ", stackPolicy='" + stackPolicy + '\'' +
      ", stackName='" + stackName + '\'' +
      ", stackStatus=" + stackStatus +
      ", stackStatusReason='" + stackStatusReason + '\'' +
      ", tagsJson='" + tagsJson + '\'' +
      ", templateBody='" + templateBody + '\'' +
      ", templateFormatVersion='" + templateFormatVersion + '\'' +
      ", timeoutInMinutes=" + timeoutInMinutes +
      ", stackVersion=" + stackVersion +
      ", recordDeleted=" + recordDeleted +
      '}';
  }
}
