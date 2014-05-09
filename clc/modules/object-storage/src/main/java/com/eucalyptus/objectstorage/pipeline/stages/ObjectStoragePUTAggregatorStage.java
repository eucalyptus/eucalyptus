package com.eucalyptus.objectstorage.pipeline.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.objectstorage.pipeline.binding.ObjectStoragePUTBinding;
import com.eucalyptus.objectstorage.pipeline.handlers.ObjectStoragePUTAggregator;
import com.eucalyptus.ws.stages.UnrollableStage;

public class ObjectStoragePUTAggregatorStage implements UnrollableStage {

	@Override
	public int compareTo( UnrollableStage o ) {
		return this.getName( ).compareTo( o.getName( ) );
	}
	
	@Override
	public String getName( ) {
		return "objectstorage-put-aggregator";
	}
	
	@Override
	public void unrollStage( ChannelPipeline pipeline ) {
		pipeline.addLast( "objectstorage-put-aggregator", new ObjectStoragePUTAggregator( ) );
	}
	
}
