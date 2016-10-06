/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.exceptions.InternalFailureException;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by ethomas on 9/7/16.
 */
public class Queue {
  private String accountId;
  private String queueName;
  private Map<String, String> attributes = Maps.newTreeMap();

  public Queue() {
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getQueueName() {
    return queueName;
  }

  // some getter wrappers
  public int getDelaySeconds() {
    return Integer.parseInt(attributes.get(Constants.DELAY_SECONDS));
  }

  public int getMaximumMessageSize() {
    return Integer.parseInt(attributes.get(Constants.MAXIMUM_MESSAGE_SIZE));
  }

  public int getMessageRetentionPeriod() {
    return Integer.parseInt(attributes.get(Constants.MESSAGE_RETENTION_PERIOD));
  }

  public int getReceiveMessageWaitTimeSeconds() {
    return Integer.parseInt(attributes.get(Constants.RECEIVE_MESSAGE_WAIT_TIME_SECONDS));
  }

  public int getVisibilityTimeout() {
    return Integer.parseInt(attributes.get(Constants.VISIBILITY_TIMEOUT));
  }

  public String getPolicy() {
    return attributes.get(Constants.POLICY);
  }

  public JsonNode getRedrivePolicy() throws SimpleQueueException {
    if (!attributes.containsKey(Constants.REDRIVE_POLICY)) {
      return null;
    } else {
      try {
        return new ObjectMapper().readTree(attributes.get(Constants.REDRIVE_POLICY));
      } catch (IOException e) {
        throw new InternalFailureException("Invalid json for redrive policy " + attributes.get(Constants.REDRIVE_POLICY));
      }
    }
  }

  public Date getCreatedTimestamp() {
    if (!attributes.containsKey(Constants.CREATED_TIMESTAMP)) {
      return null;
    } else {
      // timestamp is in seconds
      return new Date(1000L * Long.parseLong(attributes.get(Constants.CREATED_TIMESTAMP)));
    }
  }

  public Date getLastModifiedTimestamp() {
    if (!attributes.containsKey(Constants.LAST_MODIFIED_TIMESTAMP)) {
      return null;
    } else {
      // timestamp is in seconds
      return new Date(1000L * Long.parseLong(attributes.get(Constants.LAST_MODIFIED_TIMESTAMP)));
    }
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }
}
