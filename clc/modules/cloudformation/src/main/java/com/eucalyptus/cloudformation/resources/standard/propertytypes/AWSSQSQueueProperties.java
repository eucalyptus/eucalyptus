/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
