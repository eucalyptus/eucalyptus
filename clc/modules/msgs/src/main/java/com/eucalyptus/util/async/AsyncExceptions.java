/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * <p/>
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
