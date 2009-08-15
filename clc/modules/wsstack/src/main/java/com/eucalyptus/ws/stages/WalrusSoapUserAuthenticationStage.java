package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.WalrusSoapHandler;
import com.eucalyptus.ws.handlers.WalrusSoapUserAuthenticationHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;
import com.eucalyptus.ws.handlers.wssecurity.UserWsSecHandler;

public class WalrusSoapUserAuthenticationStage implements UnrollableStage {

	@Override
	public void unrollStage( final ChannelPipeline pipeline ) {
		pipeline.addLast("walrus-soap-outbound", new WalrusSoapHandler());
		pipeline.addLast( "walrus-soap-authentication", new WalrusSoapUserAuthenticationHandler( ) );
	}

	@Override
	public String getStageName( ) {
		return "walrus-soap-user-authentication";
	}

}
