/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
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
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldResourceDependencyManagerJson;

  @Column(name = "resource_dependency_manager_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
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
