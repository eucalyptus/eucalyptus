/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
