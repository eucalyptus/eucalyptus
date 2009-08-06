package com.eucalyptus.ws.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.registry.Registry;
import org.mule.config.spring.SpringRegistry;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.util.Messaging;
import com.eucalyptus.ws.util.ReplyQueue;

import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.PutObjectType;
import edu.ucsb.eucalyptus.msgs.WalrusDataGetResponseType;
import edu.ucsb.eucalyptus.msgs.WalrusDeleteResponseType;


@ChannelPipelineCoverage("one")
public class ServiceSinkHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler{
	private static Logger LOG = Logger.getLogger( ServiceSinkHandler.class );

	@Override
	public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
		ctx.sendDownstream( e );
	}

	@Override
	public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
		LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + e );
		if ( e instanceof MessageEvent ) {
			final MessageEvent event = ( MessageEvent ) e;
			if(event.getMessage() instanceof MappingHttpMessage) {
				MappingHttpMessage message = ( MappingHttpMessage ) event.getMessage( );
				EucalyptusMessage msg = (EucalyptusMessage) message.getMessage( );
				LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_RECEIVED, msg.getClass().getSimpleName() ) );
				long startTime = System.currentTimeMillis();
				if(msg instanceof PutObjectType) {
					Dispatcher dispatch = new Dispatcher(ctx, msg, message, startTime);
					dispatch.start();
				} else {
					Messaging.dispatch( "vm://RequestQueue", msg );
					EucalyptusMessage reply = null;

					reply = ReplyQueue.getReply( msg.getCorrelationId() );
					LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_SERVICED, ( System.currentTimeMillis() - startTime ) ) );
					if ( reply == null ) {
						reply = new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, "Received a NULL reply" );
					}
					MappingHttpResponse response = new MappingHttpResponse( message.getProtocolVersion( ) ); 
					response.setMessage( reply );
					if(!(reply instanceof WalrusDataGetResponseType)) {
						Channels.write(ctx.getChannel(), response);
						//ChannelFuture writeFuture = Channels.write( ctx.getChannel( ), response );						
						//writeFuture.addListener( ChannelFutureListener.CLOSE );
					}
				}
			}
		}
	}

	private class Dispatcher extends Thread {
		private ChannelHandlerContext ctx;
		private EucalyptusMessage msg;
		private MappingHttpMessage message;
		private long startTime;

		public Dispatcher(ChannelHandlerContext ctx, EucalyptusMessage msg, MappingHttpMessage message, long startTime) {
			this.ctx = ctx;
			this.msg = msg;
			this.message = message;
			this.startTime = startTime;
		}

		public void run() {
		  //:: register endpoint w/ mule specific to this message :://
			Messaging.dispatch( "vm://RequestQueue", msg );

			EucalyptusMessage reply = ReplyQueue.getReply( msg.getCorrelationId() );
			LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_SERVICED, ( System.currentTimeMillis() - startTime ) ) );
			if ( reply == null ) {
				reply = new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, "Received a NULL reply" );
			}
			MappingHttpResponse response = new MappingHttpResponse( message.getProtocolVersion( ) ); 
			response.setMessage( reply );
			Channels.write( ctx.getChannel( ), response );			
		}
	}
}
