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
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.SimpleQueueMetadata;
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
public class Queue implements SimpleQueueMetadata.QueueMetadata {
  private String accountId;
  private String queueName;

  private String uniqueId;
  private Integer version;


  private Map<String, String> attributes = Maps.newTreeMap();

  public Queue() {
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
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

  public String getPolicyAsString() {
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

  @Override
  public String getDisplayName() {
    return queueName;
  }
  @Override
  public OwnerFullName getOwner() {
    return AccountFullName.getInstance(accountId);
  }

  @Override
  public PolicyVersion getPolicy() {
    return new PolicyVersion() {

      @Override
      public String getPolicyVersionId() {
        return getArn() + "/policy/" + getUniqueId() + "/" + getVersion();
      }

      @Override
      public String getPolicyName() {
        return "QueuePolicy"; // TODO: use Id?
      }

      @Override
      public PolicyScope getPolicyScope() {
        return PolicyScope.Resource;
      }

      @Override
      public String getPolicy() {
        if (getPolicyAsString() == null) {
          return "{\n" +
            "  \"Version\": \"2012-10-17\",\n" +
            "  \"Id\": \""+getArn()+"/SQSDefaultPolicy\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Sid\": \"DefaultSid"+getVersion()+"\",\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Principal\": {\n" +
            "        \"AWS\": \"arn:aws:iam::"+getAccountId()+":root\"\n" +
            "      },\n" +
            "      \"Action\": \"SQS:*\",\n" +
            "      \"Resource\": \""+getArn()+"\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        } else {
          return getPolicyAsString();
        }
      }

      @Override
      public String getPolicyHash() {
        return PolicyVersions.hash(getPolicy());
      }
    };
  }

  public String getArn() {
    return "arn:aws:sqs:" + RegionConfigurations.getRegionNameOrDefault() + ":" + getAccountId()
      + ":" + getQueueName();
  }
}
