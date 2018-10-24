/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue.persistence;

import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.common.SimpleQueueMetadata;
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

  public static class Key {
    private String accountId;
    private String queueName;

    public String getAccountId() {
      return accountId;
    }

    public String getQueueName() {
      return queueName;
    }

    public Key(String accountId, String queueName) {
      this.accountId = accountId;
      this.queueName = queueName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key) o;

      if (accountId != null ? !accountId.equals(key.accountId) : key.accountId != null) return false;
      return queueName != null ? queueName.equals(key.queueName) : key.queueName == null;

    }

    @Override
    public int hashCode() {
      int result = accountId != null ? accountId.hashCode() : 0;
      result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
      return result;
    }

    public String getArn() {
      return "arn:aws:sqs:" + RegionConfigurations.getRegionNameOrDefault() + ":" + getAccountId()
        + ":" + getQueueName();
    }

  }


  private String accountId;
  private String queueName;

  private String uniqueIdPerVersion;


  private Map<String, String> attributes = Maps.newTreeMap();

  public Queue() {
  }

  public String getUniqueIdPerVersion() {
    return uniqueIdPerVersion;
  }

  public void setUniqueIdPerVersion(String uniqueIdPerVersion) {
    this.uniqueIdPerVersion = uniqueIdPerVersion;
  }

  public Key getKey() {
    return new Key(accountId, queueName);
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
        return getArn() + "/policy/" + getUniqueIdPerVersion();
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
            "      \"Sid\": \"DefaultSid"+getUniqueIdPerVersion()+"\",\n" +
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
    return getKey().getArn();
  }

  public String getDeadLetterTargetArn() {
    try {
      JsonNode redrivePolicy = getRedrivePolicy();
      if (redrivePolicy != null && redrivePolicy.isObject() &&
        redrivePolicy.has(Constants.DEAD_LETTER_TARGET_ARN) &&
        redrivePolicy.get(Constants.DEAD_LETTER_TARGET_ARN).isTextual()) {
        return redrivePolicy.get(Constants.DEAD_LETTER_TARGET_ARN).asText();
      }
    } catch (SimpleQueueException ignore) {
      ;
    }
    return null;
  }
}
