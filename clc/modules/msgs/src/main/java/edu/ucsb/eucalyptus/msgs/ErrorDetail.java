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
package edu.ucsb.eucalyptus.msgs;

public class ErrorDetail extends EucalyptusData {

  private String type;
  private Integer code;
  private String message;
  private String stackTrace;

  public ErrorDetail( ) {
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public Integer getCode( ) {
    return code;
  }

  public void setCode( Integer code ) {
    this.code = code;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getStackTrace( ) {
    return stackTrace;
  }

  public void setStackTrace( String stackTrace ) {
    this.stackTrace = stackTrace;
  }
}
