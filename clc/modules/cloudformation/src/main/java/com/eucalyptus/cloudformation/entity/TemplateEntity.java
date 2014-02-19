package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 2/18/14.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "template" )
public class TemplateEntity extends AbstractPersistent {

  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "stack_id", nullable = false )
  String stackId;

  @Column(name = "template_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String templateJson;

  @Column(name="is_record_deleted", nullable = false)
  Boolean recordDeleted;

  public TemplateEntity() {
  }

  public String getTemplateJson() {

    return templateJson;
  }

  public void setTemplateJson(String templateJson) {
    this.templateJson = templateJson;
  }

  public Boolean getRecordDeleted() {
    return recordDeleted;
  }

  public void setRecordDeleted(Boolean recordDeleted) {
    this.recordDeleted = recordDeleted;
  }

  public String getStackId() {
    return stackId;

  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
}
