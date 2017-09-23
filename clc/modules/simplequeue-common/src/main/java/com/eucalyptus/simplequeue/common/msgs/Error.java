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

public class Error extends EucalyptusData {

  private String type;
  private String code;
  private String message;
  private ErrorDetail detail = new ErrorDetail( );

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
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

  public ErrorDetail getDetail( ) {
    return detail;
  }

  public void setDetail( ErrorDetail detail ) {
    this.detail = detail;
  }
}
