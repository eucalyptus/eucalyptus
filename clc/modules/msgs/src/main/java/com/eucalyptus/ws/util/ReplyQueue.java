/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.Channels;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import com.eucalyptus.records.EventRecord;

public class ReplyQueue {
  private static Logger                                 LOG                   = Logger.getLogger( ReplyQueue.class );
  
  public static void handle( String service, MuleMessage responseMessage, BaseMessage request ) {
    BaseMessage reply = null;
    if ( responseMessage.getExceptionPayload( ) == null ) {
      reply = ( BaseMessage ) responseMessage.getPayload( );
    } else {
      Throwable t = responseMessage.getExceptionPayload( ).getException( );
      t = ( t.getCause() == null ) ? t : t.getCause( );
      reply = new EucalyptusErrorMessageType( service, request, t.getMessage( ) );
    }
    String corrId = reply.getCorrelationId( );
    EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, reply.getCorrelationId( ), reply.getClass( ).getSimpleName( ) ).debug( );    
    try {
      Context context = Contexts.lookup( corrId );
      Channel channel = context.getChannel( );
      Channels.write( channel, reply );
      Contexts.clear(context);
    } catch ( NoSuchContextException e ) {
      LOG.trace( e, e );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public void handle( BaseMessage responseMessage ) {
    EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, responseMessage.getCorrelationId( ), responseMessage.getClass( ).getSimpleName( ) ).debug( );
    String corrId = responseMessage.getCorrelationId( );
    try {
      Context context = Contexts.lookup( corrId );
      Channel channel = context.getChannel( );
      Channels.write( channel, responseMessage );
      Contexts.clear(context);
    } catch ( NoSuchContextException e ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message.", e );
      LOG.debug( responseMessage );
    }
  }

  public void handle( ExceptionMessage exMsg ) {
    EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, exMsg.getPayload( ).getClass( ).getSimpleName( ) ).debug( );
    LOG.trace( "Caught exception while servicing: " + exMsg.getPayload( ) );
    Throwable exception = exMsg.getException( );
    Object payload = null;
    BaseMessage msg = null;
    if ( exception instanceof MessagingException ) {
      MessagingException ex = ( MessagingException ) exception;
      MuleMessage muleMsg = ex.getUmoMessage( );

      if ( payload instanceof VmAllocationInfo ) {
        msg = ( ( VmAllocationInfo ) payload ).getRequest( );
      } else {
        try {
          msg = parsePayload( muleMsg.getPayload( ) );
        } catch ( Exception e ) {
          LOG.error( "Bailing out of error handling: don't have the correlationId for the caller!" );
          LOG.error( e, e );
          return;
        }
      }
      EucalyptusErrorMessageType errMsg = getErrorMessageType( exMsg.getComponentName( ), exMsg.getException( ), msg );
      errMsg.setException( exception.getCause( ) );
      this.handle( errMsg );
    }
  }

  private EucalyptusErrorMessageType getErrorMessageType( final String componentName, Throwable exception, final BaseMessage msg ) {
    EucalyptusErrorMessageType errMsg = null;
    if ( exception != null ) {
      String desc = "";
      LOG.error( exception, exception );
      exception = exception.getCause( ) != null ? exception.getCause( ) : exception;
      for( Throwable e = exception; e != null; e = e.getCause( ) ) {
        desc += "\n" + e.getMessage( );
      }
      errMsg = new EucalyptusErrorMessageType( componentName, msg, desc );
    } else {
      ByteArrayOutputStream exStream = new ByteArrayOutputStream( );
      if(exception != null)
          exception.printStackTrace( new PrintStream( exStream ) );
      errMsg = new EucalyptusErrorMessageType( componentName, msg, "Internal Error: " + exStream.toString( ) );
    }
    return errMsg;
  }

  private BaseMessage parsePayload( Object payload ) throws Exception {
    if ( payload instanceof BaseMessage ) {
      return ( BaseMessage ) payload;
    } else if ( payload instanceof VmAllocationInfo ) {
      return ( ( VmAllocationInfo ) payload ).getRequest( );
    } else if ( !( payload instanceof String ) ) {
      return new EucalyptusErrorMessageType( "ReplyQueue", LogUtil.dumpObject( payload ) );
    } else {
      return ( BaseMessage ) BindingManager.getBinding( "msgs_eucalyptus_com" ).fromOM( ( String ) payload );
    }
  }

}
