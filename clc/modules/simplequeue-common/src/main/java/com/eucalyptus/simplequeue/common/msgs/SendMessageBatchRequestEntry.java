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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SendMessageBatchRequestEntry extends EucalyptusData {

  private String id;
  private String messageBody;
  private Integer delaySeconds;
  @HttpEmbedded( multiple = true )
  private ArrayList<MessageAttribute> messageAttribute = new ArrayList<MessageAttribute>( );

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
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

  public ArrayList<MessageAttribute> getMessageAttribute( ) {
    return messageAttribute;
  }

  public void setMessageAttribute( ArrayList<MessageAttribute> messageAttribute ) {
    this.messageAttribute = messageAttribute;
  }
}
