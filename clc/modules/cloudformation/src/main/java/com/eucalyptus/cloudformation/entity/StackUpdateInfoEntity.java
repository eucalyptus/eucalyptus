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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 4/4/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_update_info" )
public class StackUpdateInfoEntity extends AbstractPersistent {


  @Column(name = "stack_id", nullable = false, length = 400)
  String stackId;
  @Column(name = "account_id", nullable = false)
  String accountId;

  // For stack tags:
  @Column(name = "stack_name", nullable = false)
  String stackName;

  // For stack tags:
  @Column(name = "account_alias", nullable = false)
  String accountAlias;

  @Column(name = "old_resource_dependency_manager_json" )
  @Type(type="text")
  String oldResourceDependencyManagerJson;

  @Column(name = "resource_dependency_manager_json" )
  @Type(type="text")
  String resourceDependencyManagerJson;

  @Column(name = "updated_stack_version")
  Integer updatedStackVersion;

  @Column(name = "has_called_rollback_stack_state")
  Boolean hasCalledRollbackStackState = false;

  public Boolean getHasCalledRollbackStackState() {
    return hasCalledRollbackStackState;
  }

  public void setHasCalledRollbackStackState(Boolean hasCalledRollbackStackState) {
    this.hasCalledRollbackStackState = hasCalledRollbackStackState;
  }

  @Entity
  @PersistenceContext( name = "eucalyptus_cloudformation" )
  @Table( name = "stack_update_info_rolledback_resources" )
  public static class RolledBackResource extends AbstractPersistent {

    @Column(name = "stack_id", nullable = false, length = 400)
    String stackId;
    @Column(name = "account_id", nullable = false)
    String accountId;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;
    public enum RollbackStatus {
      STARTED,
      COMPLETED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "rollback_status", nullable = false)
    private RollbackStatus rollbackStatusValue;

    public RolledBackResource() {
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

    public String getResourceId() {
      return resourceId;
    }

    public void setResourceId(String resourceId) {
      this.resourceId = resourceId;
    }

    public RollbackStatus getRollbackStatusValue() {
      return rollbackStatusValue;
    }

    public void setRollbackStatusValue(RollbackStatus rollbackStatusValue) {
      this.rollbackStatusValue = rollbackStatusValue;
    }
  }

  public StackUpdateInfoEntity() {
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

  public String getOldResourceDependencyManagerJson() {
    return oldResourceDependencyManagerJson;
  }

  public void setOldResourceDependencyManagerJson(String oldResourceDependencyManagerJson) {
    this.oldResourceDependencyManagerJson = oldResourceDependencyManagerJson;
  }

  public String getResourceDependencyManagerJson() {
    return resourceDependencyManagerJson;
  }

  public void setResourceDependencyManagerJson(String resourceDependencyManagerJson) {
    this.resourceDependencyManagerJson = resourceDependencyManagerJson;
  }

  public Integer getUpdatedStackVersion() {
    return updatedStackVersion;
  }

  public void setUpdatedStackVersion(Integer updatedStackVersion) {
    this.updatedStackVersion = updatedStackVersion;
  }

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getAccountAlias() {
    return accountAlias;
  }

  public void setAccountAlias(String accountAlias) {
    this.accountAlias = accountAlias;
  }
}
