package com.eucalyptus.walrus.pipeline.stages;

import org.jboss.netty.channel.ChannelPipeline;

import com.eucalyptus.objectstorage.pipeline.ObjectStorageRESTExceptionHandler;
import com.eucalyptus.ws.stages.UnrollableStage;

public class WalrusRESTExceptionStage implements UnrollableStage {

	@Override
	public int compareTo(UnrollableStage arg0) {
		return this.getName().compareTo(arg0.getName());
	}

	@Override
	public void unrollStage(ChannelPipeline pipeline) {
		pipeline.addLast("walrus-exception", new ObjectStorageRESTExceptionHandler());
	}

	@Override
	public String getName() {
		return "walrus-exception";
	}

}
