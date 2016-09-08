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
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@ComponentNamed
public class SimpleQueueService {

  static final Logger LOG = Logger.getLogger(SimpleQueueService.class);

  public CreateQueueResponseType createQueue(CreateQueueType request) throws EucalyptusCloudException {
    CreateQueueResponseType reply = request.getReply();
    return reply;
  }

  public GetQueueUrlResponseType getQueueUrl(GetQueueUrlType request) throws EucalyptusCloudException {
    GetQueueUrlResponseType reply = request.getReply();
    return reply;
  }

  public ListQueuesResponseType listQueues(ListQueuesType request) throws EucalyptusCloudException {
    ListQueuesResponseType reply = request.getReply();
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
