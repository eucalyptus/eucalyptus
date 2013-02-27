/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.service.ws;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.api.MessagingException;
import org.mule.message.ExceptionMessage;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.reporting.service.ReportingErrorResponseType;
import com.eucalyptus.reporting.service.ReportingErrorType;
import com.eucalyptus.reporting.service.ReportingException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

/**
 *
 */
public class ReportingErrorHandler {
  private static final Logger log = Logger.getLogger( ReportingErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void handle( ExceptionMessage exMsg ) {
    EventRecord.here(ReportingErrorHandler.class, EventType.MSG_REPLY, exMsg.getPayload().getClass().getSimpleName()).debug( );
    log.trace("Caught exception while servicing: " + exMsg.getPayload());
    Throwable exception = exMsg.getException( );
    if ( exception instanceof MessagingException && exception.getCause( ) instanceof EucalyptusCloudException ) {
      try {
        final EucalyptusCloudException cloudException = (EucalyptusCloudException) exception.getCause( );
        final BaseMessage payload = parsePayload( ( ( MessagingException ) exception ).getUmoMessage( ).getPayload( ) );
        final ReportingErrorResponseType errorResp = new ReportingErrorResponseType( );
        final HttpResponseStatus status;
        final String code;
        if ( cloudException instanceof ReportingException ) {
          final ReportingException reportingException = (ReportingException) cloudException;
          status = reportingException.getStatus();
          code = reportingException.getError();
        } else {
          status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
          code = INTERNAL_FAILURE;
        }
        errorResp.setHttpStatus( status );
        errorResp.setCorrelationId( payload.getCorrelationId( ) );
        errorResp.setRequestId( payload.getCorrelationId( ) );
        final ReportingErrorType error = new ReportingErrorType( );
        error.setType( "Receiver" );
        error.setCode( code );
        error.setMessage( cloudException.getMessage() );
        errorResp.getErrors().add( error );
        Contexts.response(errorResp);
      } catch ( final PayloadParseException e ) {
        log.error( "Failed to parse payload ", e.getCause() );
      }
    } else {
      log.error( "Unable to handle exception", exception );
    }
  }

  private static BaseMessage parsePayload( final Object payload ) throws PayloadParseException {
    if ( payload instanceof BaseMessage ) {
      return ( BaseMessage ) payload;
    } else if ( payload instanceof String ) {
      try {
        return ( BaseMessage ) BindingManager.getBinding( ReportingQueryBinding.REPORTING_DEFAULT_NAMESPACE ).fromOM( ( String ) payload );
      } catch ( Exception e ) {
        throw new PayloadParseException( e );
      }
    }
    return new EucalyptusErrorMessageType( "ReportingErrorHandler", LogUtil.dumpObject( payload ) );
  }

  private static final class PayloadParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public PayloadParseException( final Throwable cause ) {
      super(cause);
    }
  }

}
