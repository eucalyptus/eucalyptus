package com.eucalyptus.ws.client.pipeline;

import java.security.GeneralSecurityException;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;

public class ClusterClientPipeline extends NioClientPipeline {
  public ClusterClientPipeline( final NioResponseHandler handler ) throws GeneralSecurityException {
    super( handler, "cc_eucalyptus_ucsb_edu", new ClusterWsSecHandler( ) );
  }
}
