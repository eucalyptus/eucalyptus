/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.template;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 4/1/16.
 */
public class UpdatePolicy {

  public static class AutoScalingRollingUpdate {
    int maxBatchSize = 1;
    int minInstancesInService = 0;
    int minSuccessfulInstancesPercent = 100;
    String pauseTime;
    List<String> suspendProcesses = Lists.newArrayList();
    boolean waitOnResourceSignals = false;

    public int getMaxBatchSize() {
      return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
    }

    public int getMinInstancesInService() {
      return minInstancesInService;
    }

    public void setMinInstancesInService(int minInstancesInService) {
      this.minInstancesInService = minInstancesInService;
    }

    public int getMinSuccessfulInstancesPercent() {
      return minSuccessfulInstancesPercent;
    }

    public void setMinSuccessfulInstancesPercent(int minSuccessfulInstancesPercent) {
      this.minSuccessfulInstancesPercent = minSuccessfulInstancesPercent;
    }

    public String getPauseTime() {
      return pauseTime;
    }

    public void setPauseTime(String pauseTime) {
      this.pauseTime = pauseTime;
    }

    public List<String> getSuspendProcesses() {
      return suspendProcesses;
    }

    public void setSuspendProcesses(List<String> suspendProcesses) {
      this.suspendProcesses = suspendProcesses;
    }

    public boolean isWaitOnResourceSignals() {
      return waitOnResourceSignals;
    }

    public void setWaitOnResourceSignals(boolean waitOnResourceSignals) {
      this.waitOnResourceSignals = waitOnResourceSignals;
    }
  }

  private AutoScalingRollingUpdate autoScalingRollingUpdate;

  public AutoScalingRollingUpdate getAutoScalingRollingUpdate() {
    return autoScalingRollingUpdate;
  }

  public void setAutoScalingRollingUpdate(AutoScalingRollingUpdate autoScalingRollingUpdate) {
    this.autoScalingRollingUpdate = autoScalingRollingUpdate;
  }

  private static int parseIntOrDefault(JsonNode parentNode, String fieldKey, int defaultValue) throws CloudFormationException {
    if (parentNode != null) {
      String strVal = JsonHelper.getString(parentNode, fieldKey);
      if (strVal != null) {
        try {
          return Integer.parseInt(strVal);
        } catch (NumberFormatException ex) {
          throw new ValidationErrorException("Encountered non-numeric value for property " + fieldKey);
        }
      }
    }
    return defaultValue;
  }

  public static UpdatePolicy parse(String updatePolicyJson) throws CloudFormationException {
    if (updatePolicyJson == null) return null;
    UpdatePolicy updatePolicy = new UpdatePolicy();
    JsonNode updatePolicyJsonNode = JsonHelper.getJsonNodeFromString(updatePolicyJson);
    if (!updatePolicyJsonNode.isObject()) {
      throw new ValidationErrorException("UpdatePolicy is not a JSON object");
    }
    JsonNode autoScalingRollingUpdateJsonNode = JsonHelper.checkObject(updatePolicyJsonNode,"AutoScalingRollingUpdate");
    if (autoScalingRollingUpdateJsonNode != null) {
      AutoScalingRollingUpdate autoScalingRollingUpdate = new AutoScalingRollingUpdate();
      updatePolicy.setAutoScalingRollingUpdate(autoScalingRollingUpdate);

      autoScalingRollingUpdate.setMaxBatchSize(parseIntOrDefault(autoScalingRollingUpdateJsonNode, "MaxBatchSize", autoScalingRollingUpdate.getMaxBatchSize()));
      if (autoScalingRollingUpdate.getMaxBatchSize() < 1) {
        throw new ValidationErrorException("MaxBatchSize must be a number greater than 0");
      }

      autoScalingRollingUpdate.setMinInstancesInService(parseIntOrDefault(autoScalingRollingUpdateJsonNode, "MinInstancesInService", autoScalingRollingUpdate.getMinInstancesInService()));
      autoScalingRollingUpdate.setMinSuccessfulInstancesPercent(parseIntOrDefault(autoScalingRollingUpdateJsonNode, "MinSuccessfulInstancesPercent", autoScalingRollingUpdate.getMinSuccessfulInstancesPercent()));
      autoScalingRollingUpdate.setWaitOnResourceSignals(Boolean.parseBoolean(JsonHelper.getString(autoScalingRollingUpdateJsonNode, "WaitOnResourceSignals")));
      if (!autoScalingRollingUpdate.isWaitOnResourceSignals() && JsonHelper.getString(autoScalingRollingUpdateJsonNode, "MinSuccessfulInstancesPercent") != null) {
        throw new ValidationErrorException("Can not set MinSuccessfulInstancesPercent greater than zero and not set WaitOnResourceSignals");
      }
      String pauseTime = JsonHelper.getString(autoScalingRollingUpdateJsonNode, "PauseTime");
      if (pauseTime == null) pauseTime = autoScalingRollingUpdate.isWaitOnResourceSignals() ? "PT5M" : "PT0S";
      try {
        Duration.parse(pauseTime).getSeconds();
        autoScalingRollingUpdate.setPauseTime(pauseTime);
      } catch (DateTimeParseException ex) {
        throw new ValidationErrorException("PauseTime must be an ISO-8601 compliant duration");
      }
      JsonNode suspendProcessesNode = JsonHelper.checkArray(autoScalingRollingUpdateJsonNode, "SuspendProcesses");
      if (suspendProcessesNode != null) {
        for (int i = 0; i < suspendProcessesNode.size(); i++) {
          JsonNode childNode = ((ArrayNode) suspendProcessesNode).get(i);
          if (!childNode.isTextual()) {
            throw new ValidationErrorException("SuspendProcesses must be a list of strings");
          }
          String suspendedProcessName = childNode.asText();
          autoScalingRollingUpdate.getSuspendProcesses().add(suspendedProcessName);
        }
      }
    }
    return updatePolicy;
  }
}
