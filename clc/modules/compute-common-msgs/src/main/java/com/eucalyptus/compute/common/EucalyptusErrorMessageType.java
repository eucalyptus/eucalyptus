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
package com.eucalyptus.compute.common;

import com.eucalyptus.util.Classes;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class EucalyptusErrorMessageType extends ComputeMessage {

  private String source;
  private String message;
  private String requestType = "not available";
  private Throwable exception;

  public EucalyptusErrorMessageType( ) {
  }

  public EucalyptusErrorMessageType( String source, String message ) {
    this.source = source;
    this.message = message;
  }

  public EucalyptusErrorMessageType( String source, BaseMessage msg, String message ) {
    this( source, message );
    regardingUserRequest( msg );
    this.requestType = Classes.simpleName( msg, this.requestType );
  }

  public String toString( ) {
    return String.format( "SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType );
  }

  public String getSource( ) {
    return source;
  }

  public void setSource( String source ) {
    this.source = source;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getRequestType( ) {
    return requestType;
  }

  public void setRequestType( String requestType ) {
    this.requestType = requestType;
  }

  public Throwable getException( ) {
    return exception;
  }

  public void setException( Throwable exception ) {
    this.exception = exception;
  }
}
