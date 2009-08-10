package com.eucalyptus.ws.client.pipeline;

import java.security.GeneralSecurityException;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;

public class InternalClientPipeline extends NioClientPipeline {
  public InternalClientPipeline( final NioResponseHandler handler ) throws GeneralSecurityException {
    super( handler, "msgs_eucalyptus_ucsb_edu", new InternalWsSecHandler( ) );
  }
}
