/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 3/30/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_resource_signals" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class SignalEntity extends AbstractPersistent {

  @Column(name = "stack_id", nullable = false, length = 400)
  private String stackId;

  @Column(name = "account_id", nullable = false)
  private String accountId;

  @Column(name = "logical_resource_id", nullable = false )
  private String logicalResourceId;

  @Column(name = "resource_version")
  private Integer resourceVersion;

  @Column(name = "unique_id", nullable = false)
  private String uniqueId;

  public enum Status {SUCCESS, FAILURE}

  @Column(name = "status", nullable = false )
  @Enumerated(EnumType.STRING)
  private Status status;

  @Column(name = "processed", nullable = false )
  Boolean processed = false;

  public SignalEntity() {
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

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public Boolean getProcessed() {
    return processed;
  }

  public void setProcessed(Boolean processed) {
    this.processed = processed;
  }

  public void setLogicalResourceId(String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public Integer getResourceVersion() {
    return resourceVersion;
  }

  public void setResourceVersion(Integer resourceVersion) {
    this.resourceVersion = resourceVersion;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

}
