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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.ws.WebServiceError;

public class ErrorResponse extends ComputeMessage implements WebServiceError {

  private String requestId;
  private ArrayList<ErrorDetail> error = new ArrayList<ErrorDetail>( );

  public ErrorResponse( ) {
    set_return( false );
  }

  @Override
  public String toSimpleString( ) {
    final ErrorDetail at = error.get( 0 );
    return ( at == null ? null : at.getType( ) ) + " error (" + getWebServiceErrorCode( ) + "): " + getWebServiceErrorMessage( );
  }

  @Override
  public String getWebServiceErrorCode( ) {
    final ErrorDetail at = error.get( 0 );
    return ( at == null ? null : at.getCode( ) );
  }

  @Override
  public String getWebServiceErrorMessage( ) {
    final ErrorDetail at = error.get( 0 );
    return ( at == null ? null : at.getMessage( ) );
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public ArrayList<ErrorDetail> getError( ) {
    return error;
  }

  public void setError( ArrayList<ErrorDetail> error ) {
    this.error = error;
  }
}
