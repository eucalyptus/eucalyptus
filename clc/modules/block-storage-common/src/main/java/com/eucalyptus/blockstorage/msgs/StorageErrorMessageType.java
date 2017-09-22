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
package com.eucalyptus.blockstorage.msgs;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.component.annotation.ComponentMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( Storage.class )
public class StorageErrorMessageType extends BaseMessage {

  private String code;
  private String message;
  private Integer httpCode;
  private String requestId;

  public StorageErrorMessageType( ) { }

  public StorageErrorMessageType( String code, String message, Integer httpCode, String requestId ) {
    this.code = code;
    this.message = message;
    this.requestId = requestId;
    this.httpCode = httpCode;
  }

  public String toString( ) {
    return "StrorageErrorMessage:" + message;
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

  public Integer getHttpCode( ) {
    return httpCode;
  }

  public void setHttpCode( Integer httpCode ) {
    this.httpCode = httpCode;
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }
}
