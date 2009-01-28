/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.RequestTransactionScript;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.binding.BindingManager;
import edu.ucsb.eucalyptus.util.ReplyCoordinator;
import org.apache.log4j.Logger;
import org.mule.api.*;
import org.mule.message.ExceptionMessage;

import java.io.*;

public class ReplyQueue {

  private static Logger LOG = Logger.getLogger( ReplyQueue.class );

  private static ReplyCoordinator replies = new ReplyCoordinator();

  public void handle( EucalyptusMessage msg ) {
    if ( msg.getCorrelationId() != null && msg.getCorrelationId().length() != 0 )
      replies.putMessage( msg );
  }

  public static EucalyptusMessage getReply( String msgId ) {
    EucalyptusMessage msg = null;
    msg = replies.getMessage( msgId );
    return msg;
  }

  public void handle( ExceptionMessage exMsg ) {
    Throwable exception = exMsg.getException();
    Object payload = null;
    EucalyptusMessage msg = null;
    //:: messaging exceptions are the easiest to handle, deal with it first :://
    if ( exception instanceof MessagingException ) {
      MessagingException ex = ( MessagingException ) exception;
      MuleMessage muleMsg = ex.getUmoMessage();

      if ( payload instanceof RequestTransactionScript ) {
        msg = ( ( RequestTransactionScript ) payload ).getRequestMessage();
      } else {
        try {
          msg = parsePayload( muleMsg.getPayload() );
        } catch ( Exception e ) {
          LOG.error( "Bailing out of error handling: don't have the correlationId for the caller!" );
          LOG.error( e, e );
          return;
        }
      }
      EucalyptusErrorMessageType errMsg = getErrorMessageType( exMsg, msg );
      replies.putMessage( errMsg );
    }
  }

  private EucalyptusErrorMessageType getErrorMessageType( final ExceptionMessage exMsg, final EucalyptusMessage msg ) {
    Throwable exception = exMsg.getException();
    EucalyptusErrorMessageType errMsg = null;
    if ( exception != null ) {
      Throwable e = exMsg.getException().getCause();
      if ( e != null ) {
        errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName(), msg, e.getMessage() );
      }
    }
    if ( errMsg == null ) {
      ByteArrayOutputStream exStream = new ByteArrayOutputStream();
      exception.printStackTrace( new PrintStream( exStream ) );
      errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName(), msg, "Internal Error: \n" + exStream.toString() );
    }
    return errMsg;
  }

  private EucalyptusMessage parsePayload( Object payload ) throws Exception {
    if ( payload instanceof EucalyptusMessage ) {
      return (EucalyptusMessage) payload;
    } else {
      return ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( ( String ) payload );
    }
  }

}
