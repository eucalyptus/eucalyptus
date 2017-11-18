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

package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.messaging.MessagingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.HasRequest;

@SuppressWarnings( "Guava" )
@ComponentNamed
public class ReplyQueue {
  private static Logger LOG = Logger.getLogger( ReplyQueue.class );

  /**
   *
   */
  public String handle( BaseMessage responseMessage ) {
    Contexts.response( responseMessage );
    return null;
  }
  
  public void handle( final MessagingException messagingEx ) {
    final EucalyptusWebServiceException webServiceException =
        Exceptions.findCause( messagingEx, EucalyptusWebServiceException.class );
    Object payload = messagingEx.getFailedMessage( ).getPayload( );
    BaseMessage msg = convert( payload );
    if( msg != null ) {
      if ( webServiceException != null ) {
        final Optional<Integer> statusCodeOptional = ErrorHandlerSupport.getHttpResponseStatus( webServiceException );
        final HttpResponseStatus status = !statusCodeOptional.isPresent( ) ?
            HttpResponseStatus.INTERNAL_SERVER_ERROR :
            new HttpResponseStatus( statusCodeOptional.get(), "" );
        Contexts.response( new ExceptionResponseType( msg, webServiceException.getCode( ), webServiceException.getMessage( ), status, webServiceException )  );
      } else {
        final Throwable responseEx = MoreObjects.firstNonNull( messagingEx.getCause( ), messagingEx );
        Contexts.response( new ExceptionResponseType( msg, responseEx.getMessage( ), HttpResponseStatus.NOT_ACCEPTABLE, responseEx )  );
      }
    } else {
      LOG.error( "Failed to identify request context for received error: " + messagingEx.toString( ) );
      final Throwable cause = messagingEx.getCause( );
      Contexts.responseError( new WebServicesException(
          "Failed to identify request context for received error: " + messagingEx.toString( ) + " while handling error: " + cause.getMessage( ),
          cause,
          HttpResponseStatus.NOT_ACCEPTABLE ) );
    }
  }

  private BaseMessage convert( Object payload ) {
    BaseMessage ret = null;
    if ( payload instanceof BaseMessage ) {
      ret = ( BaseMessage ) payload;
    } else if ( payload instanceof HasRequest ) {
      ret = ( ( HasRequest ) payload ).getRequest( );
    }
    return ret;
  }
  
}
