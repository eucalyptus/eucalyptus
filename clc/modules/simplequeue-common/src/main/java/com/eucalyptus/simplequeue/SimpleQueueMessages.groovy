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
@GroovyAddClassUUID
package com.eucalyptus.simplequeue

import com.eucalyptus.auth.policy.annotation.PolicyAction
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import java.lang.reflect.Field

public class SimpleQueueMessageWithQueueUrl extends SimpleQueueMessage {
  String queueUrl;
  public SimpleQueueMessageWithQueueUrl() {  }
}

public class CreateQueueType extends SimpleQueueMessage {
  String queueName;
  public CreateQueueType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class ListQueuesType extends SimpleQueueMessage {
  String queueNamePrefix;
  public ListQueuesType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class SendMessageBatchResultEntry extends EucalyptusData {
  String id;
  String messageId;
  String mD5OfMessageBody;
  String mD5OfMessageAttributes;
  public SendMessageBatchResultEntry() {  }
}
public class RemovePermissionType extends SimpleQueueMessageWithQueueUrl {
  String label;
  public RemovePermissionType() {  }
}
public class GetQueueUrlResult extends EucalyptusData {
  String queueUrl;
  public GetQueueUrlResult() {  }
}
public class ListDeadLetterSourceQueuesResult extends EucalyptusData {
  public ListDeadLetterSourceQueuesResult() {  }
  ArrayList<String> queueUrl = new ArrayList<String>();
}
public class ChangeMessageVisibilityType extends SimpleQueueMessageWithQueueUrl {
  String receiptHandle;
  Integer visibilityTimeout;
  public ChangeMessageVisibilityType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class BatchResultErrorEntry extends EucalyptusData {
  String id;
  String code;
  String message;
  Boolean senderFault;
  public BatchResultErrorEntry() {  }
}
public class SendMessageBatchResponseType extends SimpleQueueMessage {
  public SendMessageBatchResponseType() {  }
  SendMessageBatchResult sendMessageBatchResult = new SendMessageBatchResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SetQueueAttributesType extends SimpleQueueMessageWithQueueUrl {
  public SetQueueAttributesType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class CreateQueueResult extends EucalyptusData {
  String queueUrl;
  public CreateQueueResult() {  }
}
public class DeleteMessageBatchResultEntry extends EucalyptusData {
  String id;
  public DeleteMessageBatchResultEntry() {  }
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  public Error() {  }
  ErrorDetail detail = new ErrorDetail();
}
public class ChangeMessageVisibilityBatchRequestEntry extends EucalyptusData {
  String id;
  String receiptHandle;
  Integer visibilityTimeout;
  public ChangeMessageVisibilityBatchRequestEntry() {  }
}
public class SendMessageType extends SimpleQueueMessageWithQueueUrl {
  String messageBody;
  Integer delaySeconds;
  public SendMessageType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
  @HttpEmbedded(multiple=true)
  ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>();
}
public class DeleteMessageBatchResult extends EucalyptusData {
  public DeleteMessageBatchResult() {  }
  ArrayList<DeleteMessageBatchResultEntry> deleteMessageBatchResultEntry = new ArrayList<DeleteMessageBatchResultEntry>();
  ArrayList<BatchResultErrorEntry> batchResultErrorEntry = new ArrayList<BatchResultErrorEntry>();
}
public class AddPermissionType extends SimpleQueueMessageWithQueueUrl {
  String label;
  public AddPermissionType() {  }
  @HttpParameterMapping(parameter="AWSAccountId")
  ArrayList<String> awsAccountId = new ArrayList<String>();
  ArrayList<String> actionName = new ArrayList<String>();
}
public class GetQueueAttributesResponseType extends SimpleQueueMessage {
  public GetQueueAttributesResponseType() {  }
  GetQueueAttributesResult getQueueAttributesResult = new GetQueueAttributesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateQueueResponseType extends SimpleQueueMessage {
  public CreateQueueResponseType() {  }
  CreateQueueResult createQueueResult = new CreateQueueResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SendMessageBatchResult extends EucalyptusData {
  public SendMessageBatchResult() {  }
  ArrayList<SendMessageBatchResultEntry> sendMessageBatchResultEntry = new ArrayList<SendMessageBatchResultEntry>();
  ArrayList<BatchResultErrorEntry> batchResultErrorEntry = new ArrayList<BatchResultErrorEntry>();
}
public class ChangeMessageVisibilityBatchResultEntry extends EucalyptusData {
  String id;
  public ChangeMessageVisibilityBatchResultEntry() {  }
}
@PolicyAction(vendor=SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE, action=SimpleQueuePolicySpec.SIMPLEQUEUE_CHANGEMESSAGEVISIBILITY)
public class ChangeMessageVisibilityBatchType extends SimpleQueueMessageWithQueueUrl {
  public ChangeMessageVisibilityBatchType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<ChangeMessageVisibilityBatchRequestEntry> changeMessageVisibilityBatchRequestEntry = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
}
public class GetQueueAttributesResult extends EucalyptusData {
  public GetQueueAttributesResult() {  }
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class ListDeadLetterSourceQueuesType extends SimpleQueueMessageWithQueueUrl {
  public ListDeadLetterSourceQueuesType() {  }
}
public class SendMessageBatchRequestEntry extends EucalyptusData {
  String id;
  String messageBody;
  Integer delaySeconds;
  public SendMessageBatchRequestEntry() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>();
}
public class DeleteMessageBatchResponseType extends SimpleQueueMessage {
  public DeleteMessageBatchResponseType() {  }
  DeleteMessageBatchResult deleteMessageBatchResult = new DeleteMessageBatchResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetQueueAttributesType extends SimpleQueueMessageWithQueueUrl {
  String unused;
  public GetQueueAttributesType() {  }
  ArrayList<String> attributeName = new ArrayList<String>();
}
public class MessageAttribute extends EucalyptusData {
  String name;
  MessageAttributeValue value;
  public MessageAttribute() {  }
}
public class DeleteQueueType extends SimpleQueueMessageWithQueueUrl {
  public DeleteQueueType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class PurgeQueueType extends SimpleQueueMessageWithQueueUrl {
  public PurgeQueueType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class MessageAttributeValue extends EucalyptusData {
  ArrayList<String> binaryListValue = new ArrayList<String>();
  ArrayList<String> stringListValue = new ArrayList<String>();
  String stringValue;
  String binaryValue;
  String dataType;
  public MessageAttributeValue() {  }
}
public class GetQueueUrlType extends SimpleQueueMessage {
  String queueName;
  String queueOwnerAWSAccountId;
  public GetQueueUrlType() {  }
}
public class ChangeMessageVisibilityBatchResponseType extends SimpleQueueMessage {
  public ChangeMessageVisibilityBatchResponseType() {  }
  ChangeMessageVisibilityBatchResult changeMessageVisibilityBatchResult = new ChangeMessageVisibilityBatchResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteQueueResponseType extends SimpleQueueMessage {
  public DeleteQueueResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class PurgeQueueResponseType extends SimpleQueueMessage {
  public PurgeQueueResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Message extends SimpleQueueMessage {
  String messageId;
  String receiptHandle;
  String mD5OfBody;
  String mD5OfMessageAttributes;
  String body;
  public Message() {  }
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
  ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>();
}
public class ChangeMessageVisibilityResponseType extends SimpleQueueMessage {
  public ChangeMessageVisibilityResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SendMessageResult extends EucalyptusData {
  String messageId;
  String mD5OfMessageBody;
  String mD5OfMessageAttributes;
  public SendMessageResult() {  }
}
public class ChangeMessageVisibilityBatchResult extends EucalyptusData {
  public ChangeMessageVisibilityBatchResult() {  }
  ArrayList<ChangeMessageVisibilityBatchResultEntry> changeMessageVisibilityBatchResultEntry = new ArrayList<ChangeMessageVisibilityBatchResultEntry>();
  ArrayList<BatchResultErrorEntry> batchResultErrorEntry = new ArrayList<BatchResultErrorEntry>();
}
public class DeleteMessageResponseType extends SimpleQueueMessage {
  public DeleteMessageResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ListQueuesResponseType extends SimpleQueueMessage {
  public ListQueuesResponseType() {  }
  ListQueuesResult listQueuesResult = new ListQueuesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class RemovePermissionResponseType extends SimpleQueueMessage {
  public RemovePermissionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
@ComponentMessage(SimpleQueue.class)
public class SimpleQueueMessage extends BaseMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    try {
      Field responseMetadataField = type.class.getDeclaredField("responseMetadata")
      responseMetadataField.setAccessible( true )
      ((ResponseMetadata) responseMetadataField.get( type )).requestId = getCorrelationId()
    } catch ( Exception e ) {
    }
    return type
  }
}
public class ListDeadLetterSourceQueuesResponseType extends SimpleQueueMessage {
  public ListDeadLetterSourceQueuesResponseType() {  }
  ListDeadLetterSourceQueuesResult listDeadLetterSourceQueuesResult = new ListDeadLetterSourceQueuesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SendMessageResponseType extends SimpleQueueMessage {
  public SendMessageResponseType() {  }
  SendMessageResult sendMessageResult = new SendMessageResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteMessageType extends SimpleQueueMessageWithQueueUrl {
  String receiptHandle;
  public DeleteMessageType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<Attribute> attribute = new ArrayList<Attribute>();
}
public class AddPermissionResponseType extends SimpleQueueMessage {
  public AddPermissionResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class GetQueueUrlResponseType extends SimpleQueueMessage {
  public GetQueueUrlResponseType() {  }
  GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
@PolicyAction(vendor=SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE, action=SimpleQueuePolicySpec.SIMPLEQUEUE_SENDMESSAGE)
public class SendMessageBatchType extends SimpleQueueMessageWithQueueUrl {
  public SendMessageBatchType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<SendMessageBatchRequestEntry> sendMessageBatchRequestEntry = new ArrayList<SendMessageBatchRequestEntry>();
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
@PolicyAction(vendor=SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE, action=SimpleQueuePolicySpec.SIMPLEQUEUE_DELETEMESSAGE)
public class DeleteMessageBatchType extends SimpleQueueMessageWithQueueUrl {
  public DeleteMessageBatchType() {  }
  @HttpEmbedded(multiple=true)
  ArrayList<DeleteMessageBatchRequestEntry> deleteMessageBatchRequestEntry = new ArrayList<DeleteMessageBatchRequestEntry>();
}
public class ReceiveMessageType extends SimpleQueueMessageWithQueueUrl {
  Integer maxNumberOfMessages;
  Integer visibilityTimeout;
  Integer waitTimeSeconds;
  String unused;
  public ReceiveMessageType() {  }
  ArrayList<String> attributeName = new ArrayList<String>();
  ArrayList<String> messageAttributeName = new ArrayList<String>();
}
public class DeleteMessageBatchRequestEntry extends EucalyptusData {
  String id;
  String receiptHandle;
  public DeleteMessageBatchRequestEntry() {  }
}
public class SetQueueAttributesResponseType extends SimpleQueueMessage {
  public SetQueueAttributesResponseType() {  }
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Attribute extends EucalyptusData {
  String name;
  String value;
  public Attribute() {  }

  public Attribute(String name, String value) {
    this.name = name
    this.value = value
  }
}
public class SimpleQueueErrorResponse extends SimpleQueueMessage {
  String requestId;
  ArrayList<Error> error = new ArrayList<Error>( );

  SimpleQueueErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${error?.getAt(0)?.code}): ${error?.getAt(0)?.message}"
  }
}
public class ReceiveMessageResponseType extends SimpleQueueMessage {
  public ReceiveMessageResponseType() {  }
  ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ReceiveMessageResult extends EucalyptusData {
  public ReceiveMessageResult() {  }
  ArrayList<Message> message = new ArrayList<Message>();
}
public class ListQueuesResult extends EucalyptusData {
  public ListQueuesResult() {  }
  ArrayList<String> queueUrl = new ArrayList<String>();
}
