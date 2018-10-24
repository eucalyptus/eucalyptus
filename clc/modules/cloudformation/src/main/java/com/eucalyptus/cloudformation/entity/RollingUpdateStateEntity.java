/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.standard.actions.AWSAutoScalingAutoScalingGroupResourceAction;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.entities.AbstractPersistent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Collection;
import java.util.Date;

/**
 * Created by ethomas on 4/19/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "rolling_update_state" )
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

  @Column(name = "obsolete_instances_json" )
  @Type(type="text")
  String obsoleteInstancesJson = "";

  @Column(name = "previous_running_instance_ids" )
  @Type(type="text")
  String previousRunningInstanceIds = "";

  @Column(name = "current_batch_instance_ids" )
  @Type(type="text")
  String currentBatchInstanceIds = "";

  @Column(name = "already_suspended_process_names" )
  @Type(type="text")
  String alreadySuspendedProcessNames = "";

  @Column(name = "num_expected_total_signals")
  Integer numExpectedTotalSignals = 0;

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

  public String getObsoleteInstancesJson() {
    return obsoleteInstancesJson;
  }

  public void setObsoleteInstancesJson(String obsoleteInstancesJson) {
    this.obsoleteInstancesJson = obsoleteInstancesJson;
  }

  public AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State getState() {
    return state;
  }

  public void setState(AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State state) {
    this.state = state;
  }

  public String getPreviousRunningInstanceIds() {
    return previousRunningInstanceIds;
  }

  public void setPreviousRunningInstanceIds(String previousRunningInstanceIds) {
    this.previousRunningInstanceIds = previousRunningInstanceIds;
  }

  public String getCurrentBatchInstanceIds() {
    return currentBatchInstanceIds;
  }

  public void setCurrentBatchInstanceIds(String currentBatchInstanceIds) {
    this.currentBatchInstanceIds = currentBatchInstanceIds;
  }

  public String getAlreadySuspendedProcessNames() {
    return alreadySuspendedProcessNames;
  }

  public void setAlreadySuspendedProcessNames(String alreadySuspendedProcessNames) {
    this.alreadySuspendedProcessNames = alreadySuspendedProcessNames;
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

  public static class ObsoleteInstance {
    private String instanceId;

    public ObsoleteInstance(String instanceId, TerminationState lastKnownState) {
      this.instanceId = instanceId;
      this.lastKnownState = lastKnownState;
    }

    public enum TerminationState {RUNNING, TERMINATING, TERMINATED};
    private TerminationState lastKnownState;

    public String getInstanceId() {
      return instanceId;
    }

    public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
    }

    public TerminationState getLastKnownState() {
      return lastKnownState;
    }

    public void setLastKnownState(TerminationState lastKnownState) {
      this.lastKnownState = lastKnownState;
    }

    public static String obsoleteInstancesToJson(Collection<ObsoleteInstance> obsoleteInstances) {
      ObjectNode objectNode = JsonHelper.createObjectNode();
      for (ObsoleteInstance obsoleteInstance: obsoleteInstances) {
        objectNode.put(obsoleteInstance.getInstanceId(), obsoleteInstance.getLastKnownState().toString());
      }
      return JsonHelper.getStringFromJsonNode(objectNode);
    }

    public static Collection<ObsoleteInstance> jsonToObsoleteInstances(String obsoleteInstancesJson) throws ValidationErrorException {
      Collection<ObsoleteInstance> obsoleteInstances = Lists.newArrayList();
      JsonNode jsonNode = JsonHelper.getJsonNodeFromString(obsoleteInstancesJson);
      if (!jsonNode.isObject()) {
        throw new ValidationErrorException("Unable to create collection of obsolete instances from " + obsoleteInstancesJson);
      }
      for (String fieldName : Lists.newArrayList(jsonNode.fieldNames())) {
        ObsoleteInstance obsoleteInstance = new ObsoleteInstance(fieldName, TerminationState.valueOf(jsonNode.get(fieldName).asText()));
        obsoleteInstances.add(obsoleteInstance);
      }
      return obsoleteInstances;
    }
  }
}
