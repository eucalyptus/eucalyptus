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

public class EucalyptusErrorMessageType extends EucalyptusMessage {

  private String source;
  private String message;
  private String requestType = "not available";
  private Throwable exception;

  public EucalyptusErrorMessageType( ) {
  }

  public EucalyptusErrorMessageType( String source, String message ) {
    this.source = source;
    this.message = message;
  }

  public EucalyptusErrorMessageType( String source, BaseMessage msg, String message ) {
    this( source, message );
    regardingUserRequest( msg );
    this.requestType = msg != null ? msg.getClass( ).getSimpleName( ) : this.requestType;
  }

  public String toString( ) {
    return String.format( "SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType );
  }

  public String getSource( ) {
    return source;
  }

  public void setSource( String source ) {
    this.source = source;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getRequestType( ) {
    return requestType;
  }

  public void setRequestType( String requestType ) {
    this.requestType = requestType;
  }

  public Throwable getException( ) {
    return exception;
  }

  public void setException( Throwable exception ) {
    this.exception = exception;
  }
}
