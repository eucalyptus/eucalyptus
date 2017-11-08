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
package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.messaging.MessagingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.HasHttpStatusCode;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.protocol.QueryBindingInfo;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessageSupplier;
import edu.ucsb.eucalyptus.msgs.ErrorDetail;
import edu.ucsb.eucalyptus.msgs.ErrorResponse;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

/**
 * Support for service error handler implementations
 */
public abstract class ErrorHandlerSupport {

  private final Logger LOG;
  private final String namespace;
  private final String defaultCode;
  
  protected ErrorHandlerSupport( final Logger logger,
                                 final String namespace,
                                 final String defaultCode ) {
    this.LOG = logger;  
    this.namespace = namespace;
    this.defaultCode = defaultCode;
  }

  public void handle( final MessagingException messagingEx ) {
    final EucalyptusCloudException cloudException =
        Exceptions.findCause( messagingEx, EucalyptusCloudException.class );
    final Object payloadObject = messagingEx.getFailedMessage( ).getPayload( );
    if( cloudException != null ) {
      try {
        final BaseMessage payload = parsePayload( payloadObject );
        final HttpResponseStatus status;
        final Role role;
        final String code;
        if ( cloudException instanceof EucalyptusWebServiceException ) {
          final EucalyptusWebServiceException webServiceException = (EucalyptusWebServiceException) cloudException;
          role = webServiceException.getRole();
          code = webServiceException.getCode();
        } else {
          role = Role.Receiver;
          code = defaultCode;
        }
        final Optional<Integer> statusCodeOptional = getHttpResponseStatus( cloudException );
        status = !statusCodeOptional.isPresent( ) ?
            HttpResponseStatus.INTERNAL_SERVER_ERROR :
            new HttpResponseStatus( statusCodeOptional.get( ), code );
        final BaseMessage errorResp = buildErrorResponse(
            payload.getCorrelationId( ),
            role,
            code,
            cloudException.getMessage()
        );
        Contexts.response( new BaseMessageSupplier( errorResp, status ) );
      } catch ( final PayloadParseException e ) {
        LOG.error( "Failed to parse payload ", e.getCause() );
      }
    } else {
      final BaseMessage errorResp = buildFatalResponse( messagingEx.getCause( )==null?messagingEx:messagingEx.getCause( ) );
      Contexts.response( new BaseMessageSupplier( errorResp, HttpResponseStatus.INTERNAL_SERVER_ERROR ) );
    }
  }

  private BaseMessage buildFatalResponse( Throwable exception ) {
    final ErrorResponse errorResponse = new ErrorResponse( );
    ErrorDetail error = new ErrorDetail( );
    error.setCode( HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode( ) );
    error.setMessage( exception.getMessage( ) );
    error.setType( defaultCode );
    if ( Logs.isDebug( ) ) {
      error.setStackTrace( Exceptions.string( exception ) );
    }
    return errorResponse;
  }

  /**
   * Implementations construct a service specific message from the parameters.
   * 
   * @param correlationId The request message identifier
   * @param role The role for the error
   * @param code The code for the error
   * @param message The message for the error
   * @return The error message
   */
  protected abstract BaseMessage buildErrorResponse( String correlationId,
                                                     Role role,
                                                     String code,
                                                     String message );

  protected static Optional<Integer> getHttpResponseStatus( final Throwable t ) {
    final QueryBindingInfo info = Ats.inClassHierarchy( t.getClass() ).get( QueryBindingInfo.class );
    final Optional<Integer> status = info == null ?
        Optional.<Integer>absent( ) :
        Optional.of( info.statusCode( ) );
    return Iterables.tryFind( Exceptions.causes( t ), Predicates.instanceOf( HasHttpStatusCode.class ) )
            .transform( Functions.compose(
                HasHttpStatusCode.Utils.httpStatusCode( ),
                CollectionUtils.cast( HasHttpStatusCode.class ) ) )
        .or( status );
  }

  private BaseMessage parsePayload( final Object payload ) throws PayloadParseException {
    if ( payload instanceof BaseMessage ) {
      return ( BaseMessage ) payload;
    } else if ( payload instanceof String ) {
      try {
        return ( BaseMessage ) BindingManager.getBinding( namespace ).fromOM( ( String ) payload );
      } catch ( Exception e ) {
        throw new PayloadParseException( e );
      }
    }
    return new EucalyptusErrorMessageType( getClass().getSimpleName(), LogUtil.dumpObject( payload ) );
  }

  private static final class PayloadParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public PayloadParseException( final Throwable cause ) {
      super(cause);
    }
  }
}
