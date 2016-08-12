/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.api.MessagingException;
import org.mule.message.ExceptionMessage;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
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
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

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

  public void handle( final org.springframework.messaging.MessagingException messagingEx ) {
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

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void handle( final ExceptionMessage exMsg ) {
    EventRecord.here( getClass(), EventType.MSG_REPLY, exMsg.getPayload().getClass().getSimpleName() ).debug();
    final Throwable exception = exMsg.getException( );
    if ( exception instanceof MessagingException && exception.getCause( ) instanceof EucalyptusCloudException ) {
      try {
        final EucalyptusCloudException cloudException = (EucalyptusCloudException) exception.getCause( );
        final BaseMessage payload = parsePayload( exMsg.getPayload( ) );
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
      final BaseMessage errorResp = buildFatalResponse( exception );
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
