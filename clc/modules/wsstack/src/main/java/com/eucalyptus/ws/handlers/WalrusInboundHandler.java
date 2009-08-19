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
