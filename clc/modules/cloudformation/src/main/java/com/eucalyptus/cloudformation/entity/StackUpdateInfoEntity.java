package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 12/10/15.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_update_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackUpdateInfoEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "old_capabilities_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldCapabilitiesJson;

  @Column(name = "old_notification_arns_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldNotificationARNsJson;

  @Column(name = "old_parameters_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldParametersJson;

  @Column(name = "stack_id", nullable = false, length = 400 )
  String stackId;

  @Column( name = "new_stack_policy")
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String newStackPolicy;

  @Column( name = "temp_stack_policy")
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String tempStackPolicy;

  @Column(name = "stack_name", nullable = false )
  String stackName;

  @Column( name = "old_template_body" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldTemplateBody;

  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  public StackUpdateInfoEntity() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getOldCapabilitiesJson() {
    return oldCapabilitiesJson;
  }

  public void setOldCapabilitiesJson(String oldCapabilitiesJson) {
    this.oldCapabilitiesJson = oldCapabilitiesJson;
  }

  public String getOldNotificationARNsJson() {
    return oldNotificationARNsJson;
  }

  public void setOldNotificationARNsJson(String oldNotificationARNsJson) {
    this.oldNotificationARNsJson = oldNotificationARNsJson;
  }

  public String getOldParametersJson() {
    return oldParametersJson;
  }

  public void setOldParametersJson(String oldParametersJson) {
    this.oldParametersJson = oldParametersJson;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getNewStackPolicy() {
    return newStackPolicy;
  }

  public void setNewStackPolicy(String newStackPolicy) {
    this.newStackPolicy = newStackPolicy;
  }

  public String getTempStackPolicy() {
    return tempStackPolicy;
  }

  public void setTempStackPolicy(String tempStackPolicy) {
    this.tempStackPolicy = tempStackPolicy;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getOldTemplateBody() {
    return oldTemplateBody;
  }

  public void setOldTemplateBody(String oldTemplateBody) {
    this.oldTemplateBody = oldTemplateBody;
  }

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }

}
