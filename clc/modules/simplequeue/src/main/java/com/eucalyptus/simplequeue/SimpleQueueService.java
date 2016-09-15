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

package com.eucalyptus.simplequeue;

import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.simplequeue.persistence.PersistenceFactory;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.ws.WebServices;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sf.json.JSONException;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ConfigurableClass( root = "services.simplequeue", description = "Parameters controlling simple queue (SQS)")

@ComponentNamed
public class SimpleQueueService {

  // TODO: move
  public enum AttributeName {
    All,
    Policy,
    VisibilityTimeout,
    MaximumMessageSize,
    MessageRetentionPeriod,
    ApproximateNumberOfMessages,
    ApproximateNumberOfMessagesNotVisible,
    CreatedTimestamp,
    LastModifiedTimestamp,
    QueueArn,
    ApproximateNumberOfMessagesDelayed,
    DelaySeconds,
    ReceiveMessageWaitTimeSeconds,
    RedrivePolicy
  }

  @ConfigurableField( description = "Maximum number of characters in a queue name.",
    initial = "80", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_QUEUE_NAME_LENGTH_CHARS = 80;

  @ConfigurableField( description = "Maximum value for delay seconds.",
    initial = "900", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_DELAY_SECONDS = 900;

  @ConfigurableField( description = "Maximum value for maximum message size.",
    initial = "262144", changeListener = CheckMin1024IntPropertyChangeListener.class )
  public volatile static int MAX_MAXIMUM_MESSAGE_SIZE = 262144;

  @ConfigurableField( description = "Maximum value for message retention period.",
    initial = "1209600", changeListener = CheckMin60IntPropertyChangeListener.class )
  public volatile static int MAX_MESSAGE_RETENTION_PERIOD = 1209600;

  @ConfigurableField( description = "Maximum value for receive message wait time seconds.",
    initial = "20", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_RECEIVE_MESSAGE_WAIT_TIME_SECONDS = 20;

  @ConfigurableField( description = "Maximum value for visibility timeout.",
    initial = "43200", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_VISIBILITY_TIMEOUT = 43200;

  @ConfigurableField( description = "Maximum value for maxReceiveCount (dead letter queue).",
    initial = "43200", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_MAX_RECEIVE_COUNT = 1000;

  public abstract static class CheckMinIntPropertyChangeListener implements PropertyChangeListener {
    protected int minValue = 0;

    public CheckMinIntPropertyChangeListener(int minValue) {
      this.minValue = minValue;
    }

    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      long value;
      try {
        value = Long.parseLong((String) newValue);
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (value > minValue ) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }


  public static class CheckMin1024IntPropertyChangeListener extends CheckMinIntPropertyChangeListener {
    public CheckMin1024IntPropertyChangeListener() {
      super(1024);
    }
  }

  public static class CheckMin60IntPropertyChangeListener extends CheckMinIntPropertyChangeListener {
    public CheckMin60IntPropertyChangeListener() {
      super(60);
    }
  }

  static final Logger LOG = Logger.getLogger(SimpleQueueService.class);

  private int checkAttributeIntMinMax(Attribute attribute, int min, int max) throws InvalidParameterValueException {
    int value;
    try {
      value = Integer.parseInt(attribute.getValue());
    } catch (Exception e) {
      throw new InvalidParameterValueException(attribute.getName() + " must be a number");
    }
    if (value < min || value > max) {
       throw new InvalidParameterValueException(attribute.getName() + " must be a number " +
         "between " + min + " and " + max);
    }
    return value;
  }

  public CreateQueueResponseType createQueue(CreateQueueType request) throws SimpleQueueException {
    CreateQueueResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final String accountId = ctx.getAccountNumber();
      if (request.getQueueName() == null) {
        throw new InvalidParameterValueException("Value for parameter QueueName is invalid. Reason: Must specify a queue name.");
      }

      if (request.getQueueName().isEmpty()) {
        throw new InvalidParameterValueException("Queue name cannot be empty.");
      }

      Pattern queueNamePattern = Pattern.compile("[A-Za-z0-9_-]+");
      if (!queueNamePattern.matcher(request.getQueueName()).matches() ||
        request.getQueueName().length() < 1 ||
        request.getQueueName().length() > MAX_QUEUE_NAME_LENGTH_CHARS) {
        throw new InvalidParameterValueException("Queue name can only include alphanumeric characters, hyphens, or " +
          "underscores. 1 to " + MAX_QUEUE_NAME_LENGTH_CHARS + " in length");
      }

      Map<String, String> attributeMap = Maps.newTreeMap();

      // set some defaults (TODO: constants)
      attributeMap.put(AttributeName.DelaySeconds.toString(), "0");
      attributeMap.put(AttributeName.MaximumMessageSize.toString(), "262144");
      attributeMap.put(AttributeName.MessageRetentionPeriod.toString(), "345600");
      attributeMap.put(AttributeName.ReceiveMessageWaitTimeSeconds.toString(), "0");
      attributeMap.put(AttributeName.VisibilityTimeout.toString(), "30");

      if (request.getAttribute() != null) {
        for (Attribute attribute : request.getAttribute()) {
          AttributeName attributeName;
          try {
            attributeName = AttributeName.valueOf(attribute.getName());
          } catch (NullPointerException | IllegalArgumentException e) {
            throw new InvalidParameterValueException("Attribute.Name must be one of " + Joiner.on(" | ").join(AttributeName.values()));
          }
          switch (attributeName) {

            case DelaySeconds:
              checkAttributeIntMinMax(attribute, 0, MAX_DELAY_SECONDS);
              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case MaximumMessageSize:
              checkAttributeIntMinMax(attribute, 1024, MAX_MAXIMUM_MESSAGE_SIZE);
              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case MessageRetentionPeriod:
              checkAttributeIntMinMax(attribute, 60, MAX_MESSAGE_RETENTION_PERIOD);
              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case ReceiveMessageWaitTimeSeconds:
              checkAttributeIntMinMax(attribute, 0, MAX_RECEIVE_MESSAGE_WAIT_TIME_SECONDS);
              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case VisibilityTimeout:
              checkAttributeIntMinMax(attribute, 0, MAX_VISIBILITY_TIMEOUT);
              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case Policy:

              if (Strings.isNullOrEmpty(attribute.getValue())) continue;

              // TODO: we don't support wildcard Principal
              try {
                PolicyParser.getResourceInstance().parse(attribute.getValue());
              } catch (PolicyParseException e) {
                throw new InvalidParameterValueException("Invalid value for the parameter Policy. ");
              }

              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            case RedrivePolicy:

              if (Strings.isNullOrEmpty(attribute.getValue())) continue;

              // TODO: maybe put this json stuff in its own class/method
              JsonNode redrivePolicyJsonNode;
              try {
                redrivePolicyJsonNode = new ObjectMapper().readTree(attribute.getValue());
              } catch (IOException e) {
                throw new InvalidParameterValueException("Invalid value for the parameter RedrivePolicy. Reason: Redrive policy is not a valid JSON map.");
              }

              if (redrivePolicyJsonNode == null || !redrivePolicyJsonNode.isObject()) {
                throw new InvalidParameterValueException("Invalid value for the parameter RedrivePolicy. Reason: Redrive policy is not a valid JSON map.");
              }

              if (!redrivePolicyJsonNode.has("maxReceiveCount")) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Redrive policy does not contain mandatory attribute: maxReceiveCount.");
              }

              if (!redrivePolicyJsonNode.has("deadLetterTargetArn")) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Redrive policy does not contain mandatory attribute: deadLetterTargetArn.");
              }

              if (redrivePolicyJsonNode.size() > 2) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Only following attributes are supported: [deadLetterTargetArn, maxReceiveCount].");
              }

              JsonNode maxReceiveCountJsonNode = redrivePolicyJsonNode.get("maxReceiveCount");
              // note, if node is non-textual or has non-integer value, .asInt() will return 0, which is ok here.
              if (maxReceiveCountJsonNode == null || (maxReceiveCountJsonNode.asInt() < 1) ||
                (maxReceiveCountJsonNode.asInt() > MAX_MAX_RECEIVE_COUNT)) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Invalid value for maxReceiveCount: " +
                  maxReceiveCountJsonNode + ", valid values are from 1 to" + MAX_MAX_RECEIVE_COUNT + " both " +
                  "inclusive.");
              }

              JsonNode deadLetterTargetArnJsonNode = redrivePolicyJsonNode.get("deadLetterTargetArn");
              if (deadLetterTargetArnJsonNode == null || !(deadLetterTargetArnJsonNode.isTextual())) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Invalid value for deadLetterTargetArn.");
              }

              Ern simpleQueueArn;
              try {
                simpleQueueArn = Ern.parse(deadLetterTargetArnJsonNode.textValue());
              } catch (JSONException e) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Invalid value for deadLetterTargetArn.");
              }

              if (!simpleQueueArn.getRegion().equals(RegionConfigurations.getRegionNameOrDefault())) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Dead-letter target must be in same region as the source.");
              }

              if (!simpleQueueArn.getAccount().equals(accountId)) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Dead-letter target owner should be same as the source.");
              }

              if (PersistenceFactory.getQueuePersistence().lookupQueue(simpleQueueArn.getAccount(), simpleQueueArn.getResourceName()) == null) {
                throw new InvalidParameterValueException("Value " + attribute.getValue() + " for parameter " +
                  "RedrivePolicy is invalid. Reason: Dead letter target does not exist.");
              }

              attributeMap.put(attribute.getName(), attribute.getValue());
              break;

            default:
              continue; // ignore other attributes
          }
        }

        // see if the queue already exists...
        // TODO: maybe record arn or queue url
        Queue queue = PersistenceFactory.getQueuePersistence().lookupQueue(accountId, request.getQueueName());
        if (queue == null) {
          queue = PersistenceFactory.getQueuePersistence().createQueue(accountId, request.getQueueName(), attributeMap);
        } else {
          // make sure fields match
          Set<String> keysWeCareAbout = Sets.newHashSet(
            AttributeName.DelaySeconds.toString(),
            AttributeName.MaximumMessageSize.toString(),
            AttributeName.MessageRetentionPeriod.toString(),
            AttributeName.ReceiveMessageWaitTimeSeconds.toString(),
            AttributeName.VisibilityTimeout.toString(),
            AttributeName.Policy.toString(),
            AttributeName.RedrivePolicy.toString());

          Map<String, String> requestAttributeMap = Maps.newTreeMap();
          requestAttributeMap.putAll(attributeMap);

          Map<String, String> queueAttributeMap = Maps.newTreeMap();
          queueAttributeMap.putAll(queue.getAttributes());

          requestAttributeMap.keySet().retainAll(keysWeCareAbout);
          queueAttributeMap.keySet().retainAll(keysWeCareAbout);
          if (!Objects.equals(requestAttributeMap, queueAttributeMap)) {
            throw new QueueAlreadyExistsException(request.getQueueName() + " already exists.");
          }
        }
        String queueUrl = getQueueUrlFromAccountIdAndQueueName(accountId, request.getQueueName());
        reply.getCreateQueueResult().setQueueUrl(queueUrl);
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  private String getQueueUrlFromAccountIdAndQueueName(String accountId, String queueName) {
    return ServiceUris.remotePublicify(Topology.lookup(SimpleQueue.class)).toString() + accountId + "/" + queueName;
  }

  private Pair<String, String> getAccountIdAndQueueNameFromQueueUrl(String queueUrl) throws InvalidAddressException {
    String accountId = null;
    String queueName = null;
    try {
      URL url = new URL(queueUrl);
      List<String> pathParts = Splitter.on('/').omitEmptyStrings().splitToList(url.getPath());
      if (pathParts.size() == 2) {
        accountId = Accounts.lookupAccountById(pathParts.get(0)).getAccountNumber();
        queueName = pathParts.get(1);
      }
    } catch (MalformedURLException | NullPointerException | AuthException ignore) {}
    if (accountId == null || queueName == null) {
      throw new InvalidAddressException("The address " + queueUrl + " is not valid for this endpoint.");
    }
    return new Pair<>(accountId, queueName);
  }


  public GetQueueUrlResponseType getQueueUrl(GetQueueUrlType request) throws EucalyptusCloudException {
    GetQueueUrlResponseType reply = request.getReply();
    return reply;
  }

  public ListQueuesResponseType listQueues(ListQueuesType request) throws EucalyptusCloudException {
    ListQueuesResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      final String accountId = ctx.getAccountNumber();
      Collection<Queue> queues = PersistenceFactory.getQueuePersistence().listQueuesByPrefix(accountId, request.getQueueNamePrefix());
      if (queues != null) {
        for (Queue queue: queues) {
          reply.getListQueuesResult().getQueueUrl().add(getQueueUrlFromAccountIdAndQueueName(queue.getAccountId(), queue.getQueueName()));
        }
      }
    } catch (Exception ex) {
      handleException(ex);
    }
    return reply;
  }

  public AddPermissionResponseType addPermission(AddPermissionType request) throws EucalyptusCloudException {
    AddPermissionResponseType reply = request.getReply();
    return reply;
  }

  public ChangeMessageVisibilityResponseType changeMessageVisibility(ChangeMessageVisibilityType request) throws EucalyptusCloudException {
    ChangeMessageVisibilityResponseType reply = request.getReply();
    return reply;
  }

  public DeleteMessageResponseType deleteMessage(DeleteMessageType request) throws EucalyptusCloudException {
    DeleteMessageResponseType reply = request.getReply();
    return reply;
  }

  public DeleteQueueResponseType deleteQueue(DeleteQueueType request) throws EucalyptusCloudException {
    DeleteQueueResponseType reply = request.getReply();
    // TODO: IAM rather than own account for now
    try {
      final Context ctx = Contexts.lookup();
      final String accountId = ctx.getAccountNumber();
      Pair<String, String> accountIdAndQueueName = getAccountIdAndQueueNameFromQueueUrl(request.getQueueUrl());
      if (!accountIdAndQueueName.getLeft().equals(accountId)) {
        throw new AccessDeniedException("Access to the resource " + request.getQueueUrl() + " is denied.");
      }
      String queueName = accountIdAndQueueName.getRight();
      PersistenceFactory.getQueuePersistence().deleteQueue(accountId, queueName);
      // TODO: delete messages
    } catch (Exception ex) {
      handleException(ex);
    }

    return reply;
  }

  public PurgeQueueResponseType purgeQueue(PurgeQueueType request) throws EucalyptusCloudException {
    PurgeQueueResponseType reply = request.getReply();
    return reply;
  }

  public GetQueueAttributesResponseType getQueueAttributes(GetQueueAttributesType request) throws EucalyptusCloudException {
    GetQueueAttributesResponseType reply = request.getReply();
    return reply;
  }

  public RemovePermissionResponseType removePermission(RemovePermissionType request) throws EucalyptusCloudException {
    RemovePermissionResponseType reply = request.getReply();
    return reply;
  }

  public ReceiveMessageResponseType receiveMessage(ReceiveMessageType request) throws EucalyptusCloudException {
    ReceiveMessageResponseType reply = request.getReply();
    return reply;
  }

  public SendMessageResponseType sendMessage(SendMessageType request) throws EucalyptusCloudException {
    SendMessageResponseType reply = request.getReply();
    // message id is required
    reply.getSendMessageResult().setMessageId(UUID.randomUUID().toString());
    if (request.getMessageBody() != null) {
      reply.getSendMessageResult().setmD5OfMessageBody(calculateMessageBodyMd5(request.getMessageBody()));
    }
    if (request.getMessageAttribute() != null) {
      reply.getSendMessageResult().setmD5OfMessageAttributes(
        calculateMessageAttributesMd5(convertMessageAttributesToMap(request.getMessageAttribute())));
    }
    return reply;
  }

  public SetQueueAttributesResponseType setQueueAttributes(SetQueueAttributesType request) throws EucalyptusCloudException {
    SetQueueAttributesResponseType reply = request.getReply();
    return reply;
  }

  public ChangeMessageVisibilityBatchResponseType changeMessageVisibilityBatch(ChangeMessageVisibilityBatchType request) throws EucalyptusCloudException {
    ChangeMessageVisibilityBatchResponseType reply = request.getReply();
    return reply;
  }

  public DeleteMessageBatchResponseType deleteMessageBatch(DeleteMessageBatchType request) throws EucalyptusCloudException {
    DeleteMessageBatchResponseType reply = request.getReply();
    return reply;
  }

  public SendMessageBatchResponseType sendMessageBatch(SendMessageBatchType request) throws EucalyptusCloudException {
    SendMessageBatchResponseType reply = request.getReply();
    if (request.getSendMessageBatchRequestEntry() != null) {
      for (SendMessageBatchRequestEntry sendMessageBatchRequestEntry: request.getSendMessageBatchRequestEntry()) {
        if (sendMessageBatchRequestEntry.getId() != null) {
          SendMessageBatchResultEntry sendMessageBatchResultEntry = new SendMessageBatchResultEntry();
          // message id is required
          sendMessageBatchResultEntry.setMessageId(UUID.randomUUID().toString());
          sendMessageBatchResultEntry.setId(sendMessageBatchRequestEntry.getId());
          if (sendMessageBatchRequestEntry.getMessageBody() != null) {
            sendMessageBatchResultEntry.setmD5OfMessageBody(calculateMessageBodyMd5(
              sendMessageBatchRequestEntry.getMessageBody()));
          }
          if (sendMessageBatchRequestEntry.getMessageAttribute() != null) {
            sendMessageBatchResultEntry.setmD5OfMessageAttributes(
              calculateMessageAttributesMd5(
                convertMessageAttributesToMap(sendMessageBatchRequestEntry.getMessageAttribute())));
          }
          reply.getSendMessageBatchResult().getSendMessageBatchResultEntry().add(sendMessageBatchResultEntry);
        }
      }
    }
    return reply;
  }

  public ListDeadLetterSourceQueuesResponseType listDeadLetterSourceQueues(ListDeadLetterSourceQueuesType request) throws EucalyptusCloudException {
    ListDeadLetterSourceQueuesResponseType reply = request.getReply();
    return reply;
  }

  private static Map<String, MessageAttributeValue> convertMessageAttributesToMap(Collection<MessageAttribute> messageAttributes) {
    // yay lambdas?
    return messageAttributes == null ? null : messageAttributes.stream().collect(Collectors.toMap(MessageAttribute::getName, MessageAttribute::getValue));
  }

  private static void handleException(final Exception e) throws SimpleQueueException {
    final SimpleQueueException cause = Exceptions.findCause(e, SimpleQueueException.class);
    if (cause != null) {
      throw cause;
    }

    LOG.error( e, e );

    final InternalFailureException exception = new InternalFailureException(String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }

  // BEGIN CODE FROM Amazon AWS SDK 1.11.28-SNAPSHOT, file: com.amazonaws.services.sqs.MessageMD5ChecksumHandler

  /**
   * Returns the hex-encoded MD5 hash String of the given message body.
   */

  private static final int INTEGER_SIZE_IN_BYTES = 4;
  private static final byte STRING_TYPE_FIELD_INDEX = 1;
  private static final byte BINARY_TYPE_FIELD_INDEX = 2;
  private static final byte STRING_LIST_TYPE_FIELD_INDEX = 3;
  private static final byte BINARY_LIST_TYPE_FIELD_INDEX = 4;


  private static String calculateMessageBodyMd5(String messageBody) throws EucalyptusCloudException {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Message body: " + messageBody);
    }
    byte[] expectedMd5;
    try {
      expectedMd5 = Md5Utils.computeMD5Hash(messageBody.getBytes(UTF8));
    } catch (Exception e) {
      throw new EucalyptusCloudException("Unable to calculate the MD5 hash of the message body. " + e.getMessage(),
        e);
    }
    String expectedMd5Hex = BinaryUtils.toHex(expectedMd5);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Expected  MD5 of message body: " + expectedMd5Hex);
    }
    return expectedMd5Hex;
  }

  /**
   * Returns the hex-encoded MD5 hash String of the given message attributes.
   */
  private static String calculateMessageAttributesMd5(final Map<String, MessageAttributeValue> messageAttributes) throws EucalyptusCloudException {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Message attribtues: " + messageAttributes);
    }
    List<String> sortedAttributeNames = new ArrayList<String>(messageAttributes.keySet());
    Collections.sort(sortedAttributeNames);

    MessageDigest md5Digest = null;
    try {
      md5Digest = MessageDigest.getInstance("MD5");

      for (String attrName : sortedAttributeNames) {
        MessageAttributeValue attrValue = messageAttributes.get(attrName);

        // Encoded Name
        updateLengthAndBytes(md5Digest, attrName);
        // Encoded Type
        updateLengthAndBytes(md5Digest, attrValue.getDataType());

        // Encoded Value
        if (attrValue.getStringValue() != null) {
          md5Digest.update(STRING_TYPE_FIELD_INDEX);
          updateLengthAndBytes(md5Digest, attrValue.getStringValue());
        } else if (attrValue.getBinaryValue() != null) {
          md5Digest.update(BINARY_TYPE_FIELD_INDEX);
          // Eucalyptus stores the value as a Base 64 encoded string.  Convert to byte buffer
          ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.decode(attrValue.getBinaryValue()));
          updateLengthAndBytes(md5Digest, byteBuffer);
        } else if (attrValue.getStringListValue() != null && attrValue.getStringListValue().size() > 0) {
          md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX);
          for (String strListMember : attrValue.getStringListValue()) {
            updateLengthAndBytes(md5Digest, strListMember);
          }
        } else if (attrValue.getBinaryListValue() != null && attrValue.getBinaryListValue().size() > 0) {
          md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX);
          for (String byteListMember : attrValue.getBinaryListValue()) {
            // Eucalyptus stores the value as a Base 64 encoded string.  Convert to byte buffer
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.decode(byteListMember));
            updateLengthAndBytes(md5Digest, byteBuffer);
          }
        }
      }
    } catch (Exception e) {
      throw new EucalyptusCloudException("Unable to calculate the MD5 hash of the message attributes. "
        + e.getMessage(), e);
    }

    String expectedMd5Hex = BinaryUtils.toHex(md5Digest.digest());
    if (LOG.isTraceEnabled()) {
      LOG.trace("Expected  MD5 of message attributes: " + expectedMd5Hex);
    }
    return expectedMd5Hex;
  }

  /**
   * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
   * input String and the actual utf8-encoded byte values.
   */
  private static void updateLengthAndBytes(MessageDigest digest, String str) throws UnsupportedEncodingException {
    byte[] utf8Encoded = str.getBytes(UTF8);
    ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(utf8Encoded.length);
    digest.update(lengthBytes.array());
    digest.update(utf8Encoded);
  }

  /**
   * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
   * input ByteBuffer and all the bytes it contains.
   */
  private static void updateLengthAndBytes(MessageDigest digest, ByteBuffer binaryValue) {
    ByteBuffer readOnlyBuffer = binaryValue.asReadOnlyBuffer();
    int size = readOnlyBuffer.remaining();
    ByteBuffer lengthBytes = ByteBuffer.allocate(INTEGER_SIZE_IN_BYTES).putInt(size);
    digest.update(lengthBytes.array());
    digest.update(readOnlyBuffer);
  }

  // From com.amazonaws.util.StringUtils:

  private static final String DEFAULT_ENCODING = "UTF-8";

  public static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);

  // END CODE FROM Amazon AWS SDK 1.11.28-SNAPSHOT

}
