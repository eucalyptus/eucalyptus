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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.persistence.postgresql;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by ethomas on 9/17/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_simplequeue" )
@Table( name = "messages", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "queue_name", "message_id"},
  name = "queues_composite_key") )
public class MessageEntity extends AbstractPersistent {
  @Column(name = "account_id", nullable = false)
  String accountId;
  @Column(name = "queue_name", nullable = false)
  String queueName;
  @Column(name = "message_id", nullable = false)
  String messageId;
  @Column(name = "sent_timestamp_secs", nullable = false)
  Long sentTimestampSecs = 0L;
  @Column(name = "visible_timestamp_secs", nullable = false)
  Long visibleTimestampSecs = 0L; // when the message will be visible
  @Column(name = "expired_timestamp_secs", nullable = false)
  Long expiredTimestampSecs = 0L; // when the message will expire
  @Column(name = "receive_count", nullable = false)
  Integer receiveCount = 0;
  @Column(name = "local_receive_count", nullable = false)
  Integer localReceiveCount = 0; //

  // In this case receipt handle = account id + queue id + message_id + #times received (might not be best choice)

  @Column(name = "message_json", nullable = false)
  @Type(type="text")
  String messageJson;

  public MessageEntity() {
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

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getMessageJson() {
    return messageJson;
  }

  public void setMessageJson(String messageJson) {
    this.messageJson = messageJson;
  }


  public Long getSentTimestampSecs() {
    return sentTimestampSecs;
  }

  public void setSentTimestampSecs(Long sentTimestampSecs) {
    this.sentTimestampSecs = sentTimestampSecs;
  }

  public Long getVisibleTimestampSecs() {
    return visibleTimestampSecs;
  }

  public void setVisibleTimestampSecs(Long visibleTimestampSecs) {
    this.visibleTimestampSecs = visibleTimestampSecs;
  }

  public Long getExpiredTimestampSecs() {
    return expiredTimestampSecs;
  }

  public void setExpiredTimestampSecs(Long expiredTimestampSecs) {
    this.expiredTimestampSecs = expiredTimestampSecs;
  }

  public Integer getReceiveCount() {
    return receiveCount;
  }

  public void setReceiveCount(Integer receiveCount) {
    this.receiveCount = receiveCount;
  }

  public Integer getLocalReceiveCount() {
    return localReceiveCount;
  }

  public void setLocalReceiveCount(Integer localReceiveCount) {
    this.localReceiveCount = localReceiveCount;
  }
}
