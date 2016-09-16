/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.tokens;

import static org.hamcrest.Matchers.notNullValue;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Parameters;

/**
 *
 */
public class TokensException extends EucalyptusCloudException {
  private static final long serialVersionUID = 1L;

  enum Code {
    AccessDenied( HttpResponseStatus.FORBIDDEN, "Sender" ),
    ExpiredTokenException,
    IDPCommunicationError,
    InvalidAction,
    InvalidIdentityToken,
    InvalidParameterValue,
    MissingAuthenticationToken( HttpResponseStatus.FORBIDDEN, "Sender" ),
    ServiceUnavailable( HttpResponseStatus.SERVICE_UNAVAILABLE, "Receiver" ),
    ValidationError,
    ;

    private final HttpResponseStatus status;
    private final String type;

    private Code( ) {
      this( HttpResponseStatus.BAD_REQUEST, "Sender" );
    }

    private Code( final HttpResponseStatus status, final String type ) {
      this.status = status;
      this.type = type;
    }

    private HttpResponseStatus getHttpStatus() {
      return status;
    }

    private String getType() {
      return type;
    }
  }

  private final Code code;

  public TokensException( final Code code,
                          final String message ) {
    super( message );
    this.code = Parameters.checkParam( "code", code, notNullValue( ) );
  }

  public HttpResponseStatus getStatus( ) {
    return code.getHttpStatus( );
  }

  public String getError( ) {
    return code.name( );
  }

  public String getType( ) {
    return code.getType( );
  }
}
