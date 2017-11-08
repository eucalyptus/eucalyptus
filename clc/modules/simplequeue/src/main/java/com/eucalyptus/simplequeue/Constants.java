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

package com.eucalyptus.simplequeue;

/**
 * Created by ethomas on 9/28/16.
 */
public class Constants {

  public static final String ALL = "All";
  public static final String POLICY = "Policy";
  public static final String VISIBILITY_TIMEOUT = "VisibilityTimeout";
  public static final String MAXIMUM_MESSAGE_SIZE = "MaximumMessageSize";
  public static final String MESSAGE_RETENTION_PERIOD = "MessageRetentionPeriod";
  public static final String APPROXIMATE_NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";
  public static final String APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE = "ApproximateNumberOfMessagesNotVisible";
  public static final String CREATED_TIMESTAMP = "CreatedTimestamp";
  public static final String LAST_MODIFIED_TIMESTAMP = "LastModifiedTimestamp";
  public static final String QUEUE_ARN = "QueueArn";
  public static final String APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED = "ApproximateNumberOfMessagesDelayed";
  public static final String DELAY_SECONDS = "DelaySeconds";
  public static final String RECEIVE_MESSAGE_WAIT_TIME_SECONDS = "ReceiveMessageWaitTimeSeconds";
  public static final String REDRIVE_POLICY = "RedrivePolicy";
  public static final String APPROXIMATE_FIRST_RECEIVE_TIMESTAMP = "ApproximateFirstReceiveTimestamp";
  public static final String APPROXIMATE_RECEIVE_COUNT = "ApproximateReceiveCount";
  public static final String SENDER_ID = "SenderId";
  public static final String SENT_TIMESTAMP = "SentTimestamp";

  public static final String WAIT_TIME_SECONDS = "WaitTimeSeconds";
  public static final String MAX_NUMBER_OF_MESSAGES = "MaxNumberOfMessages";

  public static final String DEAD_LETTER_TARGET_ARN = "deadLetterTargetArn";
  public static final String MAX_RECEIVE_COUNT = "maxReceiveCount";

  public static final String NUMBER_OF_MESSAGES_SENT = "NumberOfMessagesSent";
  public static final String AWS_SQS = "AWS/SQS";
  public static final String QUEUE_NAME = "QueueName";
  public static final String SENT_MESSAGE_SIZE = "SentMessageSize";
  public static final String APPROXIMATE_AGE_OF_OLDEST_MESSAGE = "ApproximateAgeOfOldestMessage";

  public static final String APPROXIMATE_NUMBER_OF_MESSAGES_VISIBLE = "ApproximateNumberOfMessagesVisible";
  public static final String NUMBER_OF_EMPTY_RECEIVES = "NumberOfEmptyReceives";
  public static final String NUMBER_OF_MESSAGES_DELETED = "NumberOfMessagesDeleted";
  public static final String NUMBER_OF_MESSAGES_RECEIVED = "NumberOfMessagesReceived";
}
