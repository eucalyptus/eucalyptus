package com.eucalyptus.ws.server;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.stages.HmacV2UserAuthenticationStage;
import com.eucalyptus.ws.stages.QueryBindingStage;
import com.eucalyptus.ws.stages.UnrollableStage;
import com.eucalyptus.ws.stages.WalrusOutboundStage;
import com.eucalyptus.ws.stages.WalrusRESTBindingStage;
import com.eucalyptus.ws.stages.WalrusUserAuthenticationStage;
import com.eucalyptus.ws.util.WalrusProperties;


public class WalrusRESTPipeline extends FilteredPipeline {
	private static Logger LOG = Logger.getLogger( WalrusRESTPipeline.class );

	@Override
	protected void addStages( List<UnrollableStage> stages ) {
		stages.add( new WalrusUserAuthenticationStage( ) );
		stages.add( new WalrusRESTBindingStage( ) );
		stages.add( new WalrusOutboundStage());
	}

	@Override
	protected boolean checkAccepts( HttpRequest message ) {
		//TODO: Mangle uri for virtual hosting
		return message.getUri().startsWith(WalrusProperties.walrusServicePath) ||
		message.getHeader(HttpHeaders.Names.HOST).contains(".walrus");		
	}

	@Override
	public String getPipelineName( ) {
		return "walrus-rest";
	}

}
