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
package com.eucalyptus.tokens.common.msgs;

import java.util.ArrayList;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.WebServiceError;

public class TokensErrorResponseType extends TokenMessage implements WebServiceError {
  private String requestId;
  private HttpResponseStatus httpStatus;
  private ArrayList<TokensErrorType> errors = new ArrayList<TokensErrorType>( );

  public TokensErrorResponseType() {
    set_return( false );
  }

  @Override
  public String toSimpleString() {
    final TokensErrorType at = errors.get( 0 );
    return ( at == null ? null : at.getType( ) ) + " error (" + getWebServiceErrorCode( ) + "): " + getWebServiceErrorMessage( );
  }

  @Override
  public String getWebServiceErrorCode() {
    final TokensErrorType at = errors.get( 0 );
    return ( at == null ? null : at.getCode( ) );
  }

  @Override
  public String getWebServiceErrorMessage() {
    final TokensErrorType at = errors.get( 0 );
    return ( at == null ? null : at.getMessage( ) );
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public HttpResponseStatus getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus( HttpResponseStatus httpStatus ) {
    this.httpStatus = httpStatus;
  }

  public ArrayList<TokensErrorType> getErrors() {
    return errors;
  }

  public void setErrors( ArrayList<TokensErrorType> errors ) {
    this.errors = errors;
  }
}
