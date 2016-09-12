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
package com.eucalyptus.simpleworkflow.ws;

import org.apache.log4j.Logger;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowErrorResponse;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@ComponentNamed
public class SimpleWorkflowErrorHandler extends ErrorHandlerSupport {
  private static final Logger LOG = Logger.getLogger( SimpleWorkflowErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  public SimpleWorkflowErrorHandler( ) {
    super( LOG, null, INTERNAL_FAILURE );
  }

  @Override
  protected BaseMessage buildErrorResponse( final String correlationId,
                                            final Role role,
                                            final String code,
                                            final String message ) {
    final SimpleWorkflowErrorResponse errorResponse = new SimpleWorkflowErrorResponse( message, code );
    errorResponse.setCorrelationId( correlationId );
    return errorResponse;
  }
}
