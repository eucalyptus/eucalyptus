package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.HmacV2Handler;
import com.eucalyptus.ws.handlers.QueryTimestampHandler;
import com.eucalyptus.ws.handlers.RestfulMarshallingHandler;
import com.eucalyptus.ws.handlers.WalrusAuthenticationHandler;
import com.eucalyptus.ws.handlers.WalrusInboundHandler;

public class WalrusUserAuthenticationStage implements UnrollableStage {

	@Override
	public String getStageName( ) {
		return "walrus-user-authentication";
	}

	@Override
	public void unrollStage( ChannelPipeline pipeline ) {
		pipeline.addLast("walrus-inbound", new WalrusInboundHandler());
		pipeline.addLast( "walrus-verify", new WalrusAuthenticationHandler( ) );
	}

}
