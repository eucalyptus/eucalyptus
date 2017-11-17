/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;

public class AWSSQSQueueProperties implements ResourceProperties {

  @Property
  private Integer delaySeconds;

  @Property
  private Long maximumMessageSize;

  @Property
  private Integer messageRetentionPeriod;

  @Property
  private String queueName;

  @Property
  private Integer receiveMessageWaitTimeSeconds;

  @Property
  private SQSRedrivePolicy redrivePolicy;

  @Property
  private Integer visibilityTimeout;

  public Integer getDelaySeconds( ) {
    return delaySeconds;
  }

  public void setDelaySeconds( Integer delaySeconds ) {
    this.delaySeconds = delaySeconds;
  }

  public Long getMaximumMessageSize( ) {
    return maximumMessageSize;
  }

  public void setMaximumMessageSize( Long maximumMessageSize ) {
    this.maximumMessageSize = maximumMessageSize;
  }

  public Integer getMessageRetentionPeriod( ) {
    return messageRetentionPeriod;
  }

  public void setMessageRetentionPeriod( Integer messageRetentionPeriod ) {
    this.messageRetentionPeriod = messageRetentionPeriod;
  }

  public String getQueueName( ) {
    return queueName;
  }

  public void setQueueName( String queueName ) {
    this.queueName = queueName;
  }

  public Integer getReceiveMessageWaitTimeSeconds( ) {
    return receiveMessageWaitTimeSeconds;
  }

  public void setReceiveMessageWaitTimeSeconds( Integer receiveMessageWaitTimeSeconds ) {
    this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
  }

  public SQSRedrivePolicy getRedrivePolicy( ) {
    return redrivePolicy;
  }

  public void setRedrivePolicy( SQSRedrivePolicy redrivePolicy ) {
    this.redrivePolicy = redrivePolicy;
  }

  public Integer getVisibilityTimeout( ) {
    return visibilityTimeout;
  }

  public void setVisibilityTimeout( Integer visibilityTimeout ) {
    this.visibilityTimeout = visibilityTimeout;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "delaySeconds", delaySeconds )
        .add( "maximumMessageSize", maximumMessageSize )
        .add( "messageRetentionPeriod", messageRetentionPeriod )
        .add( "queueName", queueName )
        .add( "receiveMessageWaitTimeSeconds", receiveMessageWaitTimeSeconds )
        .add( "redrivePolicy", redrivePolicy )
        .add( "visibilityTimeout", visibilityTimeout )
        .toString( );
  }
}
