/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
