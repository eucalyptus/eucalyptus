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
