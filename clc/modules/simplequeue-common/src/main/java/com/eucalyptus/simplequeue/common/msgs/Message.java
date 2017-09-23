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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Message extends EucalyptusData {

  private String messageId;
  private String receiptHandle;
  private String mD5OfBody;
  private String mD5OfMessageAttributes;
  private String body;
  private ArrayList<Attribute> attribute = new ArrayList<Attribute>( );
  private ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>( );

  public String getMessageId( ) {
    return messageId;
  }

  public void setMessageId( String messageId ) {
    this.messageId = messageId;
  }

  public String getReceiptHandle( ) {
    return receiptHandle;
  }

  public void setReceiptHandle( String receiptHandle ) {
    this.receiptHandle = receiptHandle;
  }

  public String getmD5OfBody( ) {
    return mD5OfBody;
  }

  public void setmD5OfBody( String mD5OfBody ) {
    this.mD5OfBody = mD5OfBody;
  }

  public String getmD5OfMessageAttributes( ) {
    return mD5OfMessageAttributes;
  }

  public void setmD5OfMessageAttributes( String mD5OfMessageAttributes ) {
    this.mD5OfMessageAttributes = mD5OfMessageAttributes;
  }

  public String getBody( ) {
    return body;
  }

  public void setBody( String body ) {
    this.body = body;
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
