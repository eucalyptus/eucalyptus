/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.ws;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.messaging.MessagingException;
import com.eucalyptus.auth.euare.common.msgs.ErrorResponseType;
import com.eucalyptus.auth.euare.common.msgs.ErrorType;
import com.eucalyptus.auth.euare.EuareException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ComponentNamed
public class EuareReplyQueue {
  private static Logger                                 LOG                   = Logger.getLogger( EuareReplyQueue.class );

  public void handle( BaseMessage responseMessage ) {
    EventRecord.here( EuareReplyQueue.class, EventType.MSG_REPLY, responseMessage.getCorrelationId( ), responseMessage.getClass( ).getSimpleName( ) ).debug( );
    String corrId = responseMessage.getCorrelationId( );
    try {
      Context context = Contexts.lookup( corrId );
      Channel channel = context.getChannel( );
      Channels.write( channel, responseMessage );
      Contexts.clear(context);
    } catch ( NoSuchContextException e ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message.", e );
      if ( LOG.isDebugEnabled( ) ) {
        LOG.debug( responseMessage );
      }
    }
  }

  public void handle( Throwable exception ) {
    if ( exception instanceof MessagingException && Exceptions.isCausedBy( exception, EucalyptusCloudException.class ) ) {
      HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      String errorName = EuareException.INTERNAL_FAILURE;
      if ( Exceptions.isCausedBy( exception, EuareException.class ) ) {
        EuareException euareException = Exceptions.findCause( exception, EuareException.class );
        status = euareException.getStatus( );
        errorName = euareException.getError( );
      }
      ErrorResponseType errorResp = new ErrorResponseType( );
      BaseMessage payload = null;
      try {
        payload = parsePayload( ((MessagingException)exception).getFailedMessage( ).getPayload( ) );
      } catch ( Exception e ) {
        LOG.error( "Failed to parse payload ", e );
      }
      if ( payload != null ) {
        errorResp.setHttpStatus( status );
        errorResp.setCorrelationId( payload.getCorrelationId( ) );
        errorResp.setRequestId( payload.getCorrelationId( ) );
        ErrorType error = new ErrorType( );
        error.setType( "Sender" );
        error.setCode( errorName );
        error.setMessage( Exceptions.findCause( exception, EucalyptusCloudException.class ).getMessage( ) );
        errorResp.getErrorList( ).add( error );
        this.handle( errorResp );
      }
    }
  }

  private BaseMessage parsePayload( Object payload ) throws Exception {
    if ( payload instanceof BaseMessage ) {
      return ( BaseMessage ) payload;
    } else if ( payload instanceof String ) {
      return ( BaseMessage ) BindingManager.getBinding( "https://iam.amazonaws.com/doc/2010-05-08/" ).fromOM( ( String ) payload );
    }
    return new EucalyptusErrorMessageType( "ReplyQueue", LogUtil.dumpObject( payload ) );
  }

}
