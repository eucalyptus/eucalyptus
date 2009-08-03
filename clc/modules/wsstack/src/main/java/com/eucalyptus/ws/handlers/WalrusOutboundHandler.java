package com.eucalyptus.ws.handlers;

import com.eucalyptus.ws.MappingHttpResponse;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class WalrusOutboundHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusOutboundHandler.class );

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpResponse ) {
			MappingHttpResponse httpResponse = ( MappingHttpResponse ) event.getMessage( );
			EucalyptusMessage msg = (EucalyptusMessage) httpResponse.getMessage( );
			if(msg instanceof WalrusDeleteResponseType) {
				httpResponse.setStatus(HttpResponseStatus.NO_CONTENT);
			}
		}
	}
	
}
