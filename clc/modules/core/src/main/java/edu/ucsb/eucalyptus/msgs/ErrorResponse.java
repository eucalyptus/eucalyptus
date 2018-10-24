/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package edu.ucsb.eucalyptus.msgs;

import java.util.ArrayList;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.component.id.Eucalyptus;

@ComponentMessage( Eucalyptus.class )
public class ErrorResponse extends BaseMessage {

  private String requestId;
  private ArrayList<ErrorDetail> error = new ArrayList<ErrorDetail>( );

  public ErrorResponse( ) {
    set_return( false );
  }

  @Override
  public String toSimpleString( ) {
    final ErrorDetail at = error.get( 0 );
    final ErrorDetail at1 = error.get( 0 );
    final ErrorDetail at2 = error.get( 0 );
    return ( at == null ? null : at.getType( ) ) + " error (" + String.valueOf( ( at1 == null ? null : at1.getCode( ) ) ) + "): " + ( at2 == null ? null : at2.getMessage( ) );
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public ArrayList<ErrorDetail> getError( ) {
    return error;
  }

  public void setError( ArrayList<ErrorDetail> error ) {
    this.error = error;
  }
}
