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

public class BatchResultErrorEntry extends EucalyptusData {

  private String id;
  private String code;
  private String message;
  private Boolean senderFault;

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getCode( ) {
    return code;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public Boolean getSenderFault( ) {
    return senderFault;
  }

  public void setSenderFault( Boolean senderFault ) {
    this.senderFault = senderFault;
  }
}
