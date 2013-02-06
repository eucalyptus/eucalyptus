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
package com.eucalyptus.loadbalancing.ws;

import com.eucalyptus.ws.Role;
import com.eucalyptus.loadbalancing.ErrorResponse;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class LoadBalancingErrorHandler extends ErrorHandlerSupport {
  private static final Logger LOG = Logger.getLogger( LoadBalancingErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";  //TODO:GEN2OOLS: Verify / replace default error code for service

  public LoadBalancingErrorHandler( ) {
    super( LOG, LoadBalancingQueryBinding.LOADBALANCING_DEFAULT_NAMESPACE, INTERNAL_FAILURE );
  }

  @Override
  protected BaseMessage buildErrorResponse( final String correlationId,
                                            final Role role,
                                            final String code,
                                            final String message ) {
    final ErrorResponse errorResp = new ErrorResponse( ); //TODO:GEN2OOLS: Ensure this is a message and has appropriate binding
    errorResp.setCorrelationId( correlationId );
    errorResp.setRequestId( correlationId );
    final com.eucalyptus.loadbalancing.Error error = new com.eucalyptus.loadbalancing.Error( );
    error.setType( role == Role.Receiver ? "Receiver" : "Sender" ); //TODO:GEN2OOLS: Customize type for service
    error.setCode( code );
    error.setMessage( message );
    errorResp.getError().add( error );
    return errorResp;
  }
}
