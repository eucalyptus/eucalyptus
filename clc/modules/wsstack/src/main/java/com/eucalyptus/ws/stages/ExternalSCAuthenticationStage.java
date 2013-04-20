package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.ws.handlers.NodeWsSecHandler;

public class ExternalSCAuthenticationStage implements UnrollableStage {

	@Override
	public int compareTo(UnrollableStage o) {
		return this.getName( ).compareTo( o.getName( ) );		
	}

	@Override
	public void unrollStage(ChannelPipeline pipeline) {
		pipeline.addLast( "node-soap-authentication", new NodeWsSecHandler( ) );
	}

	@Override
	public String getName() {
		return "external-sc-soap-authentication";
	}

}
