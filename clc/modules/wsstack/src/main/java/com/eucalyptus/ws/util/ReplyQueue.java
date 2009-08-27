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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
  
  @SuppressWarnings( "unchecked" )
  public void handle( EucalyptusMessage responseMessage ) {
    String corrId = responseMessage.getCorrelationId( );
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
    } else {
      return ( EucalyptusMessage ) BindingManager.getBinding( "msgs_eucalyptus_ucsb_edu" ).fromOM( ( String ) payload );
    }
  }

}
