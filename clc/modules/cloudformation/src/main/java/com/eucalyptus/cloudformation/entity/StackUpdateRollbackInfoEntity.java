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
import com.google.common.collect.Sets;
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
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import java.util.Set;

/**
 * Created by ethomas on 4/4/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_update_rollback_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackUpdateRollbackInfoEntity extends AbstractPersistent {


  @Column(name = "stack_id", nullable = false, length = 400)
  String stackId;
  @Column(name = "account_id", nullable = false)
  String accountId;

  @Column(name = "old_resource_dependency_manager_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String oldResourceDependencyManagerJson;

  @Column(name = "resource_dependency_manager_json" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String resourceDependencyManagerJson;

  @Column(name = "rolled_back_stack_version")
  Integer rolledBackStackVersion;

  @Entity
  @PersistenceContext( name = "eucalyptus_cloudformation" )
  @Table( name = "stack_update_rollback_info_resources" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class Resource extends AbstractPersistent {

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

    public Resource() {
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

  public StackUpdateRollbackInfoEntity() {
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

  public Integer getRolledBackStackVersion() {
    return rolledBackStackVersion;
  }

  public void setRolledBackStackVersion(Integer rolledBackStackVersion) {
    this.rolledBackStackVersion = rolledBackStackVersion;
  }

}
