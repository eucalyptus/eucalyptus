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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;

import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.binding.BindingManager;

import edu.ucsb.eucalyptus.cloud.RequestTransactionScript;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class ReplyQueue {

  private static Logger                                 LOG                   = Logger.getLogger( ReplyQueue.class );
  //TODO: measure me
  private static int                                    MAP_CAPACITY          = 64;
  private static int                                    MAP_NUM_CONCURRENT    = MAP_CAPACITY / 2;
  private static float                                  MAP_BIN_AVG_THRESHOLD = 1.0f;
  private static long                                   MAP_GET_WAIT_MS       = 10;
  private static ConcurrentMap<String, ChannelHandlerContext> pending               = new ConcurrentHashMap<String, ChannelHandlerContext>( MAP_CAPACITY, MAP_BIN_AVG_THRESHOLD, MAP_NUM_CONCURRENT );

  public static void addReplyListener( String correlationId, ChannelHandlerContext ctx ) {
    pending.put( correlationId, ctx );
  }
  public static void removeReplyListener( String correlationId ) {
    pending.remove( correlationId );
  }
  
  @SuppressWarnings( "unchecked" )
  public void handle( EucalyptusMessage responseMessage ) {
    String corrId = responseMessage.getCorrelationId( );
    LOG.info("Message: " + responseMessage.getClass().getCanonicalName( ) + ":" + responseMessage.getCorrelationId( ) );
    ChannelHandlerContext ctx = pending.remove( corrId );
    if ( ctx == null ) {
      LOG.warn( "Received a reply for absent client:  No channel to write response message." );
      LOG.debug( responseMessage );
    } else {
      Channels.write( ctx.getChannel( ), responseMessage );
    }
  }

  public void handle( ExceptionMessage exMsg ) {
    Throwable exception = exMsg.getException( );
    Object payload = null;
    EucalyptusMessage msg = null;
    if ( exception instanceof MessagingException ) {
      MessagingException ex = ( MessagingException ) exception;
      MuleMessage muleMsg = ex.getUmoMessage( );

      if ( payload instanceof RequestTransactionScript ) {
        msg = ( ( RequestTransactionScript ) payload ).getRequestMessage( );
      } else {
        try {
          msg = parsePayload( muleMsg.getPayload( ) );
        } catch ( Exception e ) {
          LOG.error( "Bailing out of error handling: don't have the correlationId for the caller!" );
          LOG.error( e, e );
          return;
        }
      }
      EucalyptusErrorMessageType errMsg = getErrorMessageType( exMsg, msg );
      errMsg.setException( exception.getCause( ) );
      this.handle( errMsg );
    }
  }

  private EucalyptusErrorMessageType getErrorMessageType( final ExceptionMessage exMsg, final EucalyptusMessage msg ) {
    Throwable exception = exMsg.getException( );
    EucalyptusErrorMessageType errMsg = null;
    if ( exception != null ) {
      Throwable e = exMsg.getException( ).getCause( );
      if ( e != null ) {
        errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, e.getMessage( ) );
      }
    }
    if ( errMsg == null ) {
      ByteArrayOutputStream exStream = new ByteArrayOutputStream( );
      if(exception != null)
          exception.printStackTrace( new PrintStream( exStream ) );
      errMsg = new EucalyptusErrorMessageType( exMsg.getComponentName( ), msg, "Internal Error: \n" + exStream.toString( ) );
    }
    return errMsg;
  }

  private EucalyptusMessage parsePayload( Object payload ) throws Exception {
    if ( payload instanceof EucalyptusMessage ) {
      return ( EucalyptusMessage ) payload;
    } else if ( payload instanceof VmAllocationInfo ) {
      return ( ( VmAllocationInfo ) payload ).getRequest( );
    } else if ( !( payload instanceof String ) ) {
      return new EucalyptusErrorMessageType( "ReplyQueue", LogUtil.dumpObject( payload ) );
    } else {
      return ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( ( String ) payload );
    }
  }

}
