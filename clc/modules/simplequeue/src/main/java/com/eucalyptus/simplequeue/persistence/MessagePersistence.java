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

import com.eucalyptus.simplequeue.Message;
import com.eucalyptus.simplequeue.exceptions.SimpleQueueException;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ethomas on 9/16/16.
 */
public interface MessagePersistence {

  Collection<Message> receiveMessages(Queue queue, Map<String, String> receiveAttributes) throws SimpleQueueException;

  void sendMessage(Queue queue, Message message, Map<String, String> sendAttributes) throws SimpleQueueException;

  void deleteMessage(Queue queue, String receiptHandle) throws SimpleQueueException;

  void deleteAllMessages(Queue queue);

  Map<String, String> getApproximateMessageCounts(Queue queue);

  void changeMessageVisibility(Queue queue, String receiptHandle, Integer visibilityTimeout) throws SimpleQueueException;

}
