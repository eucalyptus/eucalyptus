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
