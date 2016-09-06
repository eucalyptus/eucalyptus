/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.simplequeue.SimpleQueueErrorResponse;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class SimpleQueueErrorHandler extends ErrorHandlerSupport {
  private static final Logger LOG = Logger.getLogger( SimpleQueueErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  public SimpleQueueErrorHandler() {
    super( LOG, SimpleQueueQueryBinding.SIMPLEQUEUE_DEFAULT_NAMESPACE, INTERNAL_FAILURE );
  }

  @Override
  protected BaseMessage buildErrorResponse( final String correlationId,
                                            final Role role,
                                            final String code,
                                            final String message ) {
    final SimpleQueueErrorResponse errorResp = new SimpleQueueErrorResponse( );
    errorResp.setCorrelationId( correlationId );
    errorResp.setRequestId( correlationId );
    final com.eucalyptus.simplequeue.Error error = new com.eucalyptus.simplequeue.Error( );
    error.setType( role == Role.Receiver ? "Receiver" : "Sender" );
    error.setCode( code );
    error.setMessage( message );
    errorResp.getError().add( error );
    return errorResp;
  }
}
