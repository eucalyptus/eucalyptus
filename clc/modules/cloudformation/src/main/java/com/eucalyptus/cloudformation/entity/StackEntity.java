package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by ethomas on 12/18/13.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stacks" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackEntity extends AbstractPersistent {

  @ElementCollection
  @CollectionTable( name = "stack_capabilities" )
  @Column( name = "capability" )
  @JoinColumn( name = "stack_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<String> capabilities = Lists.newArrayList();

  @Column(name = "description")
  String description;

  @Column(name = "disable_rollback", nullable = false )
  Boolean disableRollback;

  @ElementCollection
  @CollectionTable( name = "stack_notification_arns" )
  @Column( name = "notification_arn" )
  @JoinColumn( name = "stack_id" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<String> notificationARNs = Lists.newArrayList();

  @ElementCollection
  @CollectionTable(
    name="stack_outputs",
    joinColumns=@JoinColumn(name="stack_id")
  )
  List<Output> outputs;

  @ElementCollection
  @CollectionTable(
    name="stack_parameters",
    joinColumns=@JoinColumn(name="stack_id")
  )
  List<Parameter> parameters = Lists.newArrayList();

  @Column(name = "stack_id", nullable = false )
  String stackId;
  @Column(name = "stack_name", nullable = false )
  String stackName;

  @Column(name = "stack_status", nullable = false )
  @Enumerated(EnumType.STRING)
  Status stackStatus;

  @Column(name = "stack_status_reason" )
  String stackStatusReason;

  @ElementCollection
  @CollectionTable(
    name="stack_tags",
    joinColumns=@JoinColumn(name="stack_id")
  )
  List<Tag> tags = Lists.newArrayList();

  @Column(name = "timeout_in_minutes")
  Integer timeoutInMinutes;

  @Embeddable
  public static class Output {
    @Column(name = "description")
    String description;
    @Column(name = "output_key", nullable = false )
    String outputKey;
    @Column(name = "output_value", nullable = false )
    String outputValue;

    public Output() {
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getOutputKey() {
      return outputKey;
    }

    public void setOutputKey(String outputKey) {
      this.outputKey = outputKey;
    }

    public String getOutputValue() {
      return outputValue;
    }

    public void setOutputValue(String outputValue) {
      this.outputValue = outputValue;
    }
  }

  @Embeddable
  public static class Tag {
    @Column(name = "tag_key", nullable = false )
    String key;
    @Column(name = "tag_value", nullable = false )
    String value;

    public Tag() {
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @Embeddable
  public static class Parameter {
    @Column(name = "parameter_key", nullable = false )
    String parameterKey;
    @Column(name = "parameter_value", nullable = false )
    String parameterValue;

    public Parameter() {
    }

    public String getParameterKey() {
      return parameterKey;
    }

    public void setParameterKey(String parameterKey) {
      this.parameterKey = parameterKey;
    }

    public String getParameterValue() {
      return parameterValue;
    }

    public void setParameterValue(String parameterValue) {
      this.parameterValue = parameterValue;
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

  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
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

  public List<String> getNotificationARNs() {
    return notificationARNs;
  }

  public void setNotificationARNs(List<String> notificationARNs) {
    this.notificationARNs = notificationARNs;
  }

  public List<Output> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<Output> outputs) {
    this.outputs = outputs;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<Parameter> parameters) {
    this.parameters = parameters;
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

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public Integer getTimeoutInMinutes() {
    return timeoutInMinutes;
  }

  public void setTimeoutInMinutes(Integer timeoutInMinutes) {
    this.timeoutInMinutes = timeoutInMinutes;
  }

}
