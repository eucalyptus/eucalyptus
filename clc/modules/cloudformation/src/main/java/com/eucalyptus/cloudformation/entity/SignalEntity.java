/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
