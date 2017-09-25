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
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.ws.WebServiceError;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class CloudFormationErrorResponse extends CloudFormationMessage implements WebServiceError {

  private String requestId;
  private Error error;

  public CloudFormationErrorResponse( ) {
    set_return( false );
  }

  @Override
  public String toSimpleString( ) {
    return ( error == null ? null : error.getType( ) ) + " error (" + getWebServiceErrorCode( ) + "): " + getWebServiceErrorMessage( );
  }

  @JsonIgnore
  @Override
  public String getWebServiceErrorCode( ) {
    return error == null ? null : error.getCode( );
  }

  @JsonIgnore
  @Override
  public String getWebServiceErrorMessage( ) {
    return error == null ? null : error.getMessage( );
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public Error getError( ) {
    return error;
  }

  public void setError( Error error ) {
    this.error = error;
  }
}
