/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import com.eucalyptus.cloudformation.resources.standard.actions.AWSAutoScalingAutoScalingGroupResourceAction;
import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Created by ethomas on 4/19/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "rolling_update_state" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class RollingUpdateStateEntity extends AbstractPersistent {

  @Column(name = "account_id", nullable = false)
  String accountId;
  @Column(name = "stack_id", nullable = false, length = 400)
  String stackId;
  @Column(name = "logical_resource_id", nullable = false )
  String logicalResourceId;
  @Column(name = "min_size")
  Integer minSize = 0;
  @Column(name = "max_size")
  Integer maxSize = 0;
  @Column(name = "desired_capacity")
  Integer desiredCapacity = 0;

  @Column(name = "batch_size")
  Integer batchSize = 0;

  @Column(name = "temp_desired_capacity")
  Integer tempDesiredCapacity = 0;

  @Column(name = "state", nullable = false )
  @Enumerated(EnumType.STRING)
  AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State state = AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State.NOT_STARTED;

  @Column(name = "obsolete_instance_ids" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String obsoleteInstanceIds = "";

  @Column(name = "terminating_instance_ids" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String terminatingInstanceIds = "";

  @Column(name = "previous_running_instance_ids" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String previousRunningInstanceIds = "";

  @Column(name = "already_suspended_process_names" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String alreadySuspendedProcessNames = "";

  @Column(name = "num_original_obsolete_instances")
  Integer numOriginalObsoleteInstances = 0;

  @Column(name = "num_success_signals")
  Integer numSuccessSignals = 0;

  @Column(name = "num_failure_signals")
  Integer numFailureSignals = 0;

  @Column(name = "num_expected_total_signals")
  Integer numExpectedTotalSignals = 0;

  @Column(name = "num_needed_signals_this_batch")
  Integer numNeededSignalsThisBatch = 0;

  @Column(name = "num_received_signals_this_batch")
  Integer numReceivedSignalsThisBatch = 0;

  @Column(name = "needs_rollback_update")
  Boolean needsRollbackUpdate = false;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "signal_cutoff_timestamp")
  Date signalCutoffTimestamp;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getLogicalResourceId() {
    return logicalResourceId;
  }

  public void setLogicalResourceId(String logicalResourceId) {
    this.logicalResourceId = logicalResourceId;
  }

  public Integer getMinSize() {
    return minSize;
  }

  public void setMinSize(Integer minSize) {
    this.minSize = minSize;
  }

  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Integer maxSize) {
    this.maxSize = maxSize;
  }

  public Integer getDesiredCapacity() {
    return desiredCapacity;
  }

  public void setDesiredCapacity(Integer desiredCapacity) {
    this.desiredCapacity = desiredCapacity;
  }

  public AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State getState() {
    return state;
  }

  public void setState(AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State state) {
    this.state = state;
  }

  public String getObsoleteInstanceIds() {
    return obsoleteInstanceIds;
  }

  public String getPreviousRunningInstanceIds() {
    return previousRunningInstanceIds;
  }

  public void setPreviousRunningInstanceIds(String previousRunningInstanceIds) {
    this.previousRunningInstanceIds = previousRunningInstanceIds;
  }

  public void setObsoleteInstanceIds(String obsoleteInstanceIds) {
    this.obsoleteInstanceIds = obsoleteInstanceIds;
  }

  public String getAlreadySuspendedProcessNames() {
    return alreadySuspendedProcessNames;
  }

  public void setAlreadySuspendedProcessNames(String alreadySuspendedProcessNames) {
    this.alreadySuspendedProcessNames = alreadySuspendedProcessNames;
  }

  public Integer getNumOriginalObsoleteInstances() {
    return numOriginalObsoleteInstances;

  }

  public void setNumOriginalObsoleteInstances(Integer numOriginalObsoleteInstances) {
    this.numOriginalObsoleteInstances = numOriginalObsoleteInstances;
  }

  public Integer getNumSuccessSignals() {
    return numSuccessSignals;
  }

  public void setNumSuccessSignals(Integer numSuccessSignals) {
    this.numSuccessSignals = numSuccessSignals;
  }

  public Integer getNumFailureSignals() {
    return numFailureSignals;
  }

  public void setNumFailureSignals(Integer numFailureSignals) {
    this.numFailureSignals = numFailureSignals;
  }

  public Integer getNumNeededSignalsThisBatch() {
    return numNeededSignalsThisBatch;
  }

  public void setNumNeededSignalsThisBatch(Integer numNeededSignalsThisBatch) {
    this.numNeededSignalsThisBatch = numNeededSignalsThisBatch;
  }

  public Integer getNumReceivedSignalsThisBatch() {
    return numReceivedSignalsThisBatch;
  }

  public void setNumReceivedSignalsThisBatch(Integer numReceivedSignalsThisBatch) {
    this.numReceivedSignalsThisBatch = numReceivedSignalsThisBatch;
  }

  public Integer getNumExpectedTotalSignals() {
    return numExpectedTotalSignals;
  }

  public Boolean getNeedsRollbackUpdate() {
    return needsRollbackUpdate;
  }

  public void setNeedsRollbackUpdate(Boolean needsRollbackUpdate) {
    this.needsRollbackUpdate = needsRollbackUpdate;
  }

  public void setNumExpectedTotalSignals(Integer numExpectedTotalSignals) {
    this.numExpectedTotalSignals = numExpectedTotalSignals;
  }

  public Date getSignalCutoffTimestamp() {
    return signalCutoffTimestamp;
  }

  public void setSignalCutoffTimestamp(Date signalCutoffTimestamp) {
    this.signalCutoffTimestamp = signalCutoffTimestamp;
  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public Integer getTempDesiredCapacity() {
    return tempDesiredCapacity;
  }

  public void setTempDesiredCapacity(Integer tempDesiredCapacity) {
    this.tempDesiredCapacity = tempDesiredCapacity;
  }

  public String getTerminatingInstanceIds() {
    return terminatingInstanceIds;
  }

  public void setTerminatingInstanceIds(String terminatingInstanceIds) {
    this.terminatingInstanceIds = terminatingInstanceIds;
  }


}
