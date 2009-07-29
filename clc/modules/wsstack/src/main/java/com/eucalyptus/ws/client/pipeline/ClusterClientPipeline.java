package com.eucalyptus.ws.client.pipeline;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;

public class ClusterClientPipeline extends NioClientPipeline {
  public ClusterClientPipeline( final NioResponseHandler handler ) {
    super( handler, "cc_eucalyptus_ucsb_edu", new ClusterWsSecHandler( ) );
  }
}
