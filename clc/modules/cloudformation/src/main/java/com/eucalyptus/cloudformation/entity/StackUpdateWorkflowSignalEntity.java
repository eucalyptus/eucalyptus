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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 4/9/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_update_workflow_signals" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackUpdateWorkflowSignalEntity extends AbstractPersistent {

  @Column(name = "stack_id", nullable = false, length = 400)
  private String stackId;
  @Column(name = "account_id", nullable = false)
  private String accountId;
  @Column(name = "outer_stack_arn", nullable = false, length = 400)
  private String outerStackArn;

  public enum Signal {ROLLBACK, CLEANUP, ROLLBACK_CLEANUP};

  @Column(name = "signal", nullable = false )
  @Enumerated(EnumType.STRING)
  private Signal signal;

  public StackUpdateWorkflowSignalEntity() {
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

  public String getOuterStackArn() {
    return outerStackArn;
  }

  public void setOuterStackArn(String outerStackArn) {
    this.outerStackArn = outerStackArn;
  }

  public Signal getSignal() {
    return signal;
  }

  public void setSignal(Signal signal) {
    this.signal = signal;
  }
}
