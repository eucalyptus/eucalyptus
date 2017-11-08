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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 4/1/16.
 */
public class CreationPolicy {
  public static class ResourceSignal {
    private int count = 1;
    private long timeout = TimeUnit.MINUTES.toSeconds(5);

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public long getTimeout() {
      return timeout;
    }

    public void setTimeout(long timeout) {
      this.timeout = timeout;
    }
  }
  public ResourceSignal resourceSignal;

  public ResourceSignal getResourceSignal() {
    return resourceSignal;
  }

  public void setResourceSignal(ResourceSignal resourceSignal) {
    this.resourceSignal = resourceSignal;
  }

  public static CreationPolicy parse(String creationPolicyJson) throws CloudFormationException {
    if (creationPolicyJson == null) return null;
    CreationPolicy creationPolicy = new CreationPolicy();
    JsonNode creationPolicyJsonNode = JsonHelper.getJsonNodeFromString(creationPolicyJson);
    if (!creationPolicyJsonNode.isObject()) {
      throw new ValidationErrorException("CreationPolicy is not a JSON object");
    }
    JsonNode resourceSignalJsonNode = JsonHelper.checkObject(creationPolicyJsonNode,"ResourceSignal");
    if (resourceSignalJsonNode != null) {
      String countStr = JsonHelper.getString(resourceSignalJsonNode, "Count"); // TODO: consider getInt() method
      String timeoutStr = JsonHelper.getString(resourceSignalJsonNode, "Timeout");
      ResourceSignal resourceSignal = new ResourceSignal();
      creationPolicy.setResourceSignal(resourceSignal);
      if (countStr != null) {
        try {
          int count = Integer.parseInt(countStr);
          resourceSignal.setCount(count);
        } catch (NumberFormatException ex) {
          throw new ValidationErrorException("Encountered non numeric value for property Count");
        }
      }
      if (timeoutStr != null) {
        try {
          long timeout = Duration.parse(timeoutStr).getSeconds();
          resourceSignal.setTimeout(timeout);
        } catch (DateTimeParseException ex) {
          throw new ValidationErrorException("Timeout must be an ISO-8601 compliant duration");
        }
      }
    }
    return creationPolicy;
  }
}
