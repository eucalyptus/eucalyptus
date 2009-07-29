package com.eucalyptus.ws.client.pipeline;

import com.eucalyptus.ws.handlers.NioResponseHandler;
import com.eucalyptus.ws.handlers.wssecurity.InternalWsSecHandler;

public class InternalClientPipeline extends NioClientPipeline {
  public InternalClientPipeline( final NioResponseHandler handler ) {
    super( handler, "ec2_amazonaws_com_doc_2009_03_01", new InternalWsSecHandler( ) );
  }
}
