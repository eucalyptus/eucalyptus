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
package com.eucalyptus.cloudformation.ws;

import com.eucalyptus.cloudformation.common.msgs.CloudFormationErrorResponse;
import com.eucalyptus.cloudformation.common.msgs.Error;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.ws.Role;
import com.eucalyptus.ws.util.ErrorHandlerSupport;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

@ComponentNamed
public class CloudFormationErrorHandler extends ErrorHandlerSupport {
  private static final Logger LOG = Logger.getLogger( CloudFormationErrorHandler.class );
  private static final String INTERNAL_FAILURE = "InternalFailure";

  public CloudFormationErrorHandler( ) {
    super( LOG, CloudFormationQueryBinding.CLOUDFORMATION_DEFAULT_NAMESPACE, INTERNAL_FAILURE );
  }

  @Override
  protected BaseMessage buildErrorResponse( final String correlationId,
                                            final Role role,
                                            final String code,
                                            final String message ) {
    final CloudFormationErrorResponse errorResp = new CloudFormationErrorResponse( );
    errorResp.setCorrelationId( correlationId );
    errorResp.setRequestId( correlationId );
    final Error error = new Error( );
    error.setType( role == Role.Receiver ? "Receiver" : "Sender" );
    error.setCode( code );
    error.setMessage( message );
    errorResp.setError( error );
    return errorResp;
  }
}
