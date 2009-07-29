package com.eucalyptus.ws.client;

import org.mule.transport.AbstractMessageDispatcher;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

public class NioMessageDispatcher extends AbstractMessageDispatcher {

  public NioMessageDispatcher( OutboundEndpoint outboundEndpoint ) {
    super( outboundEndpoint );
    //:: NioClient: host,port
    //:: ws-sec policy
    //:: message pattern
  }

  @Override
  protected void doDispatch( final MuleEvent muleEvent ) throws Exception {

  }

  @Override
  protected MuleMessage doSend( final MuleEvent muleEvent ) throws Exception {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void doDispose() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void doConnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void doDisconnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
