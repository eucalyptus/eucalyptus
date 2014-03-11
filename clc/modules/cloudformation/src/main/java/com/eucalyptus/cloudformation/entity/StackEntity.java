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

import com.eucalyptus.cloudformation.Tag;
import com.eucalyptus.entities.AbstractPersistent;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 12/18/13.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stacks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackEntity extends AbstractPersistent {

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

  @Column(name = "availability_zone_map_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String availabilityZoneMapJson;

  @Column(name = "capabilities_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String capabilitiesJson;

  @Column(name = "description")
  String description;

  @Column(name = "disable_rollback", nullable = false )
  Boolean disableRollback;

  @Column(name = "pseudo_parameter_map_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String pseudoParameterMapJson;

  @Column(name = "condition_map_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String conditionMapJson;

  @Column(name = "resource_dependency_manager_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String resourceDependencyManagerJson;


  @Column(name = "mapping_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String mappingJson;

  @Column(name = "notification_arns_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String notificationARNsJson;

  @Column(name = "outputs_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String outputsJson;

  @Column(name = "parameters_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String parametersJson;

  @Column(name = "stack_id", nullable = false )
  String stackId;

  @Column( name = "stack_policy", length =  16384)
  String stackPolicy;

  @Column(name = "stack_name", nullable = false )
  String stackName;

  @Column(name = "stack_status", nullable = false )
  @Enumerated(EnumType.STRING)
  Status stackStatus;

  @Column(name = "stack_status_reason" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String stackStatusReason;

  @Column(name = "tags_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String tagsJson;

  @Column( name = "template_body", length =  307200)
  String templateBody;

  @Column(name = "template_format_version", nullable = false )
  String templateFormatVersion;

  @Column(name = "timeout_in_minutes")
  Integer timeoutInMinutes;

  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  public static class Output {
    String description;
    String key;
    String stringValue;
    String jsonValue;
    String condition;
    boolean ready = false;
    boolean allowedByCondition = true;

    public Output() {
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public boolean isReady() {
      return ready;
    }

    public void setReady(boolean ready) {
      this.ready = ready;
    }

    public boolean isAllowedByCondition() {
      return allowedByCondition;
    }

    public void setAllowedByCondition(boolean allowedByCondition) {
      this.allowedByCondition = allowedByCondition;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getJsonValue() {
      return jsonValue;
    }

    public void setJsonValue(String jsonValue) {
      this.jsonValue = jsonValue;
    }
  }

  public static class Parameter {
    String key;
    String stringValue;
    String jsonValue;
    boolean noEcho = false;

    public Parameter() {
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getJsonValue() {
      return jsonValue;
    }

    public void setJsonValue(String jsonValue) {
      this.jsonValue = jsonValue;
    }

    public boolean isNoEcho() {
      return noEcho;
    }

    public void setNoEcho(boolean noEcho) {
      this.noEcho = noEcho;
    }
  }

  public enum Status {
    CREATE_IN_PROGRESS,
    CREATE_FAILED,
    CREATE_COMPLETE,
    ROLLBACK_IN_PROGRESS,
    ROLLBACK_FAILED,
    ROLLBACK_COMPLETE,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETE,
    UPDATE_IN_PROGRESS,
    UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
    UPDATE_COMPLETE,
    UPDATE_ROLLBACK_IN_PROGRESS,
    UPDATE_ROLLBACK_FAILED,
    UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
    UPDATE_ROLLBACK_COMPLETE
  }

  public StackEntity() {  }

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

  public String getAvailabilityZoneMapJson() {
    return availabilityZoneMapJson;
  }

  public void setAvailabilityZoneMapJson(String availabilityZoneMapJson) {
    this.availabilityZoneMapJson = availabilityZoneMapJson;
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

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }
}
