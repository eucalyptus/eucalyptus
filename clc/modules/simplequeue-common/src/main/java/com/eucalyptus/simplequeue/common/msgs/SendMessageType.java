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
package com.eucalyptus.simplequeue.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;

public class SendMessageType extends SimpleQueueMessage implements QueueUrlGetterSetter {

  private String queueUrl;
  private String messageBody;
  private Integer delaySeconds;
  @HttpEmbedded( multiple = true )
  private ArrayList<Attribute> attribute = new ArrayList<Attribute>( );
  @HttpEmbedded( multiple = true )
  private ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>( );

  public String getQueueUrl( ) {
    return queueUrl;
  }

  public void setQueueUrl( String queueUrl ) {
    this.queueUrl = queueUrl;
  }

  public String getMessageBody( ) {
    return messageBody;
  }

  public void setMessageBody( String messageBody ) {
    this.messageBody = messageBody;
  }

  public Integer getDelaySeconds( ) {
    return delaySeconds;
  }

  public void setDelaySeconds( Integer delaySeconds ) {
    this.delaySeconds = delaySeconds;
  }

  public ArrayList<Attribute> getAttribute( ) {
    return attribute;
  }

  public void setAttribute( ArrayList<Attribute> attribute ) {
    this.attribute = attribute;
  }

  public ArrayList<MessageAttribute> getMessageAttribute( ) {
    return messageAttribute;
  }

  public void setMessageAttribute( ArrayList<MessageAttribute> messageAttribute ) {
    this.messageAttribute = messageAttribute;
  }
}
