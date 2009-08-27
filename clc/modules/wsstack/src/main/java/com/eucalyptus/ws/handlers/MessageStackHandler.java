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
package com.eucalyptus.ws.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

public abstract class MessageStackHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( MessageStackHandler.class );

  public void handleDownstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[outgoing]: " + channelEvent );
    if ( channelEvent instanceof MessageEvent ) {
      final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
      this.outgoingMessage( channelHandlerContext, msgEvent );
    }
    channelHandlerContext.sendDownstream( channelEvent );
  }

  public abstract void outgoingMessage( final ChannelHandlerContext ctx, MessageEvent event ) throws Exception;

  public abstract void incomingMessage( final ChannelHandlerContext ctx, MessageEvent event ) throws Exception;

  public void exceptionCaught( final Throwable t ) throws Exception {

  }

  public void exceptionCaught( final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[exception:" + exceptionEvent.getCause( ).getClass( ).getSimpleName( ) + "]: " + exceptionEvent.getCause( ).getMessage( ) );
  }

  public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
    MessageStackHandler.LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + channelEvent );
    if ( channelEvent instanceof MessageEvent ) {
      final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
      this.incomingMessage( channelHandlerContext, msgEvent );
    } else if ( channelEvent instanceof ExceptionEvent ) {
      this.exceptionCaught( channelHandlerContext, ( ExceptionEvent ) channelEvent );
    }
    channelHandlerContext.sendUpstream( channelEvent );
  }
}
