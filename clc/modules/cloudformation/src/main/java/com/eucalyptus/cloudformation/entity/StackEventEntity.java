package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by ethomas on 12/18/13.
 */


@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_events" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackEventEntity extends AbstractPersistent {

  @Column(name = "event_id", nullable = false )
  String eventId;
  @Column(name = "logical_resource_id", nullable = false )
  String logicalResourceId;
  @Column(name = "physical_resource_id", nullable = false )
  String physicalResourceId;
  @Column(name = "resource_properties", nullable = false )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String resourceProperties;
  @Column(name = "resource_status", nullable = false )
  @Enumerated(EnumType.STRING)
  StackResourceEntity.Status resourceStatus;
  @Column(name = "resource_status_reason", nullable = false )
  String resourceStatusReason;
  @Column(name = "resource_type", nullable = false )
  String resourceType;
  @Column(name = "stack_id", nullable = false )
  String stackId;
  @Column(name = "stack_name", nullable = false )
  String stackName;
  @Column(name = "timestamp", nullable = false )
  Date timestamp;

  public StackEventEntity() {
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
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

  public String getResourceProperties() {
    return resourceProperties;
  }

  public void setResourceProperties(String resourceProperties) {
    this.resourceProperties = resourceProperties;
  }

  public StackResourceEntity.Status getResourceStatus() {
    return resourceStatus;
  }

  public void setResourceStatus(StackResourceEntity.Status resourceStatus) {
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

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
