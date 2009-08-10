package com.eucalyptus.ws.client.pipeline;

import com.eucalyptus.ws.handlers.NioResponseHandler;

public class LogClientPipeline extends NioClientPipeline {
  public LogClientPipeline( final NioResponseHandler handler ) {
    super( handler, "eucalyptus_ucsb_edu" );
  }
}
