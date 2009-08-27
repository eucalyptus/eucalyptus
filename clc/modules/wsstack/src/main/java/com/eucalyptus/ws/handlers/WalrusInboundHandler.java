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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.InvalidOperationException;

@ChannelPipelineCoverage("one")
public class WalrusInboundHandler extends SimpleChannelHandler {
	private static Logger LOG = Logger.getLogger( WalrusInboundHandler.class );

	@Override
	public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
		LOG.info("[exception " + exceptionEvent + "]");
		HttpResponse response;
		String responseString;
		if(exceptionEvent.getCause() instanceof AuthenticationException) {
			response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN );
			responseString = "Authentication Failed: " + response.getStatus() + "\r\n";
		} else if(exceptionEvent.getCause() instanceof InvalidOperationException) {
			response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST );
			responseString = "Invalid Operation: " + response.getStatus() + "\r\n";
		} else {
			response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR );
			responseString = "Failure: " + response.getStatus() + "\r\n";
		}
		response.setContent( ChannelBuffers.copiedBuffer(responseString, "UTF-8"));
		DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
		ctx.sendDownstream( newEvent );
		newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
	}
}
