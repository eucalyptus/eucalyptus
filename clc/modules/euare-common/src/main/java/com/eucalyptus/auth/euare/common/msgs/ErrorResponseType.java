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
package com.eucalyptus.auth.euare.common.msgs;

import java.util.ArrayList;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.WebServiceError;

public class ErrorResponseType extends EuareMessage implements WebServiceError {

  private String requestId;
  private HttpResponseStatus httpStatus;
  private ArrayList<ErrorType> errorList = new ArrayList<ErrorType>( );

  public ErrorResponseType( ) {
    set_return( false );
  }

  @Override
  public String toSimpleString( ) {
    final ErrorType at = errorList.get( 0 );
    return ( at == null ? null : at.getType( ) ) + " error (" + getWebServiceErrorCode( ) + "): " + getWebServiceErrorMessage( );
  }

  @Override
  public String getWebServiceErrorCode( ) {
    final ErrorType at = errorList.get( 0 );
    return ( at == null ? null : at.getCode( ) );
  }

  @Override
  public String getWebServiceErrorMessage( ) {
    final ErrorType at = errorList.get( 0 );
    return ( at == null ? null : at.getMessage( ) );
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public HttpResponseStatus getHttpStatus( ) {
    return httpStatus;
  }

  public void setHttpStatus( HttpResponseStatus httpStatus ) {
    this.httpStatus = httpStatus;
  }

  public ArrayList<ErrorType> getErrorList( ) {
    return errorList;
  }

  public void setErrorList( ArrayList<ErrorType> errorList ) {
    this.errorList = errorList;
  }
}
