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
package com.eucalyptus.tokens.ws;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.messaging.MessagingException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.tokens.TokensErrorResponseType;
import com.eucalyptus.tokens.TokensErrorType;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.tokens.TokensException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
@ComponentNamed
public class TokensErrorHandler {
  private static final Logger LOG = Logger.getLogger( TokensErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  public void handle( final MessagingException exception ) {
    final Object payloadObject = exception.getFailedMessage( ).getPayload( );
    final EucalyptusCloudException cloudException = Exceptions.findCause( exception, EucalyptusCloudException.class );
    EventRecord.here( TokensErrorHandler.class, EventType.MSG_REPLY, payloadObject.getClass( ).getSimpleName( ) ).debug( );
    LOG.trace( "Caught exception while servicing: " + payloadObject );
    if ( cloudException != null ) {
      try {
        final BaseMessage payload = parsePayload( payloadObject );
        final TokensErrorResponseType errorResp = new TokensErrorResponseType( );
        final HttpResponseStatus status;
        final String code;
        final String type;
        if ( cloudException instanceof TokensException) {
          final TokensException tokensException = (TokensException) cloudException;
          status = tokensException.getStatus();
          code = tokensException.getError();
          type = tokensException.getType();
        } else {
          status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
          code = INTERNAL_FAILURE;
          type = "Receiver";
        }
        errorResp.setHttpStatus( status );
        errorResp.setCorrelationId( payload.getCorrelationId( ) );
        errorResp.setRequestId( payload.getCorrelationId( ) );
        final TokensErrorType error = new TokensErrorType( );
        error.setType( type );
        error.setCode( code );
        error.setMessage( cloudException.getMessage() );
        errorResp.getErrors().add( error );
        Contexts.response( errorResp );
      } catch ( final PayloadParseException e ) {
        LOG.error( "Failed to parse payload ", e.getCause() );
      }
    } else {
      LOG.error( "Unable to handle exception", exception );
    }
  }

  private static BaseMessage parsePayload( final Object payload ) throws PayloadParseException {
    if ( payload instanceof BaseMessage ) {
      return ( BaseMessage ) payload;
    } else if ( payload instanceof String ) {
      try {
        return ( BaseMessage ) BindingManager.getBinding( BindingManager.sanitizeNamespace( TokensQueryBinding.STS_DEFAULT_NAMESPACE ) ).fromOM( ( String ) payload );
      } catch ( Exception e ) {
        throw new PayloadParseException( e );
      }
    }
    return new EucalyptusErrorMessageType( "TokensErrorHandler", LogUtil.dumpObject( payload ) );
  }

  private static final class PayloadParseException extends Exception {
    public PayloadParseException( final Throwable cause ) {
      super(cause);
    }
  }
}
