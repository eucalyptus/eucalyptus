/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.util.async;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.WebServiceError;
import com.eucalyptus.ws.protocol.QueryBindingInfo;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 *
 */
public class AsyncExceptions {

  /**
   * Extract a web service error message from the given throwable.
   *
   * @param throwable The possibly web service caused throwable
   * @param defaultMessage The message to use if a service message is not found
   * @return The message or the default message
   */
  public static String asWebServiceErrorMessage( final Throwable throwable, final String defaultMessage ) {
    String message = defaultMessage;
    final Optional<AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( throwable );
    if ( serviceErrorOption.isPresent( ) ) {
      message = serviceErrorOption.get( ).getMessage( );
    }
    return message;
  }

  /**
   * Test if the given throwable was caused by a web service error with the specified code.
   *
   * @param throwable The possibly wer service caused throwable.
   * @param code The error code to test for
   * @return True if the throwable was caused by a web service error with the given code
   */
  public static boolean isWebServiceErrorCode( final Throwable throwable, final String code ) {
    boolean codeMatch = false;
    final Optional<AsyncWebServiceError> serviceErrorOption = AsyncExceptions.asWebServiceError( throwable );
    if ( serviceErrorOption.isPresent( ) ) {
      codeMatch = code.equals( serviceErrorOption.get( ).getCode( ) );
    }
    return codeMatch;
  }

  public static Optional<AsyncWebServiceError> asWebServiceError( final Throwable throwable ) {
    Optional<AsyncWebServiceError> error = Optional.absent( );

    // local
    final EucalyptusWebServiceException serviceException =
        Exceptions.findCause( throwable, EucalyptusWebServiceException.class );
    if ( serviceException != null ) {
      final QueryBindingInfo info = Ats.inClassHierarchy( serviceException.getClass( ) ).get( QueryBindingInfo.class );
      error = Optional.of( new AsyncWebServiceError(
          info == null ? 500 : info.statusCode( ),
          serviceException.getCode( ),
          serviceException.getMessage( ) ) );
    }

    // remote
    if ( !error.isPresent( ) ) {
      final EucalyptusRemoteFault remoteFault =
          Exceptions.findCause( throwable, EucalyptusRemoteFault.class );
      if ( remoteFault != null ) {
        final Integer status =
            Objects.firstNonNull( remoteFault.getStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode( ) );
        final String code = remoteFault.getFaultCode();
        final String message = remoteFault.getFaultDetail();
        error = Optional.of( new AsyncWebServiceError( status, code, message ) );
      }
    }

    // also remote ...
    if ( !error.isPresent( ) ) {
      final FailedRequestException failedRequestException =
          Exceptions.findCause( throwable, FailedRequestException.class );
      if ( failedRequestException != null && failedRequestException.getRequest( ) instanceof WebServiceError ) {
        final WebServiceError webServiceError = failedRequestException.getRequest( );
        final String code = webServiceError.getWebServiceErrorCode( );
        final String message = webServiceError.getWebServiceErrorMessage( );
        if ( code != null && message != null ) {
          error = Optional.of( new AsyncWebServiceError( 0, code, message ) );
        }
      }
    }

    return error;
  }

  public static final class AsyncWebServiceError {
    private final int httpErrorCode;
    private final String code;
    private final String message;

    public AsyncWebServiceError( final int httpErrorCode,
                                 final String code,
                                 final String message ) {
      this.code = code;
      this.httpErrorCode = httpErrorCode;
      this.message = message;
    }

    public int getHttpErrorCode( ) {
      return httpErrorCode;
    }

    public String getCode( ) {
      return code;
    }

    public String getMessage( ) {
      return message;
    }
  }
}
