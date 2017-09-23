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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SendMessageBatchResultEntry extends EucalyptusData {

  private String id;
  private String messageId;
  private String mD5OfMessageBody;
  private String mD5OfMessageAttributes;

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getMessageId( ) {
    return messageId;
  }

  public void setMessageId( String messageId ) {
    this.messageId = messageId;
  }

  public String getmD5OfMessageBody( ) {
    return mD5OfMessageBody;
  }

  public void setmD5OfMessageBody( String mD5OfMessageBody ) {
    this.mD5OfMessageBody = mD5OfMessageBody;
  }

  public String getmD5OfMessageAttributes( ) {
    return mD5OfMessageAttributes;
  }

  public void setmD5OfMessageAttributes( String mD5OfMessageAttributes ) {
    this.mD5OfMessageAttributes = mD5OfMessageAttributes;
  }
}
