/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal.ws;

import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.portal.common.model.Ec2ReportsError;
import com.eucalyptus.portal.common.model.Ec2ReportsErrorResponse;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

@ComponentNamed
public class Ec2ReportsServiceErrorHandler  extends ErrorHandlerSupport {
  private static final Logger LOG = Logger.getLogger( Ec2ReportsServiceErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  public Ec2ReportsServiceErrorHandler( ) {
    super( LOG, null, INTERNAL_FAILURE );
  }

  @Override
  protected BaseMessage buildErrorResponse(final String correlationId,
                                           final Role role,
                                           final String code,
                                           final String message ) {
    final Ec2ReportsErrorResponse errorResp = new Ec2ReportsErrorResponse( );
    errorResp.setCorrelationId( correlationId );
    errorResp.setRequestId( correlationId );
    final Ec2ReportsError error = new Ec2ReportsError( );
    error.setType( role == Role.Receiver ? "Receiver" : "Sender" );
    error.setCode( code );
    error.setMessage( message );
    errorResp.getError().add( error );
    return errorResp;
  }
}
